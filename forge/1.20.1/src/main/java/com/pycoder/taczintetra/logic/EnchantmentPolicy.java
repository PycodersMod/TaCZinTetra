package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.config.ModuleConfig;

/** 定义可通过 Tetra 应用于模组枪械的附魔。 */
public final class EnchantmentPolicy {
    private EnchantmentPolicy() {
    }

    public static boolean isAllowed(String enchantmentId, ModuleConfig config) {
        if (enchantmentId == null || config == null) return false;
        return config.enchantments().stream()
                .filter(entry -> enchantmentId.equals(entry.id()))
                .findFirst()
                .map(ModuleConfig.ConfigEntry::allowed)
                .orElse(false);
    }
}
