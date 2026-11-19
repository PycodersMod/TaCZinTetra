package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Shared, behavior-neutral logging for the opt-in QuickPlay diagnostic hooks. */
public final class DevQuickPlayTrace {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROPERTY = "taczintetra.dev_quickplay_trace";
    private static final long START_NANOS = System.nanoTime();

    private DevQuickPlayTrace() {
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void record(String stage, String details) {
        if (!isEnabled()) return;
        long elapsedMillis = (System.nanoTime() - START_NANOS) / 1_000_000L;
        LOGGER.info("TaCZinTetra QuickPlay trace: elapsedMs={}, stage={}, {}",
                elapsedMillis, stage, details);
    }
}
