package com.pycoder.taczintetra.logic;

/**
 * Commits a reload only after the caller confirms animation completion.
 * Inventory, ItemStack and network mutation remain outside this pure policy.
 */
public final class ReloadSettlementPolicy {
    private ReloadSettlementPolicy() {
    }

    public static Result settle(boolean completed, int maxAmmo, int currentAmmo,
                                AmmoRecipeDefinition recipe, ResourceAccount resources,
                                boolean fillMode) {
        int safeMax = Math.max(0, maxAmmo);
        int safeCurrent = Math.max(0, Math.min(currentAmmo, safeMax));
        if (!completed || recipe == null || resources == null) {
            return new Result(safeCurrent, resources == null ? ResourceAccount.empty() : resources, 0);
        }

        AmmoBatchCalculator.Result plan = ReloadPlanCalculator.plan(
                maxAmmo, safeCurrent, recipe, resources, fillMode);
        ResourceAccount settledResources = resources.consume(recipe.cost(), plan.paidBatches());
        return new Result(safeCurrent + plan.loadedRounds(), settledResources, plan.paidBatches());
    }

    public record Result(int loadedRounds, ResourceAccount resources, int paidBatches) {
    }
}
