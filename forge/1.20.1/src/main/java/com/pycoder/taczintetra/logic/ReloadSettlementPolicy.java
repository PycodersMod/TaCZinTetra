package com.pycoder.taczintetra.logic;

/**
 * 仅在调用方确认动画完成后提交换弹。
 * 物品栏、ItemStack 和网络状态修改均留在此纯策略之外。
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
