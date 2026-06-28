package com.eternal130.tfcaf;

import static com.eternal130.tfcaf.ConfigFile.enableAutoForging;
import static com.eternal130.tfcaf.ConfigFile.enableForgingTip;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.dries007.tfc.client.screen.AnvilScreen;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeSteps;
import net.dries007.tfc.common.capabilities.forge.Forging;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.heat.IHeat;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "tfcaf", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class forgeEvent {
    static ResourceLocation res = new ResourceLocation("tfcaf", "textures/gui/highlight_step.png");

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AnvilScreen) {
            TFCAutoForging.isWaitingForServer = false;
            TFCAutoForging.lastWorkValue = -1;
            TFCAutoForging.tapActivated = false;
            TFCAutoForging.lastRecipeName = null;
            TFCAutoForging.timer = 0;
        }
    }

    @SubscribeEvent
    public static void operationHighlight(ScreenEvent.Render.Post event) {
        try {
            if (event.getScreen() instanceof AnvilScreen) {
                AnvilBlockEntity anvilTE = getTEAnvilTFC((AnvilScreen) event.getScreen());
                Forging forging = anvilTE.getMainInputForging();
                Level level = anvilTE.getLevel();
                if (enableAutoForging.get() || enableForgingTip.get()) {
                    if (forging == null) {
                        return;
                    }
                    int currentPoint = forging.getWork();

                    if (TFCAutoForging.isWaitingForServer) {
                        if (currentPoint != TFCAutoForging.lastWorkValue) {
                            TFCAutoForging.isWaitingForServer = false;
                            TFCAutoForging.lastWorkValue = currentPoint;
                        }
                    }

                    int targetPoint = forging.getWorkTarget();
                    if (targetPoint == 0) {
                        return;
                    }

                    AnvilRecipe anvilRecipe = forging.getRecipe(level);
                    if (anvilRecipe == null) {
                        return;
                    }

                    // Recipe change detection — reset tap state
                    String currentRecipeName = getRecipeOutputName(anvilRecipe, level);
                    if (currentRecipeName != null
                            ? !currentRecipeName.equals(TFCAutoForging.lastRecipeName)
                            : TFCAutoForging.lastRecipeName != null) {
                        TFCAutoForging.lastRecipeName = currentRecipeName;
                        TFCAutoForging.tapActivated = false;
                        TFCAutoForging.lastWorkValue = currentPoint;
                    }

                    // Tap detection — player manually clicked
                    if (!TFCAutoForging.isWaitingForServer && currentPoint != TFCAutoForging.lastWorkValue) {
                        TFCAutoForging.tapActivated = true;
                    }

                    TFCAutoForging.lastWorkValue = currentPoint;

                    int ruleOffset = 0;
                    ForgeSteps steps = forging.getSteps();
                    int[] lastOperations = getRules(anvilRecipe.getRules());
                    for (int i = 0; i < 3; i++) {
                        ruleOffset += Util.operations[lastOperations[i]];
                    }

                    int x = (event.getScreen().width - 176) / 2;
                    int y = (event.getScreen().height - 207) / 2;

                    int offsetNextOperation = Util
                            .nextOperationOffset(targetPoint - currentPoint - ruleOffset, lastOperations, steps);

                    switch (offsetNextOperation) {
                        case 0:
                            x += 71;
                            y += 68;
                            break;
                        case 1:
                            x += 53;
                            y += 68;
                            break;
                        case 2:
                            x += 71;
                            y += 50;
                            break;
                        case 3:
                            x += 53;
                            y += 50;
                            break;
                        case 4:
                            x += 89;
                            y += 50;
                            break;
                        case 5:
                            x += 107;
                            y += 50;
                            break;
                        case 6:
                            x += 89;
                            y += 68;
                            break;
                        case 7:
                            x += 107;
                            y += 68;
                            break;
                        default:
                            return;
                    }

                    if (enableForgingTip.get()) {
                        GuiGraphics guiGraphics = event.getGuiGraphics();
                        drawbox(x, y, guiGraphics.pose());
                    }

                    if (enableAutoForging.get() && TFCAutoForging.timer == 0 && !TFCAutoForging.isWaitingForServer) {
                        // Temperature check
                        ItemStack stack = getInventory(anvilTE).getStackInSlot(0);
                        IHeat heat = HeatCapability.get(stack);
                        if (heat != null && !heat.canWork()) {
                            return;
                        }

                        // Policy decision
                        boolean shouldForge;
                        if (currentRecipeName != null && ConfigFile.isInWhitelist(currentRecipeName)) {
                            ConfigFile.AutoPolicy inner = ConfigFile.getInnerPolicy();
                            shouldForge = (inner == ConfigFile.AutoPolicy.AUTO)
                                    || (inner == ConfigFile.AutoPolicy.TAP && TFCAutoForging.tapActivated);
                        } else {
                            ConfigFile.OuterPolicy outer = ConfigFile.getOuterPolicy();
                            switch (outer) {
                                case AUTO:
                                    shouldForge = true;
                                    break;
                                case TAP:
                                    shouldForge = TFCAutoForging.tapActivated;
                                    break;
                                case NEVER:
                                default:
                                    shouldForge = false;
                                    break;
                            }
                        }

                        if (!shouldForge) {
                            return;
                        }

                        event.getScreen().mouseClicked(x + 8, y + 8, 0);
                        TFCAutoForging.isWaitingForServer = true;
                        TFCAutoForging.timer = ConfigFile.autoForgingCooldown.get();
                    }
                }
            }
        } catch (Exception exception) {
            TFCAutoForging.LOGGER.error("TFC Auto Forging: error in operationHighlight", exception);
        }
    }

    @SubscribeEvent
    public static void timer(TickEvent.ClientTickEvent event) {
        if (TFCAutoForging.timer > 0) {
            TFCAutoForging.timer--;
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent event) {
        if (TFCAutoForging.switchAutoForging.consumeClick()) {
            if (Screen.hasControlDown()) {
                SettingsScreen.open();
            } else {
                enableAutoForging.set(!enableAutoForging.get());
                TFCAutoForging.isWaitingForServer = false;
                TFCAutoForging.tapActivated = false;
                TFCAutoForging.timer = 0;
                TFCAutoForging.lastWorkValue = -1;
                TFCAutoForging.lastRecipeName = null;
                ConfigFile.CONFIG.save();
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    player.sendSystemMessage(Component.translatable(
                            "key.eternal130.switchAutoForging.info", enableAutoForging.get()));
                }
            }
        }
        if (TFCAutoForging.switchForgingTip.consumeClick()) {
            enableForgingTip.set(!enableForgingTip.get());
            ConfigFile.CONFIG.save();
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable(
                        "key.eternal130.switchForgingTip.info", enableForgingTip.get()));
            }
        }
    }

    private static String getRecipeOutputName(AnvilRecipe recipe, Level level) {
        try {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (!result.isEmpty()) {
                var key = ForgeRegistries.ITEMS.getKey(result.getItem());
                if (key != null) {
                    return key.toString();
                }
            }
        } catch (Exception e) {
            TFCAutoForging.LOGGER.warn("TFC Auto Forging: failed to get recipe output for whitelist", e);
        }
        return null;
    }

    private static AnvilBlockEntity getTEAnvilTFC(AnvilScreen gui) throws NoSuchFieldException, IllegalAccessException {
        Field fields = gui.getClass().getSuperclass().getDeclaredField("blockEntity");
        fields.setAccessible(true);
        return (AnvilBlockEntity) fields.get(gui);
    }

    private static AnvilBlockEntity.AnvilInventory getInventory(AnvilBlockEntity te) throws NoSuchFieldException, IllegalAccessException {
        Field fields = te.getClass().getSuperclass().getDeclaredField("inventory");
        fields.setAccessible(true);
        return (AnvilBlockEntity.AnvilInventory) fields.get(te);
    }

    private static int[] getRules(ForgeRule[] rules) {
        int[] lastOperations = new int[3];
        Arrays.fill(lastOperations, -1);
        boolean[] flag = new boolean[3];
        for (ForgeRule rule : rules) {
            if ((rule.ordinal() != 1 && rule.ordinal() % 5 == 1 && !flag[0]) || rule.ordinal() == 2) {
                lastOperations[0] = Util.operationsTfc.get(rule.ordinal());
                flag[0] = true;
            } else if (rule.ordinal() % 5 == 3 && !flag[1]) {
                lastOperations[1] = Util.operationsTfc.get(rule.ordinal());
                flag[1] = true;
            } else if (rule.ordinal() % 5 == 4 && !flag[2]) {
                lastOperations[2] = Util.operationsTfc.get(rule.ordinal());
                flag[2] = true;
            }
        }
        for (ForgeRule rule : rules) {
            if ((rule.ordinal() != 2 && rule.ordinal() % 5 == 2) || rule.ordinal() == 1) {
                if (flag[2]) {
                    lastOperations[1] = Util.operationsTfc.get(rule.ordinal());
                    flag[1] = true;
                } else {
                    lastOperations[2] = Util.operationsTfc.get(rule.ordinal());
                    flag[2] = true;
                }
            }
        }
        for (ForgeRule rule : rules) {
            if (rule.ordinal() % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    if (!flag[i]) {
                        lastOperations[i] = Util.operationsTfc.get(rule.ordinal());
                        flag[i] = true;
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!flag[i]) {
                lastOperations[i] = 4;
            }
        }
        return lastOperations;
    }

    private static void drawbox(int x, int y, PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, res);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int frame = (int) ((Minecraft.getInstance().level.getGameTime() / ConfigFile.highlightStepCooldown.get()) % ConfigFile.totalFrames.get());
        float uMin = (frame % ConfigFile.framesPerRow.get()) / (float) ConfigFile.framesPerRow.get();
        float vMin = (frame / ConfigFile.framesPerRow.get()) / (float) ConfigFile.framesPerColumn.get();
        float uMax = uMin + 1.0f / ConfigFile.framesPerRow.get();
        float vMax = vMin + 1.0f / ConfigFile.framesPerColumn.get();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, (float) (x - (ConfigFile.textureWidth.get() - 16) / 2.0), (float) (y + (ConfigFile.textureHeight.get() - 16) / 2.0 + 16), 0).uv(uMin, vMax).endVertex();
        bufferBuilder.vertex(matrix, (float) (x + (ConfigFile.textureWidth.get() - 16) / 2.0 + 16), (float) (y + (ConfigFile.textureHeight.get() - 16) / 2.0 + 16), 0).uv(uMax, vMax).endVertex();
        bufferBuilder.vertex(matrix, (float) (x + (ConfigFile.textureWidth.get() - 16) / 2.0 + 16), (float) (y - (ConfigFile.textureHeight.get() - 16) / 2.0), 0).uv(uMax, vMin).endVertex();
        bufferBuilder.vertex(matrix, (float) (x - (ConfigFile.textureWidth.get() - 16) / 2.0), (float) (y - (ConfigFile.textureHeight.get() - 16) / 2.0), 0).uv(uMin, vMin).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();
    }
}
