package com.pycoder.taczintetra.logic;

/** Combines TaCZ's posture spread with the module-specific accuracy adjustment. */
public final class SpreadPolicy {
    private SpreadPolicy() { }

    public static float total(float nativeSpread, float moduleAdjustment) {
        float safeNative = Float.isFinite(nativeSpread) ? Math.max(0, nativeSpread) : 0;
        float safeAdjustment = Float.isFinite(moduleAdjustment) ? Math.max(0, moduleAdjustment) : 0;
        return safeNative + safeAdjustment;
    }
}
