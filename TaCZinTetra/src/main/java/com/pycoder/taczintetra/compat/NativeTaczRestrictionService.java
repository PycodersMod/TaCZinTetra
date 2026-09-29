package com.pycoder.taczintetra.compat;

import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Central policy for native TaCZ content; kept independent from JEI. */
public final class NativeTaczRestrictionService {
    private static final Set<String> NATIVE_ITEM_IDS = Set.of(
            "modern_kinetic_gun",
            "workbench_a", "workbench_b", "workbench_c");
    private static final Set<String> NATIVE_WORKBENCH_IDS = Set.of(
            "gun_smith_table", "workbench_a", "workbench_b", "workbench_c");

    private NativeTaczRestrictionService() { }

    public static boolean isNativeTaczItem(ResourceLocation id) {
        return id != null && "tacz".equals(id.getNamespace()) && NATIVE_ITEM_IDS.contains(id.getPath());
    }

    public static boolean isNativeTaczWorkbench(ResourceLocation id) {
        return id != null && "tacz".equals(id.getNamespace()) && NATIVE_WORKBENCH_IDS.contains(id.getPath());
    }

    public static boolean shouldBlockNativeItem(ResourceLocation id) {
        return TaczInTetraForgeConfig.DISABLE_NATIVE_ITEMS.get() && isNativeTaczItem(id);
    }

    public static boolean shouldBlockNativeWorkbench(ResourceLocation id) {
        return TaczInTetraForgeConfig.DISABLE_NATIVE_WORKBENCHES.get() && isNativeTaczWorkbench(id);
    }

    /**
     * Recipe policy is intentionally broader than item policy: a native TaCZ recipe can
     * have a non-TaCZ recipe id, while TaCZ's own recipe namespace remains fully disabled.
     */
    public static boolean shouldBlockNativeRecipe(ResourceLocation recipeId, ResourceLocation resultId) {
        if (!TaczInTetraForgeConfig.DISABLE_NATIVE_RECIPES.get()) return false;
        return (recipeId != null && "tacz".equals(recipeId.getNamespace()))
                || isNativeTaczItem(resultId)
                || isNativeTaczWorkbench(resultId);
    }

    public static boolean shouldHideFromJei(ResourceLocation id) {
        return TaczInTetraForgeConfig.JEI_MODE.get() == TaczInTetraForgeConfig.JeiMode.HIDE
                && (shouldBlockNativeItem(id) || shouldBlockNativeWorkbench(id));
    }

    public static boolean shouldHideFromJei(ResourceLocation recipeId, ResourceLocation resultId) {
        return TaczInTetraForgeConfig.JEI_MODE.get() == TaczInTetraForgeConfig.JeiMode.HIDE
                && shouldBlockNativeRecipe(recipeId, resultId);
    }
}
