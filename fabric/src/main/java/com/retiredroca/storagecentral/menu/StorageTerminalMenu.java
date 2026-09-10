package com.retiredroca.storagecentral.menu;

import java.util.List;

import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.registration.Registration;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StorageTerminalMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_COUNT = 27;
    private static final int PLAYER_HOTBAR_COUNT = 9;
    private static final int SLOT_COUNT = PLAYER_INV_COUNT + PLAYER_HOTBAR_COUNT;

    private final StorageTerminalBlockEntity terminal;
    private final BlockPos pos;

    private List<ItemStack> serverItems = List.of();
    private List<Integer> serverCounts = List.of();
    private int serverTier = 0;
    private int dataVersion = 0;

    public StorageTerminalMenu(int containerId, Inventory playerInventory, StorageTerminalBlockEntity terminal) {
        super(Registration.STORAGE_TERMINAL_MENU, containerId);
        this.terminal = terminal;
        this.pos = terminal.getBlockPos();
        addPlayerInventory(playerInventory);
    }

    public static StorageTerminalMenu fromNetwork(int containerId, Inventory playerInventory, BlockPos pos) {
        return new StorageTerminalMenu(containerId, playerInventory, pos, null);
    }

    private StorageTerminalMenu(int containerId, Inventory playerInventory, BlockPos pos,
            StorageTerminalBlockEntity terminal) {
        super(Registration.STORAGE_TERMINAL_MENU, containerId);
        this.terminal = terminal;
        this.pos = pos;
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startX = 8;
        int startY = 140;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<ItemStack> getServerItems() {
        return serverItems;
    }

    public List<Integer> getServerCounts() {
        return serverCounts;
    }

    public int getServerTier() {
        return serverTier;
    }

    public void updateServerItems(List<ItemStack> items, List<Integer> counts, int tier) {
        this.serverItems = items;
        this.serverCounts = counts;
        this.serverTier = tier;
        this.dataVersion++;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public StorageTerminalBlockEntity getTerminal() {
        return terminal;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= SLOT_COUNT || terminal == null) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        int count = stack.getCount();
        ItemStack remainder = depositIntoNetwork(stack);
        slot.set(remainder);
        if (remainder.getCount() < count) {
            this.broadcastChanges();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.retiredroca.storagecentral.network.Networking.refresh(serverPlayer, terminal);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack depositIntoNetwork(ItemStack stack) {
        if (terminal == null) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (Storage<ItemVariant> handler : terminal.getScannedHandlers()) {
            remaining = insertBestFit(handler, remaining);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    private ItemStack insertBestFit(Storage<ItemVariant> handler, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = handler.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
            if (inserted <= 0) {
                return stack;
            }
            ItemStack remaining = stack.copy();
            remaining.shrink((int) inserted);
            return remaining;
        }
    }

    public void doExtract(Player player, ItemStack requested, int mode) {
        if (terminal == null) {
            return;
        }
        int desired = switch (mode) {
            case 0 -> 1;
            case 1 -> requested.getMaxStackSize();
            default -> requested.getMaxStackSize();
        };
        ItemVariant variant = ItemVariant.of(requested);
        long extracted;
        try (Transaction transaction = Transaction.openOuter()) {
            long remaining = desired;
            for (Storage<ItemVariant> handler : terminal.getScannedHandlers()) {
                if (remaining <= 0) {
                    break;
                }
                if (!handler.supportsExtraction()) {
                    continue;
                }
                remaining -= handler.extract(variant, remaining, transaction);
            }
            transaction.commit();
            extracted = desired - remaining;
        }
        if (extracted > 0 && player != null) {
            ItemStack output = variant.toStack((int) extracted);
            if (!player.getInventory().add(output)) {
                player.drop(output, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminal == null) {
            return true;
        }
        return terminal.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }
}