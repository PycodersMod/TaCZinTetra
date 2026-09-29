package com.pycoder.taczintetra.logic;

/** Combines resource affordability with single-batch/fill reload planning. */
public final class ReloadPlanCalculator {
    private ReloadPlanCalculator() { }

    public static AmmoBatchCalculator.Result plan(int maxAmmo, int currentAmmo,
                                                   AmmoRecipeDefinition recipe,
                                                   ResourceAccount resources, boolean fillMode) {
        if (recipe == null || resources == null) return new AmmoBatchCalculator.Result(0, 0);
        return AmmoBatchCalculator.calculate(maxAmmo, currentAmmo, recipe.batchSize(),
                resources.affordableBatches(recipe.cost()), fillMode);
    }
}
