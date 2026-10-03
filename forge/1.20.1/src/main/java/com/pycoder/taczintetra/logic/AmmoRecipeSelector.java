package com.pycoder.taczintetra.logic;

import java.util.Map;

/** 根据枪管弹丸类型与供弹方式选择消耗配方。 */
public final class AmmoRecipeSelector {
    private AmmoRecipeSelector() {
    }

    public static AmmoRecipeDefinition select(Map<?, AmmoRecipeDefinition> recipes,
                                               String projectile, String feedType) {
        if (recipes == null || projectile == null || feedType == null) {
            return null;
        }
        return recipes.values().stream()
                .filter(recipe -> projectile.equals(recipe.projectile())
                        && feedType.equals(recipe.feedType()))
                .findFirst().orElse(null);
    }
}
