package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireCooldownPolicyTest {
    @Test
    void rejectsASecondShotBeforeRpmInterval() {
        long interval = FireCooldownPolicy.intervalNanos(600);
        assertTrue(FireCooldownPolicy.ready(1_000_000_000L, 600, -1L));
        assertFalse(FireCooldownPolicy.ready(1_000_000_000L + interval - 1, 600,
                1_000_000_000L));
        assertTrue(FireCooldownPolicy.ready(1_000_000_000L + interval, 600,
                1_000_000_000L));
    }

    @Test
    void invalidRpmUsesSafeMinimumInterval() {
        assertTrue(FireCooldownPolicy.intervalNanos(0) > 0);
        assertTrue(FireCooldownPolicy.intervalNanos(-1) > 0);
    }
}
