package com.pycoder.taczintetra.config;

/** 对 Tetra 模块注册表键提供带命名空间感知的安全视图。 */
public record TetraModuleSelection(String variantId, String materialId) {
    public static TetraModuleSelection from(String moduleKey, String variantKey, String materialKey) {
        if (moduleKey == null || variantKey == null || materialKey == null || variantKey.isBlank()) return null;
        if (!moduleKey.startsWith("taczintetra:") && !moduleKey.startsWith("taczintetra/")) return null;
        String variant = trimSlash(variantKey);
        String material = lastPathPart(materialKey);
        if (variant.isBlank()) return null;
        return material.isBlank() ? null : new TetraModuleSelection(variant, material);
    }

    private static String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("^/+|/+$", "");
    }

    private static String lastPathPart(String value) {
        String normalized = trimSlash(value);
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? normalized : normalized.substring(slash + 1);
    }
}
