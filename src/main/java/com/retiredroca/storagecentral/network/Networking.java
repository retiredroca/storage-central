package com.retiredroca.storagecentral.network;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.ArrayList;
import net.neoforged.neoforge.items.IItemHandler;

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

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(StorageCentral.MODID).versioned("1");
        registrar.playToClient(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC, Networking::handleSync);
        registrar.playToServer(TerminalExtractPayload.TYPE, TerminalExtractPayload.STREAM_CODEC, Networking::handleExtract);
    }

    private static void handleSync(TerminalSyncPayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof StorageTerminalMenu menu) {
                menu.updateServerItems(payload.items(), payload.counts(), payload.chests(), payload.tier());
            }
        });
    }

    private static void handleExtract(TerminalExtractPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(payload.pos()) instanceof StorageTerminalBlockEntity terminal) {
                    terminal.scanNetwork();
                    if (player.containerMenu instanceof StorageTerminalMenu menu
                            && menu.getPos().equals(payload.pos())) {
                        menu.doExtract(player, payload.stack(), payload.mode());
                        refresh(player, terminal);
                    }
                }
            }
        });
    }

    public static void sendTerminalSyncToPlayer(StorageTerminalBlockEntity terminal, ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (IItemHandler handler : dedupeHandlers(terminal.getScannedHandlers())) {
            aggregate(handler, items, counts);
        }
        List<ChestSync> chests = new ArrayList<>();
        for (IItemHandler handler : terminal.getScannedHandlers()) {
            BlockPos pos = terminal.posFor(handler);
            if (pos == null) {
                continue;
            }
            List<ItemStack> chestItems = new ArrayList<>();
            List<Integer> chestCounts = new ArrayList<>();
            aggregate(handler, chestItems, chestCounts);
            chests.add(new ChestSync(terminal.labelFor(handler), pos, chestItems, chestCounts));
        }
        PacketDistributor.sendToPlayer(player, new TerminalSyncPayload(items, counts, chests, terminal.getTier()));
    }

    private static void aggregate(IItemHandler handler, List<ItemStack> items, List<Integer> counts) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                boolean matched = false;
                for (int j = 0; j < items.size(); j++) {
                    if (ItemStack.isSameItemSameComponents(items.get(j), stack)) {
                        counts.set(j, counts.get(j) + stack.getCount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    items.add(stack.copy());
                    counts.add(stack.getCount());
                }
            }
        }
    }

    /**
     * Returns the handlers with duplicate underlying inventories removed. Two handlers that
     * belong to the same container (for example the two halves of a double chest) expose the
     * same number of slots and identical item contents, so we collapse them to a single entry to
     * avoid double-counting in the terminal. Identity is also taken into account so distinct
     * handlers backed by the same block are merged.
     */
    private static List<IItemHandler> dedupeHandlers(List<IItemHandler> handlers) {
        List<IItemHandler> result = new ArrayList<>();
        for (IItemHandler handler : handlers) {
            boolean dup = false;
            for (IItemHandler existing : result) {
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

    private static boolean sameInventory(IItemHandler a, IItemHandler b) {
        if (a == b) {
            return true;
        }
        if (a.getSlots() != b.getSlots()) {
            return false;
        }
        for (int i = 0; i < a.getSlots(); i++) {
            if (!ItemStack.isSameItemSameComponents(a.getStackInSlot(i), b.getStackInSlot(i))) {
                return false;
            }
        }
        return true;
    }

    public static void refresh(ServerPlayer player, StorageTerminalBlockEntity terminal) {
        sendTerminalSyncToPlayer(terminal, player);
    }

    public static void sendExtract(net.minecraft.core.BlockPos pos, ItemStack stack, int mode) {
        PacketDistributor.sendToServer(new TerminalExtractPayload(pos, stack, mode));
    }
}
