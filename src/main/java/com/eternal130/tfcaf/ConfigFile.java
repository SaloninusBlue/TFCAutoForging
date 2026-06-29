package com.eternal130.tfcaf;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

public class ConfigFile {
    public static final ModConfigSpec.BooleanValue enableAutoForging;
    public static final ModConfigSpec.BooleanValue enableForgingTip;
    public static final ModConfigSpec.IntValue autoForgingCooldown;
    public static final ModConfigSpec.IntValue highlightStepCooldown;
    public static final ModConfigSpec.IntValue totalFrames;
    public static final ModConfigSpec.IntValue framesPerRow;
    public static final ModConfigSpec.IntValue framesPerColumn;
    public static final ModConfigSpec.IntValue textureWidth;
    public static final ModConfigSpec.IntValue textureHeight;

    public static final ModConfigSpec.ConfigValue<String> innerPolicy;
    public static final ModConfigSpec.ConfigValue<String> outerPolicy;
    public static final ModConfigSpec.ConfigValue<String> whitelistRaw;
    public static final ModConfigSpec.ConfigValue<String> forgeSpeed;

    public static ModConfigSpec CONFIG;

    private static Set<String> whitelistCache = Collections.emptySet();
    private static String lastRawWhitelist = "";

    public enum AutoPolicy { TAP, AUTO }
    public enum OuterPolicy { NEVER, TAP, AUTO }
    public enum ForgeSpeed { EXTREME, FAST, MODERATE, SAFE }

    public ConfigFile() {}

    static {
        ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        BUILDER.comment("General settings").push("general");
        enableAutoForging = BUILDER.comment("Is the auto forging enabled?")
                .define("enableAutoForging", true);
        enableForgingTip = BUILDER.comment("Is the next recommended step highlighted?")
                .define("enableDebug", true);
        autoForgingCooldown = BUILDER.comment("cooldown of each automatic forging step(tick)")
                .defineInRange("autoForgingCooldown", 1, 1, 200);
        highlightStepCooldown = BUILDER.comment("The duration of each frame of the highlight step(tick)")
                .defineInRange("highlightStepCooldown", 2, 1, 1000);
        totalFrames = BUILDER.comment("Total number of frames in the highlight step animation")
                .defineInRange("totalFrames", 16, 1, 100);
        framesPerRow = BUILDER.comment("Number of frames per row in the texture")
                .defineInRange("framesPerRow", 4, 1, 10);
        framesPerColumn = BUILDER.comment("Number of frames per column in the texture")
                .defineInRange("framesPerColumn", 4, 1, 10);
        textureWidth = BUILDER.comment("texture width of highlight step animation(px)")
                .defineInRange("textureWidth", 18, 1, 1024);
        textureHeight = BUILDER.comment("texture height of highlight step animation(px)")
                .defineInRange("textureHeight", 18, 1, 1024);
        BUILDER.pop();

        BUILDER.comment("Whitelist and forging policy settings").push("whitelist");
        innerPolicy = BUILDER.comment("Policy for items INSIDE whitelist: TAP or AUTO")
                .define("innerPolicy", "AUTO");
        outerPolicy = BUILDER.comment("Policy for items OUTSIDE whitelist: NEVER, TAP, or AUTO")
                .define("outerPolicy", "NEVER");
        whitelistRaw = BUILDER.comment("Comma-separated list of item registry names in the whitelist")
                .define("whitelistItems", "");
        forgeSpeed = BUILDER.comment("Forging speed: EXTREME, FAST, MODERATE, SAFE")
                .define("forgeSpeed", "SAFE");
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }

    public static AutoPolicy getInnerPolicy() {
        try { return AutoPolicy.valueOf(innerPolicy.get().toUpperCase()); }
        catch (IllegalArgumentException e) { return AutoPolicy.AUTO; }
    }

    public static void setInnerPolicy(AutoPolicy policy) {
        innerPolicy.set(policy.name());
    }

    public static OuterPolicy getOuterPolicy() {
        try { return OuterPolicy.valueOf(outerPolicy.get().toUpperCase()); }
        catch (IllegalArgumentException e) { return OuterPolicy.NEVER; }
    }

    public static void setOuterPolicy(OuterPolicy policy) {
        outerPolicy.set(policy.name());
    }

    public static ForgeSpeed getForgeSpeed() {
        try { return ForgeSpeed.valueOf(forgeSpeed.get().toUpperCase()); }
        catch (IllegalArgumentException e) { return ForgeSpeed.SAFE; }
    }

    public static void setForgeSpeed(ForgeSpeed speed) {
        forgeSpeed.set(speed.name());
    }

    public static int getCooldown() {
        return switch (getForgeSpeed()) {
            case EXTREME -> 0;
            case FAST -> 5;
            case MODERATE -> 10;
            case SAFE -> 20;
        };
    }

    public static List<String> getWhitelist() {
        String raw = whitelistRaw.get();
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    public static void setWhitelist(List<String> items) {
        whitelistRaw.set(String.join(",", items));
        whitelistCache = items.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(items);
        lastRawWhitelist = whitelistRaw.get();
        CONFIG.save();
    }

    public static boolean isInWhitelist(String registryName) {
        String raw = whitelistRaw.get();
        if (!Objects.equals(raw, lastRawWhitelist)) {
            lastRawWhitelist = raw;
            if (raw.isEmpty()) {
                whitelistCache = Collections.emptySet();
            } else {
                Set<String> set = new HashSet<>();
                for (String item : raw.split(",")) {
                    String t = item.trim();
                    if (!t.isEmpty()) {
                        set.add(t);
                    }
                }
                whitelistCache = set;
            }
        }
        return whitelistCache.contains(registryName);
    }

    public static void save() {
        CONFIG.save();
    }
}
