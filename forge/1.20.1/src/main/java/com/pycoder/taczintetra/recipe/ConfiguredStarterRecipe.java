package com.pycoder.taczintetra.recipe;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import com.pycoder.taczintetra.item.GunModuleSlots;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.slf4j.Logger;

import java.util.List;
import se.mickelus.tetra.items.modular.IModularItem;

/** 根据可编辑 Forge TOML 创建初始配方，不替换其他数据包配方。 */
public final class ConfiguredStarterRecipe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private ConfiguredStarterRecipe() { }

    public static Recipe<?> from(Recipe<?> original) {
        if (!(original instanceof ShapedRecipe shaped)) return original;
        try {
            List<? extends String> values = TaczInTetraForgeConfig.INITIAL_RECIPE.get();
            if (values.size() != 9) return original;
            NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
            for (int index = 0; index < values.size(); index++) {
                ingredients.set(index, parseIngredient(values.get(index)));
            }
            ItemStack result = configuredStarterResult(shaped.getResultItem(null));
            return new ShapedRecipe(shaped.getId(), shaped.getGroup(), CraftingBookCategory.MISC,
                    3, 3, ingredients, result, shaped.showNotification());
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid initial_gun.recipe in taczintetra.toml; keeping the datapack recipe", exception);
            return original;
        }
    }

    private static ItemStack configuredStarterResult(ItemStack original) {
        ItemStack result = original.copy();
        if (!(result.getItem() instanceof IModularItem)) return result;
        String body = normalizedId(TaczInTetraForgeConfig.INITIAL_BODY.get(), "pistol");
        String barrel = normalizedId(TaczInTetraForgeConfig.INITIAL_BARREL.get(), "9mm");
        String magazine = normalizedId(TaczInTetraForgeConfig.INITIAL_MAGAZINE.get(), "standard");
        String bodyMaterial = normalizedId(TaczInTetraForgeConfig.INITIAL_BODY_MATERIAL.get(), "wood");
        String barrelMaterial = normalizedId(TaczInTetraForgeConfig.INITIAL_BARREL_MATERIAL.get(), "wood");
        String magazineMaterial = normalizedId(TaczInTetraForgeConfig.INITIAL_MAGAZINE_MATERIAL.get(), "wood");
        IModularItem.putModuleInSlot(result, GunModuleSlots.BODY,
                "taczintetra/body/" + body, "taczintetra/" + bodyMaterial + "/");
        IModularItem.putModuleInSlot(result, GunModuleSlots.BARREL,
                "taczintetra/barrel/" + barrel, "taczintetra/" + barrelMaterial + "/");
        IModularItem.putModuleInSlot(result, GunModuleSlots.MAGAZINE,
                "taczintetra/magazine/" + magazine, "taczintetra/" + magazineMaterial + "/");
        return result;
    }

    private static String normalizedId(String value, String fallback) {
        if (value == null || value.isBlank() || !value.matches("[a-z0-9_]+")) return fallback;
        return value;
    }

    private static Ingredient parseIngredient(String value) {
        if (value == null || value.isBlank() || "minecraft:air".equals(value)) return Ingredient.EMPTY;
        if (value.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring(1));
            if (id == null) throw new IllegalArgumentException("Invalid item tag: " + value);
            return Ingredient.of(TagKey.create(Registries.ITEM, id));
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid item id: " + value);
        Item item = BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + value));
        return Ingredient.of(item);
    }
}
