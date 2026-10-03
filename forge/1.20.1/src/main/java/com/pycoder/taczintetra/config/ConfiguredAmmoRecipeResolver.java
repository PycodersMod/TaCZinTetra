package com.pycoder.taczintetra.config;

import com.pycoder.taczintetra.logic.AmmoRecipeDefinition;
import com.pycoder.taczintetra.item.GunModuleSlots;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;

/** 解析配置的换弹配方，不将弹药数值写入运行时代码。 */
public final class ConfiguredAmmoRecipeResolver {
    private ConfiguredAmmoRecipeResolver() {
    }

    public static AmmoRecipeDefinition select(ModuleConfig config, String projectile, String feedType) {
        if (config == null) return null;
        return config.ammoRecipes().stream()
                .filter(recipe -> projectile != null && projectile.equals(recipe.projectile()))
                .filter(recipe -> feedType != null && feedType.equals(recipe.feedType()))
                .findFirst()
                .orElse(null);
    }

    public static AmmoRecipeDefinition select(ModuleConfig config, IModularItem item, ItemStack stack) {
        TetraItemStackProfileResolver.SelectedModules modules = TetraItemStackProfileResolver.selectedModules(
                item, stack, GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (modules == null || config == null) return null;
        String barrelId = modules.barrel().variantId();
        String feedType = config.magazines().stream()
                .filter(magazine -> magazine.id().equals(modules.magazine().variantId()))
                .map(ModuleConfig.Magazine::feedType)
                .findFirst()
                .orElse(null);
        String projectile = config.barrels().stream()
                .filter(barrel -> barrel.id().equals(barrelId))
                .map(ModuleConfig.Barrel::projectileType)
                .findFirst()
                .orElse(null);
        return select(config, projectile, feedType);
    }

    /** 返回所选枪管对应的有效 TaCZ 弹药 ID。 */
    public static String projectileId(ModuleConfig config,
                                      TetraItemStackProfileResolver.SelectedModules modules) {
        if (config == null || modules == null || modules.barrel() == null) return null;
        return config.barrels().stream()
                .filter(barrel -> barrel.id().equals(modules.barrel().variantId()))
                .map(ModuleConfig.Barrel::projectileType)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
