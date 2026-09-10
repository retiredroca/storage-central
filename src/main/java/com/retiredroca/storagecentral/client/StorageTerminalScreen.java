package com.retiredroca.storagecentral.client;

import java.util.List;

import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.network.Networking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/gui/container/generic_54.png");
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 6;
    private static final int SLOT = 18;
    private static final int GRID_TOP = 18;
    private static final int SEARCH_BOX_W = 140;
    private static final int SEARCH_BOX_H = 12;
    private static final int CHEST_DROP_W = 120;
    private static final int CHEST_DROP_H = 14;
    private static final int CHEST_ROW_H = 18;
    private static final int CHEST_MAX_ROWS = 10;

    private List<ItemSorter.VirtualItem> displayItems = List.of();
    private int scrollOffset = 0;
    private int lastDataVersion = -1;
    private EditBox searchBox;
    private int searchBoxX;
    private int searchBoxY;
    private boolean draggingBar = false;
    private int dragDX = 0;
    private int dragDY = 0;

    private int chestX;
    private int chestY;
    private boolean chestOpen = false;
    private int chestScroll = 0;
    private int selectedChest = -1;
    private boolean draggingChest = false;
    private int dragCX = 0;
    private int dragCY = 0;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBoxX = TerminalUiState.hasSearch()
                ? Math.max(0, Math.min(this.width - SEARCH_BOX_W, TerminalUiState.getSearchX()))
                : Math.max(0, Math.min(this.width - SEARCH_BOX_W,
                        this.leftPos + (this.imageWidth - SEARCH_BOX_W) / 2));
        this.searchBoxY = TerminalUiState.hasSearch()
                ? Math.max(0, Math.min(this.height - SEARCH_BOX_H, TerminalUiState.getSearchY()))
                : Math.max(0, Math.min(this.height - SEARCH_BOX_H, this.topPos + this.imageHeight + 8));
        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, SEARCH_BOX_W, SEARCH_BOX_H,
                Component.translatable("gui.storage_central.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(s -> rebuild());
        this.addWidget(this.searchBox);
        // Chest selector floats to the right of the inventory screen, detached like the search field.
        this.chestX = TerminalUiState.hasChest()
                ? Math.max(0, Math.min(this.width - CHEST_DROP_W, TerminalUiState.getChestX()))
                : Math.max(0, Math.min(this.width - CHEST_DROP_W, this.leftPos + this.imageWidth + 8));
        this.chestY = TerminalUiState.hasChest()
                ? Math.max(0, Math.min(this.height - CHEST_DROP_H, TerminalUiState.getChestY()))
                : Math.max(0, Math.min(this.height - CHEST_DROP_H, this.topPos));
        rebuild();
    }

    @Override
    public void onClose() {
        TerminalUiState.setSearchX(searchBoxX);
        TerminalUiState.setSearchY(searchBoxY);
        TerminalUiState.setChestX(chestX);
        TerminalUiState.setChestY(chestY);
        TerminalUiState.save();
        super.onClose();
    }

    private void rebuild() {
        if (this.minecraft != null) {
            List<ItemStack> items = menu.getServerItems();
            List<Integer> counts = menu.getServerCounts();
            List<Networking.ChestSync> chests = menu.getServerChests();
            if (selectedChest >= 0 && selectedChest < chests.size()) {
                items = chests.get(selectedChest).items();
                counts = chests.get(selectedChest).counts();
            }
            this.displayItems = ItemSorter.filter(ItemSorter.build(items, counts),
                    searchBox == null ? "" : searchBox.getValue());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.getDataVersion() != lastDataVersion) {
            lastDataVersion = menu.getDataVersion();
            rebuild();
        }
    }

    private int chestRowCount() {
        return 1 + menu.getServerChests().size();
    }

    private int chestVisibleRows() {
        return Math.min(CHEST_MAX_ROWS, chestRowCount());
    }

    private int chestListWidth() {
        int max = CHEST_DROP_W;
        List<Networking.ChestSync> chests = menu.getServerChests();
        for (int a = 0; a < chestRowCount(); a++) {
            Networking.ChestSync chest = a > 0 ? chests.get(a - 1) : null;
            String name = chest != null
                    ? chest.name()
                    : Component.translatable("gui.storage_central.chest_all").getString();
            max = Math.max(max, font.width(name));
            String sub;
            if (chest != null) {
                BlockPos pos = chest.pos();
                sub = String.format("Chunk [%d, %d] (%d, %d, %d)",
                        pos.getX() >> 4, pos.getZ() >> 4, pos.getX(), pos.getY(), pos.getZ());
            } else {
                int n = chests.size();
                sub = n == 0
                        ? Component.translatable("gui.storage_central.chest_none").getString()
                        : Component.translatable("gui.storage_central.chest_count", n).getString();
            }
            max = Math.max(max, font.width(sub));
        }
        max += 8;
        return Math.min(max, Math.max(CHEST_DROP_W, this.width - chestX - 4));
    }

    private int rowAt(double mouseX, double mouseY) {
        int width = chestOpen ? chestListWidth() : CHEST_DROP_W;
        if (mouseX < chestX || mouseX > chestX + width || mouseY < chestY + CHEST_DROP_H + 2) {
            return -1;
        }
        int row = (int) ((mouseY - (chestY + CHEST_DROP_H + 2)) / CHEST_ROW_H);
        if (row >= chestVisibleRows() || row + chestScroll >= chestRowCount()) {
            return -1;
        }
        return row;
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) > maxWidth) {
            return font.plainSubstrByWidth(text, maxWidth) + "...";
        }
        return text;
    }

    private String headerText() {
        List<Networking.ChestSync> chests = menu.getServerChests();
        String text;
        if (selectedChest >= 0 && selectedChest < chests.size()) {
            text = chests.get(selectedChest).name();
        } else {
            text = Component.translatable("gui.storage_central.chest_all").getString();
        }
        return truncate(text, CHEST_DROP_W - 24);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, double horizontalDelta) {
        if (hasShiftDown() || hasControlDown()) {
            return super.mouseScrolled(mouseX, mouseY, delta, horizontalDelta);
        }
        if (chestOpen && mouseX >= chestX && mouseX <= chestX + chestListWidth()
                && mouseY >= chestY + CHEST_DROP_H + 2) {
            int maxScroll = Math.max(0, chestRowCount() - CHEST_MAX_ROWS);
            chestScroll = Math.max(0, Math.min(maxScroll, chestScroll - (delta > 0 ? 1 : -1)));
            return true;
        }
        int maxOffset = Math.max(0, (int) Math.ceil(displayItems.size() / (double) (GRID_COLS * GRID_ROWS)) - 1);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (delta > 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = rowAt(mouseX, mouseY);
            boolean inHeader = mouseX >= chestX && mouseX <= chestX + CHEST_DROP_W
                    && mouseY > chestY + 1 && mouseY <= chestY + CHEST_DROP_H;
            boolean inDragStrip = mouseX >= chestX - 5 && mouseX <= chestX + CHEST_DROP_W + 5
                    && (mouseY >= chestY - 6 && mouseY <= chestY
                    || mouseY >= chestY + 1 && mouseY <= chestY + CHEST_DROP_H
                    && (mouseX < chestX + 3 || mouseX > chestX + CHEST_DROP_W - 3));
            if (!inHeader && !inDragStrip && row < 0) {
                chestOpen = false;
            }
            if (chestOpen && row >= 0) {
                int absolute = row + chestScroll;
                selectedChest = absolute == 0 ? -1 : absolute - 1;
                chestOpen = false;
                chestScroll = 0;
                scrollOffset = 0;
                rebuild();
                return true;
            }
            if (inDragStrip) {
                draggingChest = true;
                dragCX = (int) mouseX - chestX;
                dragCY = (int) mouseY - chestY;
                return true;
            }
            if (inHeader) {
                chestOpen = !chestOpen;
                chestScroll = 0;
                return true;
            }
        }
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseX >= searchBoxX - 5 && mouseX <= searchBoxX + SEARCH_BOX_W + 5
                && mouseY >= searchBoxY - 5 && mouseY <= searchBoxY + SEARCH_BOX_H + 5) {
            draggingBar = true;
            dragDX = (int) mouseX - searchBoxX;
            dragDY = (int) mouseY - searchBoxY;
            return true;
        }
        if (button == 0) {
            int index = slotAt(mouseX, mouseY);
            if (index >= 0) {
                boolean fullStack = hasShiftDown();
                ItemSorter.VirtualItem item = displayItems.get(index);
                Networking.sendExtract(menu.getPos(), item.stack(), fullStack ? 1 : 0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingChest) {
            chestX = (int) mouseX - dragCX;
            chestY = (int) mouseY - dragCY;
            chestX = Math.max(0, Math.min(this.width - CHEST_DROP_W, chestX));
            chestY = Math.max(0, Math.min(this.height - CHEST_DROP_H, chestY));
            return true;
        }
        if (draggingBar && searchBox != null) {
            searchBoxX = (int) mouseX - dragDX;
            searchBoxY = (int) mouseY - dragDY;
            searchBoxX = Math.max(0, Math.min(this.width - SEARCH_BOX_W, searchBoxX));
            searchBoxY = Math.max(0, Math.min(this.height - SEARCH_BOX_H, searchBoxY));
            searchBox.setX(searchBoxX);
            searchBox.setY(searchBoxY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        draggingChest = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int slotAt(double mouseX, double mouseY) {
        int gridLeft = this.leftPos + 8;
        int gridTop = this.topPos + GRID_TOP;
        if (mouseX < gridLeft || mouseY < gridTop) {
            return -1;
        }
        int col = (int) ((mouseX - gridLeft) / SLOT);
        int row = (int) ((mouseY - gridTop) / SLOT);
        if (col < 0 || col >= GRID_COLS || row < 0 || row >= GRID_ROWS) {
            return -1;
        }
        int idx = row * GRID_COLS + col + scrollOffset * GRID_COLS * GRID_ROWS;
        return idx < displayItems.size() ? idx : -1;
    }

    @Override
    public boolean charTyped(char code, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(code, modifiers);
        }
        if (searchBox != null && Character.isLetterOrDigit(code)) {
            searchBox.setFocused(true);
            return searchBox.charTyped(code, modifiers);
        }
        return super.charTyped(code, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(BG, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Separately-positioned EMI-style search field (overlay, below the inventory)
        graphics.fill(searchBoxX - 1, searchBoxY - 1, searchBoxX + SEARCH_BOX_W + 1, searchBoxY + SEARCH_BOX_H + 1, 0xFF000000);
        graphics.fill(searchBoxX, searchBoxY, searchBoxX + SEARCH_BOX_W, searchBoxY + SEARCH_BOX_H, 0xFF101010);

        // Highlight which slots are visible if there are more pages
        int total = displayItems.size();
        int capacity = GRID_COLS * GRID_ROWS;
        int maxOffset = Math.max(0, (int) Math.ceil(total / (double) capacity) - 1);
        if (maxOffset > 0) {
            String pageText = Component.translatable("gui.storage_central.page",
                    scrollOffset + 1, maxOffset + 1).getString();
            graphics.drawString(font, pageText, x + this.imageWidth - font.width(pageText) - 8, y + 6, 0x404040, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        String tierText = Component.translatable("gui.storage_central.tier", menu.getServerTier()).getString();
        graphics.drawString(font, tierText, this.titleLabelX + font.width(this.title) + 2, this.titleLabelY, 0x404040, false);

        graphics.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void renderChestSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        // Header
        graphics.fill(chestX - 1, chestY - 1, chestX + CHEST_DROP_W + 1, chestY + CHEST_DROP_H + 1, 0xFF000000);
        graphics.fill(chestX, chestY, chestX + CHEST_DROP_W, chestY + CHEST_DROP_H, 0xFF101010);
        graphics.drawString(font, headerText(), chestX + 3, chestY + 3, 0xE0E0E0, false);
        graphics.drawString(font, "v", chestX + CHEST_DROP_W - 9, chestY + 3, 0xE0E0E0, false);

        if (chestOpen) {
            int rows = chestVisibleRows();
            int listW = chestListWidth();
            int listTop = chestY + CHEST_DROP_H + 2;
            int listH = rows * CHEST_ROW_H;
            graphics.fill(chestX - 1, listTop - 1, chestX + listW + 1, listTop + listH + 1, 0xFF000000);
            int hoveredRow = rowAt(mouseX, mouseY);
            for (int v = 0; v < rows; v++) {
                int absolute = v + chestScroll;
                int y = listTop + v * CHEST_ROW_H;
                boolean selected = absolute == selectedChest + 1;
                int bg = hoveredRow == v ? 0xFF202020 : (selected ? 0xFF181818 : 0xFF101010);
                graphics.fill(chestX, y, chestX + listW, y + CHEST_ROW_H, bg);
                if (absolute == 0) {
                    graphics.drawString(font, Component.translatable("gui.storage_central.chest_all").getString(),
                            chestX + 3, y + 2, 0xE0E0E0, false);
                    int n = menu.getServerChests().size();
                    String sub = n == 0
                            ? Component.translatable("gui.storage_central.chest_none").getString()
                            : Component.translatable("gui.storage_central.chest_count", n).getString();
                    graphics.drawString(font, truncate(sub, listW - 6), chestX + 3, y + 11, 0x707070, false);
                } else {
                    Networking.ChestSync chest = menu.getServerChests().get(absolute - 1);
                    graphics.drawString(font, truncate(chest.name(), listW - 6), chestX + 3, y + 2, 0xE0E0E0, false);
                    BlockPos pos = chest.pos();
                    String sub = String.format("Chunk [%d, %d] (%d, %d, %d)",
                            pos.getX() >> 4, pos.getZ() >> 4, pos.getX(), pos.getY(), pos.getZ());
                    graphics.drawString(font, truncate(sub, listW - 6), chestX + 3, y + 11, 0x909090, false);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.searchBox.render(graphics, mouseX, mouseY, partialTick);

        // Render virtual item grid over the double-chest slot recesses
        int gridLeft = this.leftPos + 8;
        int gridTop = this.topPos + GRID_TOP;
        int base = scrollOffset * GRID_COLS * GRID_ROWS;
        for (int v = 0; v < GRID_ROWS * GRID_COLS; v++) {
            int visIdx = base + v;
            if (visIdx >= displayItems.size()) {
                break;
            }
            int col = v % GRID_COLS;
            int row = v / GRID_COLS;
            int sx = gridLeft + col * SLOT;
            int sy = gridTop + row * SLOT;
            ItemSorter.VirtualItem item = displayItems.get(visIdx);
            graphics.renderItem(item.stack(), sx, sy);
            graphics.renderItemDecorations(this.font, item.stack(), sx, sy, String.valueOf(item.count()));
        }

        // Hover highlight on the terminal grid slot, matching the vanilla player-slot highlight
        int hovered = slotAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < displayItems.size()) {
            int col = hovered % GRID_COLS;
            int row = (hovered / GRID_COLS) % GRID_ROWS;
            int hx = gridLeft + col * SLOT;
            int hy = gridTop + row * SLOT;
            AbstractContainerScreen.renderSlotHighlight(graphics, hx, hy, 0);
        }

        renderChestSelector(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        int index = slotAt(mouseX, mouseY);
        if (index >= 0 && index < displayItems.size()) {
            ItemSorter.VirtualItem item = displayItems.get(index);
            graphics.renderTooltip(this.font, item.stack(), mouseX, mouseY);
        }
    }
}