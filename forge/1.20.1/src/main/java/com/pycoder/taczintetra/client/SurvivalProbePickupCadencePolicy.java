package com.pycoder.taczintetra.client;

/** Rate-limits survival test pickup assistance against an increasing per-phase tick counter. */
final class SurvivalProbePickupCadencePolicy {
    private SurvivalProbePickupCadencePolicy() { }

    static boolean shouldAssist(int currentTick, int lastAssistTick, int minimumInterval) {
        if (currentTick < 0 || minimumInterval <= 0) {
            throw new IllegalArgumentException("Ticks must be non-negative and interval must be positive");
        }
        return currentTick - lastAssistTick >= minimumInterval;
    }
}
