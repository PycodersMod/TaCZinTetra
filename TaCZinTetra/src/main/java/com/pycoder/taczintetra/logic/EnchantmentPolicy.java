package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.config.ModuleConfig;

/** Defines the enchantments allowed on a modular gun through Tetra. */
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
