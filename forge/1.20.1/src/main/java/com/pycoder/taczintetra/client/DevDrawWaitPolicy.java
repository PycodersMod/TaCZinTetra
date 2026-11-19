package com.pycoder.taczintetra.client;

/** Bounds opt-in developer probes which wait for TaCZ's native draw cooldown. */
final class DevDrawWaitPolicy {
    static final int LOG_INTERVAL_TICKS = 20;
    static final int MAX_WAIT_TICKS = 80;

    private DevDrawWaitPolicy() { }

    static boolean shouldLog(int waitTicks) {
        return waitTicks > 0 && (waitTicks == 1 || waitTicks % LOG_INTERVAL_TICKS == 0);
    }

    static boolean timedOut(int waitTicks) {
        return waitTicks >= MAX_WAIT_TICKS;
    }
}
