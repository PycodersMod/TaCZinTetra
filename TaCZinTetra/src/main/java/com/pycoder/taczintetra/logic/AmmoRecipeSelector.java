package com.pycoder.taczintetra.logic;

import java.util.Map;

/** Selects a cost recipe by the barrel projectile and feed type. */
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
