package com.retiredroca.storagecentral.blockentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.storagecentral.config.StorageCentralConfig;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.registration.Registration;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class StorageTerminalBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
    private static final String TAG_TIER = "tier";
    private static final long[] CHUNK_RADII = { 0, 1, 2, 3, 4, 5 };

    private int tier = 0;

    private boolean lidOpen = false;
    private long lidChangeTime = 0;

    private List<Storage<ItemVariant>> scannedHandlers = new ArrayList<>();
    private Map<Storage<ItemVariant>, String> handlerLabel = new HashMap<>();
    private Map<Storage<ItemVariant>, BlockPos> handlerPos = new HashMap<>();
    private long lastScan = 0;

    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_TERMINAL_BE, pos, state);
    }

    public int getTier() {
        return tier;
    }

    public int getEffectiveMaxTier() {
        int cap = StorageCentralConfig.getMaxTier();
        if (level instanceof ServerLevel serverLevel) {
            int viewDistance = serverLevel.getServer().getPlayerList().getViewDistance();
            int simDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();
            int radius = Math.min(viewDistance, simDistance);
            for (int t = 0; t < CHUNK_RADII.length && CHUNK_RADII[t] <= radius; t++) {
                cap = t;
            }
        }
        return cap;
    }

    public int getChunkRadius() {
        int effectiveTier = Math.min(tier, getEffectiveMaxTier());
        return (int) CHUNK_RADII[Math.min(effectiveTier, CHUNK_RADII.length - 1)];
    }

    public void startOpen(Player player) {
        if (level == null || level.isClientSide || lidOpen) {
            return;
        }
        lidOpen = true;
        lidChangeTime = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.1F + 0.9F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 1);
    }

    public void stopOpen(Player player) {
        if (level == null || level.isClientSide || !lidOpen) {
            return;
        }
        lidOpen = false;
        lidChangeTime = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.1F + 0.9F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
    }

    @Override
    public boolean triggerEvent(int type, int data) {
        if (type == 1) {
            lidOpen = data != 0;
            if (level != null) {
                lidChangeTime = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(type, data);
    }

    public float getOpenNess(float partialTick) {
        if (level == null || lidChangeTime == 0) {
            return 0.0F;
        }
        float elapsed = (float) (level.getGameTime() - lidChangeTime) + partialTick;
        float t = Mth.clamp(elapsed / 7.0F, 0.0F, 1.0F);
        return lidOpen ? t : 1.0F - t;
    }

    public void scanNetwork() {
        if (level instanceof ServerLevel serverLevel) {
            long now = System.currentTimeMillis();
            if (now - lastScan < 500) {
                return;
            }
            lastScan = now;
            scannedHandlers.clear();
            handlerLabel.clear();
            handlerPos.clear();

            int radius = getChunkRadius();
            int centerX = worldPosition.getX() >> 4;
            int centerZ = worldPosition.getZ() >> 4;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    scanChunk(serverLevel, centerX + dx, centerZ + dz);
                }
            }
        }
    }

    private void scanChunk(ServerLevel serverLevel, int chunkX, int chunkZ) {
        // Only read chunks that are fully loaded. We intentionally do not force-load,
        // since the player accesses the terminal in person and its radius chunks are
        // naturally within simulation distance.
        if (!serverLevel.getChunkSource().isPositionTicking(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ))) {
            return;
        }
        LevelChunk chunk = serverLevel.getChunk(chunkX, chunkZ);
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            BlockPos pos = blockEntity.getBlockPos();
            if (pos.getY() < serverLevel.getMinBuildHeight() || pos.getY() >= serverLevel.getMaxBuildHeight()) {
                continue;
            }
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(serverLevel, pos, null);
            if (storage != null) {
                scannedHandlers.add(storage);
                Component label = null;
                if (blockEntity instanceof Nameable nameable && nameable.hasCustomName() && nameable.getCustomName() != null) {
                    label = nameable.getCustomName();
                }
                handlerLabel.put(storage, (label != null ? label : blockEntity.getBlockState().getBlock().getName()).getString());
                handlerPos.put(storage, pos);
            }
        }
    }

    public List<Storage<ItemVariant>> getScannedHandlers() {
        return scannedHandlers;
    }

    public String labelFor(Storage<ItemVariant> handler) {
        return handlerLabel.getOrDefault(handler, "?");
    }

    public BlockPos posFor(Storage<ItemVariant> handler) {
        return handlerPos.get(handler);
    }

    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.storage_central.storage_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageTerminalMenu(containerId, playerInventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt(TAG_TIER);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, tier);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(TAG_TIER, tier);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}