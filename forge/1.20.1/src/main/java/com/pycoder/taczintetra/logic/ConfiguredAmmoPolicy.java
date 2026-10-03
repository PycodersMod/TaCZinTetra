package com.pycoder.taczintetra.logic;

import net.minecraft.resources.ResourceLocation;

/** Resolves an explicit configured ammo id without hiding malformed configuration. */
public final class ConfiguredAmmoPolicy {
    private ConfiguredAmmoPolicy() { }

    public static ResourceLocation resolve(String configuredId, ResourceLocation nativeFallback) {
        if (configuredId == null || configuredId.isBlank()) return nativeFallback;
        return ResourceLocation.tryParse(configuredId.trim());
    }
}
