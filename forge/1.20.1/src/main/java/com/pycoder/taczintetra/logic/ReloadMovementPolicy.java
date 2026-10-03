package com.pycoder.taczintetra.logic;

/** 装填移动规则：单批装填使用正常速度，装满弹匣时使用潜行速度。 */
public final class ReloadMovementPolicy {
    private ReloadMovementPolicy() {
    }

    public static double speed(double normalSpeed, double sneakSpeed, boolean fillMode) {
        double raw = fillMode ? sneakSpeed : normalSpeed;
        double base = Double.isFinite(raw) ? Math.max(0, raw) : 0;
        return base * 0.8;
    }
}
