package com.retiredroca.storagecentral.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class Networking {
    public static final ResourceLocation TERMINAL_SYNC = ResourceLocation.fromNamespaceAndPath(StorageCentral.MODID, "terminal_sync");
    public static final ResourceLocation TERMINAL_EXTRACT = ResourceLocation.fromNamespaceAndPath(StorageCentral.MODID, "terminal_extract");

    public record ChestSync(String name, BlockPos pos, List<ItemStack> items, List<Integer> counts) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ChestSync::name,
                BlockPos.STREAM_CODEC, ChestSync::pos,
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), ChestSync::items,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ChestSync::counts,
                ChestSync::new);
    }

    public record TerminalSyncPayload(List<ItemStack> items, List<Integer> counts, List<ChestSync> chests, int tier) implements CustomPacketPayload {
        public static final Type<TerminalSyncPayload> TYPE = new Type<>(TERMINAL_SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::items,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), TerminalSyncPayload::counts,
                ChestSync.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::chests,
                ByteBufCodecs.VAR_INT, TerminalSyncPayload::tier,
                TerminalSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TerminalExtractPayload(BlockPos pos, ItemStack stack, int mode) implements CustomPacketPayload {
        public static final Type<TerminalExtractPayload> TYPE = new Type<>(TERMINAL_EXTRACT);
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalExtractPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TerminalExtractPayload::pos,
                ItemStack.OPTIONAL_STREAM_CODEC, TerminalExtractPayload::stack,
                ByteBufCodecs.VAR_INT, TerminalExtractPayload::mode,
                TerminalExtractPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Networking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TerminalExtractPayload.TYPE, TerminalExtractPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TerminalExtractPayload.TYPE, Networking::handleExtract);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, Networking::handleSync);
    }

    private static void handleSync(TerminalSyncPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof StorageTerminalMenu menu) {
                menu.updateServerItems(payload.items(), payload.counts(), payload.chests(), payload.tier());
            }
        });
    }

    private static void handleExtract(TerminalExtractPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player.level().getBlockEntity(payload.pos()) instanceof StorageTerminalBlockEntity terminal) {
                terminal.scanNetwork();
                if (player.containerMenu instanceof StorageTerminalMenu menu
                        && menu.getPos().equals(payload.pos())) {
                    menu.doExtract(player, payload.stack(), payload.mode());
                    refresh(player, terminal);
                }
            }
        });
    }

    public static void sendTerminalSyncToPlayer(StorageTerminalBlockEntity terminal, ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Storage<ItemVariant> handler : dedupeHandlers(terminal.getScannedHandlers())) {
            aggregate(handler, items, counts);
        }
        List<ChestSync> chests = new ArrayList<>();
        for (Storage<ItemVariant> handler : terminal.getScannedHandlers()) {
            BlockPos pos = terminal.posFor(handler);
            if (pos == null) {
                continue;
            }
            List<ItemStack> chestItems = new ArrayList<>();
            List<Integer> chestCounts = new ArrayList<>();
            aggregate(handler, chestItems, chestCounts);
            chests.add(new ChestSync(terminal.labelFor(handler), pos, chestItems, chestCounts));
        }
        ServerPlayNetworking.send(player, new TerminalSyncPayload(items, counts, chests, terminal.getTier()));
    }

    private static void aggregate(Storage<ItemVariant> handler, List<ItemStack> items, List<Integer> counts) {
        for (StorageView<ItemVariant> view : handler.nonEmptyViews()) {
            ItemStack stack = view.getResource().toStack((int) view.getAmount());
            boolean matched = false;
            for (int j = 0; j < items.size(); j++) {
                if (ItemStack.isSameItemSameComponents(items.get(j), stack)) {
                    counts.set(j, counts.get(j) + (int) view.getAmount());
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                items.add(stack.copy());
                counts.add((int) view.getAmount());
            }
        }
    }

    /**
     * Returns the handlers with duplicate underlying inventories removed. Two handlers that
     * belong to the same container (for example the two halves of a double chest) expose the
     * same slots and identical item contents, so we collapse them to a single entry to
     * avoid double-counting in the terminal. Identity is also taken into account so distinct
     * handlers backed by the same block are merged.
     */
    private static List<Storage<ItemVariant>> dedupeHandlers(List<Storage<ItemVariant>> handlers) {
        List<Storage<ItemVariant>> result = new ArrayList<>();
        for (Storage<ItemVariant> handler : handlers) {
            boolean dup = false;
            for (Storage<ItemVariant> existing : result) {
                if (sameInventory(existing, handler)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                result.add(handler);
            }
        }
        return result;
    }

    private static boolean sameInventory(Storage<ItemVariant> a, Storage<ItemVariant> b) {
        if (a == b) {
            return true;
        }
        return contents(a).equals(contents(b));
    }

    private static Map<ItemVariant, Long> contents(Storage<ItemVariant> storage) {
        Map<ItemVariant, Long> map = new HashMap<>();
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) {
                continue;
            }
            map.merge(view.getResource(), view.getAmount(), Long::sum);
        }
        return map;
    }

    public static void refresh(ServerPlayer player, StorageTerminalBlockEntity terminal) {
        sendTerminalSyncToPlayer(terminal, player);
    }

    public static void sendExtract(BlockPos pos, ItemStack stack, int mode) {
        ClientPlayNetworking.send(new TerminalExtractPayload(pos, stack, mode));
    }
}