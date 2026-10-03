package com.pycoder.taczintetra.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/** 保存在 config/taczintetra.toml 中的运行时规则。 */
public final class TaczInTetraForgeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DISABLE_NATIVE_ITEMS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_NATIVE_RECIPES;
    public static final ForgeConfigSpec.BooleanValue DISABLE_NATIVE_WORKBENCHES;
    public static final ForgeConfigSpec.BooleanValue ONLY_SEMI_FINISHED_PARTS;
    public static final ForgeConfigSpec.BooleanValue ONLY_REPAIR_AGENTS;
    public static final ForgeConfigSpec.EnumValue<JeiMode> JEI_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> INITIAL_BARREL;
    public static final ForgeConfigSpec.ConfigValue<String> INITIAL_BODY;
    public static final ForgeConfigSpec.ConfigValue<String> INITIAL_MAGAZINE;
    public static final ForgeConfigSpec.ConfigValue<String> INITIAL_BARREL_MATERIAL;
    public static final ForgeConfigSpec.ConfigValue<String> INITIAL_BODY_MATERIAL;
    public static final ForgeConfigSpec.ConfigValue<String> INITIAL_MAGAZINE_MATERIAL;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> INITIAL_RECIPE;
    public static final ForgeConfigSpec.BooleanValue FORCE_DOT_CROSSHAIR;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("native_tacz");
        DISABLE_NATIVE_ITEMS = b.comment("Disable native TaCZ guns and native gun items").define("disable_items", true);
        DISABLE_NATIVE_RECIPES = b.comment("Disable native TaCZ crafting recipes").define("disable_recipes", true);
        DISABLE_NATIVE_WORKBENCHES = b.comment("Disable native TaCZ gun, ammo and attachment workbenches").define("disable_workbenches", true);
        b.pop();
        b.push("crafting_rules");
        ONLY_SEMI_FINISHED_PARTS = b.define("only_semi_finished_parts", true);
        ONLY_REPAIR_AGENTS = b.define("only_repair_agents", true);
        b.pop();
        b.push("jei");
        JEI_MODE = b.defineEnum("mode", JeiMode.HIDE);
        b.pop();
        b.push("initial_gun");
        INITIAL_BARREL = b.define("barrel", "9mm");
        INITIAL_BODY = b.define("body", "pistol");
        INITIAL_MAGAZINE = b.define("magazine", "standard");
        INITIAL_BARREL_MATERIAL = b.define("barrel_material", "wood");
        INITIAL_BODY_MATERIAL = b.define("body_material", "wood");
        INITIAL_MAGAZINE_MATERIAL = b.define("magazine_material", "wood");
        INITIAL_RECIPE = b.defineList("recipe", java.util.List.of("#minecraft:planks", "#minecraft:planks", "#minecraft:planks", "#minecraft:planks", "minecraft:stick", "", "#minecraft:wooden_slabs", "", ""), value -> value instanceof String);
        b.pop();
        b.push("client");
        FORCE_DOT_CROSSHAIR = b.define("force_dot_crosshair_for_modular_gun", true);
        b.pop();
        b.push("debug");
        DEBUG_LOGGING = b.define("logging", false);
        b.pop();
        SPEC = b.build();
    }

    private TaczInTetraForgeConfig() { }

    public enum JeiMode { HIDE, DISABLED }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "taczintetra.toml");
    }
}
