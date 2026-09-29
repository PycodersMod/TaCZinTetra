package com.pycoder.taczintetra.logic;

/** Reload movement rule: single batch uses normal speed, fill uses sneak speed. */
public final class ReloadMovementPolicy {
    private ReloadMovementPolicy() {
    }

    public static double speed(double normalSpeed, double sneakSpeed, boolean fillMode) {
        double raw = fillMode ? sneakSpeed : normalSpeed;
        double base = Double.isFinite(raw) ? Math.max(0, raw) : 0;
        return base * 0.8;
    }
}
