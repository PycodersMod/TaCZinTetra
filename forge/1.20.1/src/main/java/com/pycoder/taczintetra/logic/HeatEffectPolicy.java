package com.pycoder.taczintetra.logic;

/** 应用规范化热量效果，不修改静态枪械档案。 */
public final class HeatEffectPolicy {
    private HeatEffectPolicy() {
    }

    public static double rpmMultiplier(double heat, double overheatThreshold,
                                       double coldMultiplier, double hotMultiplier) {
        return interpolate(heat, overheatThreshold, coldMultiplier, hotMultiplier);
    }

    public static double inaccuracyMultiplier(double heat, double overheatThreshold,
                                               double coldMultiplier, double hotMultiplier) {
        return interpolate(heat, overheatThreshold, coldMultiplier, hotMultiplier);
    }

    public static double curveMultiplier(double heat, double overheatThreshold, HeatCurve curve,
                                         double coldMultiplier, double hotMultiplier) {
        if (curve == null) return interpolate(heat, overheatThreshold, coldMultiplier, hotMultiplier);
        if (!Double.isFinite(heat) || !Double.isFinite(overheatThreshold) || overheatThreshold <= 0) {
            return curve.valueAt(0);
        }
        return curve.valueAt(heat / overheatThreshold);
    }

    public static double coolingCoefficient(double barrelMultiplier,
                                            double materialConductivity,
                                            double coolingHoning) {
        double barrel = finiteNonNegative(barrelMultiplier, 0);
        double material = finiteNonNegative(materialConductivity, 1);
        double honing = finiteNonNegative(coolingHoning, 0);
        return barrel * material * (1 + honing);
    }

    private static double interpolate(double heat, double threshold, double cold, double hot) {
        if (!Double.isFinite(heat) || !Double.isFinite(threshold) || threshold <= 0) {
            return safeMultiplier(cold, 1);
        }
        double t = Math.max(0, Math.min(1, heat / threshold));
        double start = safeMultiplier(cold, 1);
        double end = Double.isFinite(hot) ? hot : start;
        return Math.max(0.01, start + (end - start) * t);
    }

    private static double safeMultiplier(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.01, value) : fallback;
    }

    private static double finiteNonNegative(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0, value) : fallback;
    }
}
