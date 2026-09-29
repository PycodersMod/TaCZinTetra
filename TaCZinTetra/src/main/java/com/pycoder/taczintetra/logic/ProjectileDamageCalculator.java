package com.pycoder.taczintetra.logic;

/** Keeps pellet damage distribution independent from trigger-shot count. */
public final class ProjectileDamageCalculator {
    private ProjectileDamageCalculator() {
    }

    public static double damagePerPellet(double roundDamage, int pelletsPerRound) {
        if (!Double.isFinite(roundDamage) || pelletsPerRound <= 0) {
            return 0.01;
        }
        return Math.max(0.01, roundDamage / pelletsPerRound);
    }

    public static double totalDamage(double roundDamage, int pelletsPerRound, int independentShots) {
        if (independentShots <= 0) {
            return 0;
        }
        return damagePerPellet(roundDamage, pelletsPerRound) * pelletsPerRound * independentShots;
    }
}
