package com.pycoder.taczintetra.logic;

/** Single-batch and fill reloads share the same animation duration. */
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
