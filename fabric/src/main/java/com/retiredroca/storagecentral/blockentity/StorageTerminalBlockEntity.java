package com.retiredroca.storagecentral.blockentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private List<Storage<ItemVariant>> scannedHandlers = new ArrayList<>();
    private Map<Storage<ItemVariant>, String> handlerLabel = new HashMap<>();
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
        return (int) CHUNK_RADII[Math.min(getEffectiveMaxTier(), CHUNK_RADII.length - 1)];
    }

    public boolean tryApplyUpgrade(int upgradeTier, @Nullable Player player) {
        if (upgradeTier == getTier() + 1 && upgradeTier <= getEffectiveMaxTier()) {
            this.tier = upgradeTier;
            setChanged();
            return true;
        }
        return false;
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
                handlerLabel.put(storage, blockEntity.getBlockState().getBlock().getName().getString());
            }
        }
    }

    public List<Storage<ItemVariant>> getScannedHandlers() {
        return scannedHandlers;
    }

    public String labelFor(Storage<ItemVariant> handler) {
        return handlerLabel.getOrDefault(handler, "?");
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
}