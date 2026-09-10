package com.retiredroca.storagecentral.client;

import java.util.List;

import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.network.Networking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

    private List<ItemSorter.VirtualItem> displayItems = List.of();
    private int scrollOffset = 0;
    private int lastDataVersion = -1;
    private EditBox searchBox;
    private int searchBoxX;
    private int searchBoxY;
    private boolean draggingBar = false;
    private int dragDX = 0;
    private int dragDY = 0;

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
        this.searchBoxX = Math.max(0, Math.min(this.width - SEARCH_BOX_W,
                this.leftPos + (this.imageWidth - SEARCH_BOX_W) / 2));
        this.searchBoxY = Math.max(0, Math.min(this.height - SEARCH_BOX_H,
                this.topPos + this.imageHeight + 8));
        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, SEARCH_BOX_W, SEARCH_BOX_H,
                Component.translatable("gui.storage_central.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(s -> rebuild());
        this.addWidget(this.searchBox);
        rebuild();
    }

    private void rebuild() {
        if (this.minecraft != null) {
            this.displayItems = ItemSorter.filter(
                    ItemSorter.build(menu.getServerItems(), menu.getServerCounts()),
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, double horizontalDelta) {
        if (hasShiftDown() || hasControlDown()) {
            return super.mouseScrolled(mouseX, mouseY, delta, horizontalDelta);
        }
        int maxOffset = Math.max(0, (int) Math.ceil(displayItems.size() / (double) (GRID_COLS * GRID_ROWS)) - 1);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (delta > 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseX >= this.leftPos + 7 && mouseX <= this.leftPos + 169
                && mouseY >= searchBoxY - 1 && mouseY <= searchBoxY + SEARCH_BOX_H + 1) {
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