package com.eternal130.tfcaf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int TOTAL_SLOTS = 9;

    private final int imageWidth = 210;
    private final int imageHeight = 185;
    private int leftPos;
    private int topPos;

    private final SimpleContainer filterInventory;
    private final int[] slotX = new int[TOTAL_SLOTS];
    private final int[] slotY = new int[TOTAL_SLOTS];

    private final int[] bagX = new int[36];
    private final int[] bagY = new int[36];

    private ConfigFile.AutoPolicy localInner;
    private ConfigFile.OuterPolicy localOuter;
    private ConfigFile.ForgeSpeed localSpeed;

    private ItemStack phantomCursor = ItemStack.EMPTY;

    protected SettingsScreen() {
        super(Component.translatable("tfcaf.settings.title"));
        filterInventory = new SimpleContainer(TOTAL_SLOTS);
        localInner = ConfigFile.getInnerPolicy();
        localOuter = ConfigFile.getOuterPolicy();
        localSpeed = ConfigFile.getForgeSpeed();

        List<String> saved = ConfigFile.getWhitelist();
        for (int i = 0; i < Math.min(TOTAL_SLOTS, saved.size()); i++) {
            String name = saved.get(i);
            if (name != null && !name.isEmpty()) {
                ResourceLocation rl = ResourceLocation.tryParse(name);
                if (rl != null) {
                    var item = BuiltInRegistries.ITEM.get(rl);
                    if (item != null) {
                        filterInventory.setItem(i, new ItemStack(item));
                    }
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int buttonWidth = 110;
        int buttonHeight = 20;
        int buttonX = this.leftPos + 10;

        var innerBtn = addRenderableWidget(CycleButton.<ConfigFile.AutoPolicy>builder(policy ->
                Component.translatable("tfcaf.autoPolicy." + policy.name().toLowerCase()))
            .withValues(ConfigFile.AutoPolicy.values())
            .withInitialValue(localInner)
            .create(buttonX, this.topPos + 25, buttonWidth, buttonHeight,
                    Component.translatable("tfcaf.settings.innerPolicy"), (button, value) -> {
                localInner = value;
                savePolicies();
            }));

        var outerBtn = addRenderableWidget(CycleButton.<ConfigFile.OuterPolicy>builder(policy ->
                Component.translatable("tfcaf.outerPolicy." + policy.name().toLowerCase()))
            .withValues(ConfigFile.OuterPolicy.values())
            .withInitialValue(localOuter)
            .create(buttonX, this.topPos + 50, buttonWidth, buttonHeight,
                    Component.translatable("tfcaf.settings.outerPolicy"), (button, value) -> {
                localOuter = value;
                savePolicies();
            }));

        var speedBtn = addRenderableWidget(CycleButton.<ConfigFile.ForgeSpeed>builder(speed ->
                Component.translatable("tfcaf.forgeSpeed." + speed.name().toLowerCase()))
            .withValues(ConfigFile.ForgeSpeed.values())
            .withInitialValue(localSpeed)
            .create(buttonX, this.topPos + 75, buttonWidth, buttonHeight,
                    Component.translatable("tfcaf.forgeSpeed"), (button, value) -> {
                localSpeed = value;
                savePolicies();
            }));

        int gridX = this.leftPos + 135;
        int gridY = this.topPos + 95 - 3 * SLOT_SIZE;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                slotX[idx] = gridX + col * SLOT_SIZE;
                slotY[idx] = gridY + row * SLOT_SIZE;
            }
        }

        int bagStartX = this.leftPos + (this.imageWidth - (9 * SLOT_SIZE)) / 2;
        int bagStartY = this.topPos + 100;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col;
                bagX[idx] = bagStartX + col * SLOT_SIZE;
                bagY[idx] = bagStartY + row * SLOT_SIZE;
            }
        }
        for (int col = 0; col < 9; col++) {
            int idx = col;
            bagX[idx] = bagStartX + col * SLOT_SIZE;
            bagY[idx] = bagStartY + 3 * SLOT_SIZE + 4;
        }
    }

    @Override
    protected void setInitialFocus() {
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFFFFFFFF);
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFFFFFFFF);
        guiGraphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF555555);
        guiGraphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF555555);

        guiGraphics.drawString(this.font, this.title.getString(), this.leftPos + 8, this.topPos + 8, 0xFF404040, false);

        String whitelistLabel = Component.translatable("tfcaf.settings.whitelist").getString();
        int whitelistTextX = this.leftPos + 135 + (3 * SLOT_SIZE) / 2;
        guiGraphics.drawString(this.font, whitelistLabel,
                whitelistTextX - this.font.width(whitelistLabel) / 2, this.topPos + 12, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().player == null) return;

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            renderSlot(guiGraphics, slotX[i], slotY[i], filterInventory.getItem(i), mouseX, mouseY);
        }

        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < 36; i++) {
            renderSlot(guiGraphics, bagX[i], bagY[i], inv.getItem(i), mouseX, mouseY);
        }

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (mouseInSlot(mouseX, mouseY, slotX[i], slotY[i])) {
                ItemStack stack = filterInventory.getItem(i);
                if (!stack.isEmpty()) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }

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

        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (mouseInSlot(mx, my, bagX[i], bagY[i])) {
                ItemStack bagStack = inv.getItem(i);
                if (bagStack.isEmpty()) {
                    phantomCursor = ItemStack.EMPTY;
                    return true;
                }
                if (button == 1) {
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
                var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
        ConfigFile.setForgeSpeed(localSpeed);
        ConfigFile.save();
    }

    private void renderSlot(GuiGraphics guiGraphics, int x, int y,
                            ItemStack stack, int mouseX, int mouseY) {
        boolean hovered = mouseInSlot(mouseX, mouseY, x, y);

        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);

        if (hovered) {
            guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FFFFFF);
        }

        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x + 1, y + 1);
            guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private boolean mouseInSlot(int mx, int my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    public static void open() {
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }
}
