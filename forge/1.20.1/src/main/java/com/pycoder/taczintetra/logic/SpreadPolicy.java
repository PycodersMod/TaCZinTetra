package com.pycoder.taczintetra.logic;

/** 将 TaCZ 姿势散布与模块专属精度修正合并计算。 */
public final class SpreadPolicy {
    private SpreadPolicy() { }

    public static float total(float nativeSpread, float moduleAdjustment) {
        float safeNative = Float.isFinite(nativeSpread) ? Math.max(0, nativeSpread) : 0;
        float safeAdjustment = Float.isFinite(moduleAdjustment) ? Math.max(0, moduleAdjustment) : 0;
        return safeNative + safeAdjustment;
    }
}
