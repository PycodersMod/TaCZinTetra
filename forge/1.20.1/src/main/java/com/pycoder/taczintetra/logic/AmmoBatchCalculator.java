package com.pycoder.taczintetra.logic;

/** 仅计算批次消耗；背包与 NBT 的修改在此逻辑之外执行。 */
public final class AmmoBatchCalculator {
    private AmmoBatchCalculator() {
    }

    public static Result calculate(int maxAmmo, int currentAmmo, int batchSize,
                                   int affordableBatches, boolean fillMode) {
        long needed = Math.max(0L, (long) maxAmmo - Math.max(0, currentAmmo));
        if (needed == 0 || batchSize <= 0 || affordableBatches <= 0) {
            return new Result(0, 0);
        }
        long requiredBatches = (needed + (long) batchSize - 1L) / batchSize;
        int paid = fillMode ? (int) Math.min(requiredBatches, (long) affordableBatches) : 1;
        long loaded = Math.min(needed, (long) paid * batchSize);
        return new Result(paid, (int) loaded);
    }

    public record Result(int paidBatches, int loadedRounds) {
    }
}
