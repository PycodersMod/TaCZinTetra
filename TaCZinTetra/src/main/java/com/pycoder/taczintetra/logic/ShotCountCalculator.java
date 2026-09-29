package com.pycoder.taczintetra.logic;

/** Computes N independent shots times M pellets with an explicit safety cap. */
public final class ShotCountCalculator {
    private ShotCountCalculator() {
    }

    public static int totalProjectiles(int independentShots, int pelletsPerRound, int safetyLimit) {
        if (independentShots <= 0 || pelletsPerRound <= 0 || safetyLimit <= 0) {
            return 0;
        }
        long total = (long) independentShots * pelletsPerRound;
        return (int) Math.min(total, safetyLimit);
    }
}
