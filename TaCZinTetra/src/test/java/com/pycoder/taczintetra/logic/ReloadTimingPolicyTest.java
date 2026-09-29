package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadTimingPolicyTest {
    @Test
    void singleAndFillUseTheSameDuration() {
        assertEquals(ReloadTimingPolicy.durationTicks(20, 1.25, false),
                ReloadTimingPolicy.durationTicks(20, 1.25, true));
        assertEquals(25, ReloadTimingPolicy.durationTicks(20, 1.25, true));
    }

    @Test
    void durationMillisAppliesConfiguredMultiplierAndSafeFallback() {
        assertEquals(1200, ReloadTimingPolicy.durationMillis(1500, 0.8));
        assertEquals(1500, ReloadTimingPolicy.durationMillis(1500, 0));
        assertEquals(1500, ReloadTimingPolicy.durationMillis(1500, Double.NaN));
    }
}
