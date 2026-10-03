package com.pycoder.taczintetra.logic;

/** 供模组枪械桥接使用的纯服务端射速门禁。 */
public final class FireCooldownPolicy {
    private static final long NANOS_PER_MINUTE = 60_000_000_000L;

    private FireCooldownPolicy() {
    }

    public static long intervalNanos(int roundsPerMinute) {
        long rpm = Math.max(1, roundsPerMinute);
        return Math.max(1L, NANOS_PER_MINUTE / rpm);
    }

    public static boolean ready(long nowNanos, int roundsPerMinute, long lastShotNanos) {
        if (lastShotNanos < 0) return true;
        if (nowNanos < lastShotNanos) return false;
        return nowNanos - lastShotNanos >= intervalNanos(roundsPerMinute);
    }
}
