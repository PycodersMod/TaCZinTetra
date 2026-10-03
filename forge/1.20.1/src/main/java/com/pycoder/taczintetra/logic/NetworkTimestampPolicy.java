package com.pycoder.taczintetra.logic;

/** 复现 TaCZ 服务端对客户端射击时间戳的容差规则。 */
public final class NetworkTimestampPolicy {
    private NetworkTimestampPolicy() { }

    public static boolean accepts(long nowMillis, long baseTimestamp,
                                  long clientTimestamp, double tickDurationMillis) {
        if (!Double.isFinite(tickDurationMillis)) return false;
        double safeTickDuration = Math.max(50.0D, tickDurationMillis);
        long drift = nowMillis - baseTimestamp - clientTimestamp;
        return drift >= -300L && drift <= 300.0D + safeTickDuration * 2.0D;
    }
}
