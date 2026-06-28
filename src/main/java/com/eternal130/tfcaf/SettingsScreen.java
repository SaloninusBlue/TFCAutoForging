package com.eternal130.tfcaf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int TOTAL_SLOTS = 9;

    private final SimpleContainer filterInventory;
    private final int[] slotX = new int[TOTAL_SLOTS];
    private final int[] slotY = new int[TOTAL_SLOTS];

    private final int[] bagX = new int[36];
    private final int[] bagY = new int[36];

    private ConfigFile.AutoPolicy localInner;
    private ConfigFile.OuterPolicy localOuter;

    private ItemStack phantomCursor = ItemStack.EMPTY;

    protected SettingsScreen() {
        super(Component.translatable("tfcaf.settings.title"));
        filterInventory = new SimpleContainer(TOTAL_SLOTS);
        localInner = ConfigFile.getInnerPolicy();
        localOuter = ConfigFile.getOuterPolicy();

        List<String> saved = ConfigFile.getWhitelist();
        for (int i = 0; i < Math.min(TOTAL_SLOTS, saved.size()); i++) {
            String name = saved.get(i);
            if (name != null && !name.isEmpty()) {
                ResourceLocation rl = ResourceLocation.tryParse(name);
                if (rl != null) {
                    var item = ForgeRegistries.ITEMS.getValue(rl);
                    if (item != null) {
                        filterInventory.setItem(i, new ItemStack(item));
                    }
                }
            }
        }
    }

    @Override
    protected void init() {
        // Whitelist grid — left side
        int gridX = 30;
        int gridY = 26;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                slotX[idx] = gridX + col * (SLOT_SIZE + SLOT_GAP);
                slotY[idx] = gridY + row * (SLOT_SIZE + SLOT_GAP);
            }
        }

        // Policy buttons — right side
        int btnLeftX = 120;
        int btnRightX = 200;
        int arrowSize = 20;

        addRenderableWidget(Button.builder(Component.literal("◄"), b -> cycleInnerLeft())
                .bounds(btnLeftX, 26, arrowSize, 20).build());
        addRenderableWidget(Button.builder(Component.literal("►"), b -> cycleInnerRight())
                .bounds(btnRightX, 26, arrowSize, 20).build());

        addRenderableWidget(Button.builder(Component.literal("◄"), b -> cycleOuterLeft())
                .bounds(btnLeftX, 58, arrowSize, 20).build());
        addRenderableWidget(Button.builder(Component.literal("►"), b -> cycleOuterRight())
                .bounds(btnRightX, 58, arrowSize, 20).build());

        // Backpack slot positions — 9 per row, 4 rows
        int bagStartX = (this.width - (9 * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP)) / 2;
        int bagStartY = 100;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col; // slots 9-35 (main inventory)
                bagX[idx] = bagStartX + col * (SLOT_SIZE + SLOT_GAP);
                bagY[idx] = bagStartY + row * (SLOT_SIZE + SLOT_GAP);
            }
        }
        for (int col = 0; col < 9; col++) {
            int idx = col; // slots 0-8 (hotbar)
            bagX[idx] = bagStartX + col * (SLOT_SIZE + SLOT_GAP);
            bagY[idx] = bagStartY + 3 * (SLOT_SIZE + SLOT_GAP);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().player == null) return;
        this.renderBackground(guiGraphics);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        // Whitelist label
        guiGraphics.drawString(this.font,
                Component.translatable("tfcaf.settings.whitelist"),
                30, 10, 0xA0A0A0);

        // Render whitelist slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            renderSlot(guiGraphics, i, slotX[i], slotY[i], filterInventory.getItem(i), mouseX, mouseY);
        }

        // Render bag slots
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < 36; i++) {
            renderSlot(guiGraphics, i, bagX[i], bagY[i], inv.getItem(i), mouseX, mouseY);
        }

        // Policy labels
        String innerVal = Component.translatable("tfcaf.autoPolicy." + localInner.name().toLowerCase()).getString();
        guiGraphics.drawString(this.font,
                Component.translatable("tfcaf.settings.innerPolicy"), 120, 12, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, innerVal, 170, 28, 0xFFFFFF);

        String outerVal = Component.translatable("tfcaf.outerPolicy." + localOuter.name().toLowerCase()).getString();
        guiGraphics.drawString(this.font,
                Component.translatable("tfcaf.settings.outerPolicy"), 120, 46, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, outerVal, 170, 60, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Tooltip for whitelist slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (mouseInSlot(mouseX, mouseY, slotX[i], slotY[i])) {
                ItemStack stack = filterInventory.getItem(i);
                if (!stack.isEmpty()) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }

        // Render phantom cursor
        if (!phantomCursor.isEmpty()) {
            guiGraphics.renderItem(phantomCursor, mouseX - 8, mouseY - 8);
            guiGraphics.renderItemDecorations(this.font, phantomCursor, mouseX - 8, mouseY - 8);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Minecraft.getInstance().player == null) return false;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Whitelist slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (mouseInSlot(mx, my, slotX[i], slotY[i])) {
                if (button == 1) {
                    filterInventory.setItem(i, ItemStack.EMPTY);
                    phantomCursor = ItemStack.EMPTY;
                    saveWhitelist();
                    return true;
                }
                if (button == 0) {
                    if (!phantomCursor.isEmpty()) {
                        filterInventory.setItem(i, phantomCursor.copy());
                        phantomCursor = ItemStack.EMPTY;
                        saveWhitelist();
                    } else if (!filterInventory.getItem(i).isEmpty()) {
                        phantomCursor = filterInventory.getItem(i).copy();
                        filterInventory.setItem(i, ItemStack.EMPTY);
                        saveWhitelist();
                    }
                    return true;
                }
            }
        }

        // Bag slots
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (mouseInSlot(mx, my, bagX[i], bagY[i])) {
                ItemStack bagStack = inv.getItem(i);
                if (bagStack.isEmpty()) {
                    phantomCursor = ItemStack.EMPTY;
                    return true;
                }

                if (button == 1) {
                    // Right-click → add to first empty whitelist slot, save
                    for (int j = 0; j < TOTAL_SLOTS; j++) {
                        if (filterInventory.getItem(j).isEmpty()) {
                            filterInventory.setItem(j, bagStack.copyWithCount(1));
                            phantomCursor = ItemStack.EMPTY;
                            saveWhitelist();
                            return true;
                        }
                    }
                    phantomCursor = ItemStack.EMPTY;
                    return true;
                }
                if (button == 0) {
                    phantomCursor = bagStack.copyWithCount(1);
                    return true;
                }
            }
        }

        // Clicked empty area → clear phantom cursor
        if (button == 0) {
            phantomCursor = ItemStack.EMPTY;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void saveWhitelist() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack stack = filterInventory.getItem(i);
            if (!stack.isEmpty()) {
                var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null) {
                    names.add(key.toString());
                }
            }
        }
        ConfigFile.setWhitelist(names);
    }

    private void savePolicies() {
        ConfigFile.setInnerPolicy(localInner);
        ConfigFile.setOuterPolicy(localOuter);
        ConfigFile.save();
    }

    private void renderSlot(GuiGraphics guiGraphics, int index, int x, int y,
                            ItemStack stack, int mouseX, int mouseY) {
        boolean hovered = mouseInSlot(mouseX, mouseY, x, y);

        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        if (hovered) {
            guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xBBFFFFFF);
        }

        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x + 1, y + 1);
            guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private boolean mouseInSlot(int mx, int my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    private void cycleInnerLeft() {
        localInner = (localInner == ConfigFile.AutoPolicy.AUTO)
                ? ConfigFile.AutoPolicy.TAP
                : ConfigFile.AutoPolicy.AUTO;
        savePolicies();
    }

    private void cycleInnerRight() {
        cycleInnerLeft();
    }

    private void cycleOuterLeft() {
        switch (localOuter) {
            case NEVER -> localOuter = ConfigFile.OuterPolicy.TAP;
            case TAP -> localOuter = ConfigFile.OuterPolicy.AUTO;
            case AUTO -> localOuter = ConfigFile.OuterPolicy.NEVER;
        }
        savePolicies();
    }

    private void cycleOuterRight() {
        switch (localOuter) {
            case NEVER -> localOuter = ConfigFile.OuterPolicy.AUTO;
            case AUTO -> localOuter = ConfigFile.OuterPolicy.TAP;
            case TAP -> localOuter = ConfigFile.OuterPolicy.NEVER;
        }
        savePolicies();
    }

    public static void open() {
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }
}
