package com.pycoder.taczintetra.logic;

/** 独立于物品栏与网络适配器的容量规则。 */
public final class ResourceCapacityCalculator {
    private ResourceCapacityCalculator() {
    }

    public static int reserveCapacity(int magazineBase, int honing, int fixedAdditions) {
        long total = (long) Math.max(0, magazineBase)
                + Math.max(0, honing)
                + Math.max(0, fixedAdditions);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int resourceMax(int magazineBase, int honing, int fixedAdditions,
                                  double materialMultiplier) {
        double multiplier = Double.isFinite(materialMultiplier) && materialMultiplier > 0
                ? materialMultiplier : 1;
        return Math.max(1, (int) Math.round(reserveCapacity(magazineBase, honing, fixedAdditions)
                * multiplier));
    }

    public static int loadedAmmoCapacity(int reserveBaseCapacity, double bodyMultiplier) {
        double multiplier = Double.isFinite(bodyMultiplier) && bodyMultiplier > 0
                ? bodyMultiplier : 1;
        return Math.max(0, (int) Math.round(Math.max(0, reserveBaseCapacity) * multiplier));
    }
}
