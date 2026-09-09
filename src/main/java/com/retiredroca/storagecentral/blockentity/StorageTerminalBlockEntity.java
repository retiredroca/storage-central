package com.retiredroca.storagecentral.blockentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.registration.Registration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class StorageTerminalBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_TIER = "tier";
    private static final long[] CHUNK_RADII = { 0, 1, 2, 3 };

    private int tier = 0;

    private List<IItemHandler> scannedHandlers = new ArrayList<>();
    private Map<IItemHandler, String> handlerLabel = new HashMap<>();
    private long lastScan = 0;

    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_TERMINAL_BE.get(), pos, state);
    }

    public int getTier() {
        return tier;
    }

    public int getChunkRadius() {
        return (int) CHUNK_RADII[Math.min(tier, CHUNK_RADII.length - 1)];
    }

    public boolean tryApplyUpgrade(int upgradeTier, @Nullable Player player) {
        if (upgradeTier == getTier() + 1 && upgradeTier < CHUNK_RADII.length) {
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
        // Only inventory-bearing blocks are block entities (chests, barrels, furnaces, hoppers,
        // etc.), so scanning the chunk's block entities is many orders of magnitude faster than
        // probing every single block position for an item-handler capability.
        LevelChunk chunk = serverLevel.getChunk(chunkX, chunkZ);
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            BlockPos pos = blockEntity.getBlockPos();
            if (pos.getY() < serverLevel.getMinBuildHeight() || pos.getY() >= serverLevel.getMaxBuildHeight()) {
                continue;
            }
            for (Direction side : Direction.values()) {
                IItemHandler handler = serverLevel.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
                if (handler != null) {
                    scannedHandlers.add(handler);
                    handlerLabel.put(handler, blockEntity.getBlockState().getBlock().getName().getString());
                    break;
                }
            }
        }
    }

    public List<IItemHandler> getScannedHandlers() {
        return scannedHandlers;
    }

    public String labelFor(IItemHandler handler) {
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
