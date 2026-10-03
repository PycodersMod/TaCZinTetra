package com.pycoder.taczintetra.logic;

/** Durability is charged per independent fired round, never per pellet. */
public final class DurabilityCostPolicy {
    private DurabilityCostPolicy() {
    }

    public static int cost(int independentShots, int pelletsPerRound, double multiplier) {
        if (independentShots <= 0 || pelletsPerRound <= 0) {
            return 0;
        }
        double safeMultiplier = Double.isFinite(multiplier) && multiplier > 0 ? multiplier : 1;
        return Math.max(1, (int) Math.ceil(independentShots * safeMultiplier));
    }
}
