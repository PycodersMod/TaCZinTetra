package com.pycoder.taczintetra.logic;

import net.minecraft.resources.ResourceLocation;

/** 解析显式配置的弹药 ID，并保留格式错误配置的可见性。 */
public final class ConfiguredAmmoPolicy {
    private ConfiguredAmmoPolicy() { }

    public static ResourceLocation resolve(String configuredId, ResourceLocation nativeFallback) {
        if (configuredId == null || configuredId.isBlank()) return nativeFallback;
        return ResourceLocation.tryParse(configuredId.trim());
    }
}
