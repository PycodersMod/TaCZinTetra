package com.pycoder.taczintetra.logic;

/** 单批换弹与装满式换弹共用相同的动画时长。 */
public final class ReloadTimingPolicy {
    private ReloadTimingPolicy() {
    }

    public static long durationTicks(long baseTicks, double feedMultiplier, boolean fillMode) {
        long base = Math.max(0, baseTicks);
        double multiplier = Double.isFinite(feedMultiplier) && feedMultiplier > 0
                ? feedMultiplier : 1;
        return Math.max(0, Math.round(base * multiplier));
    }

    public static long durationMillis(long baseMillis, double multiplier) {
        return durationTicks(baseMillis, multiplier, false);
    }
}
