package com.pycoder.taczintetra.logic;

/** 计算 N 次独立射击对应的 M 个弹丸，并应用明确的安全上限。 */
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
