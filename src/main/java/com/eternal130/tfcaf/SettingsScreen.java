package com.eternal130.tfcaf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int SLOTS_PER_ROW = 3;
    private static final int TOTAL_SLOTS = 9;

    private final SimpleContainer filterInventory;
    private final int[] slotX = new int[TOTAL_SLOTS];
    private final int[] slotY = new int[TOTAL_SLOTS];

    private ConfigFile.AutoPolicy localInner;
    private ConfigFile.OuterPolicy localOuter;

    private Button innerLeftBtn;
    private Button innerRightBtn;
    private Button outerLeftBtn;
    private Button outerRightBtn;
    private Button doneBtn;

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
        int gridWidth = SLOTS_PER_ROW * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int gridStartX = (this.width - gridWidth) / 2;
        int gridStartY = 40;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int idx = row * SLOTS_PER_ROW + col;
                slotX[idx] = gridStartX + col * (SLOT_SIZE + SLOT_GAP);
                slotY[idx] = gridStartY + row * (SLOT_SIZE + SLOT_GAP);
            }
        }

        int centerX = this.width / 2;
        int arrowSize = 20;

        // Inner policy row (y = 110)
        int innerY = 108;
        innerLeftBtn = Button.builder(Component.literal("◄"), b -> cycleInnerLeft())
                .bounds(centerX - 68, innerY, arrowSize, 20).build();
        innerRightBtn = Button.builder(Component.literal("►"), b -> cycleInnerRight())
                .bounds(centerX + 48, innerY, arrowSize, 20).build();
        addRenderableWidget(innerLeftBtn);
        addRenderableWidget(innerRightBtn);

        // Outer policy row (y = 138)
        int outerY = 138;
        outerLeftBtn = Button.builder(Component.literal("◄"), b -> cycleOuterLeft())
                .bounds(centerX - 68, outerY, arrowSize, 20).build();
        outerRightBtn = Button.builder(Component.literal("►"), b -> cycleOuterRight())
                .bounds(centerX + 48, outerY, arrowSize, 20).build();
        addRenderableWidget(outerLeftBtn);
        addRenderableWidget(outerRightBtn);

        // Done button
        doneBtn = Button.builder(Component.translatable("gui.done"), b -> onDone())
                .bounds(centerX - 40, 170, 80, 20).build();
        addRenderableWidget(doneBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        // Whitelist label
        guiGraphics.drawString(this.font,
                Component.translatable("tfcaf.settings.whitelist"),
                8, 20, 0xA0A0A0);

        // Render filter slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            renderSlot(guiGraphics, i, mouseX, mouseY);
        }

        // Policy labels and values
        String innerLabel = Component.translatable("tfcaf.settings.innerPolicy").getString();
        String innerVal = Component.translatable("tfcaf.autoPolicy." + localInner.name().toLowerCase()).getString();
        guiGraphics.drawString(this.font, innerLabel, 8, 112, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, innerVal, this.width / 2, 112, 0xFFFFFF);

        String outerLabel = Component.translatable("tfcaf.settings.outerPolicy").getString();
        String outerVal = Component.translatable("tfcaf.outerPolicy." + localOuter.name().toLowerCase()).getString();
        guiGraphics.drawString(this.font, outerLabel, 8, 142, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, outerVal, this.width / 2, 142, 0xFFFFFF);

        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Tooltip for hovered slot
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (isMouseOverSlot(i, mouseX, mouseY)) {
                ItemStack stack = filterInventory.getItem(i);
                if (!stack.isEmpty()) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (isMouseOverSlot(i, mx, my)) {
                if (button == 1) {
                    // Right click → clear
                    filterInventory.setItem(i, ItemStack.EMPTY);
                    return true;
                }
                if (button == 0) {
                    // Left click → set from carried or clear
                    if (Minecraft.getInstance().player == null) {
                        return false;
                    }
                    ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
                    if (!carried.isEmpty()) {
                        filterInventory.setItem(i, carried.copyWithCount(1));
                    } else {
                        filterInventory.setItem(i, ItemStack.EMPTY);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderSlot(GuiGraphics guiGraphics, int index, int mouseX, int mouseY) {
        int x = slotX[index];
        int y = slotY[index];

        boolean hovered = isMouseOverSlot(index, mouseX, mouseY);
        int color = hovered ? 0x80FFFFFF : 0x40FFFFFF;

        // Slot background
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
        if (hovered) {
            guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
        }

        // Item
        ItemStack stack = filterInventory.getItem(index);
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x + 1, y + 1);
            guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private boolean isMouseOverSlot(int index, int mouseX, int mouseY) {
        return mouseX >= slotX[index] && mouseX < slotX[index] + SLOT_SIZE
                && mouseY >= slotY[index] && mouseY < slotY[index] + SLOT_SIZE;
    }

    private void cycleInnerLeft() {
        localInner = (localInner == ConfigFile.AutoPolicy.AUTO)
                ? ConfigFile.AutoPolicy.TAP
                : ConfigFile.AutoPolicy.AUTO;
    }

    private void cycleInnerRight() {
        cycleInnerLeft(); // 2 values, left and right do the same
    }

    private void cycleOuterLeft() {
        switch (localOuter) {
            case NEVER -> localOuter = ConfigFile.OuterPolicy.TAP;
            case TAP -> localOuter = ConfigFile.OuterPolicy.AUTO;
            case AUTO -> localOuter = ConfigFile.OuterPolicy.NEVER;
        }
    }

    private void cycleOuterRight() {
        switch (localOuter) {
            case NEVER -> localOuter = ConfigFile.OuterPolicy.AUTO;
            case AUTO -> localOuter = ConfigFile.OuterPolicy.TAP;
            case TAP -> localOuter = ConfigFile.OuterPolicy.NEVER;
        }
    }

    private void onDone() {
        // Save whitelist
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

        // Save policies
        ConfigFile.setInnerPolicy(localInner);
        ConfigFile.setOuterPolicy(localOuter);
        ConfigFile.save();

        this.onClose();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }
}
