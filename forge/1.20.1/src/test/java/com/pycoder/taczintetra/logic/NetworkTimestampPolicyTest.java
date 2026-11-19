package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkTimestampPolicyTest {
    @Test
    void acceptsNativeTimestampTolerance() {
        assertTrue(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 8_700L, 50.0D));
        assertTrue(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 9_250L, 50.0D));
        assertFalse(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 8_599L, 50.0D));
        assertFalse(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 9_301L, 50.0D));
    }

    @Test
    void usesTheLargerOfNativeMinimumTickAndObservedTickDuration() {
        assertTrue(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 8_500L, 100.0D));
        assertFalse(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 8_499L, 100.0D));
        assertFalse(NetworkTimestampPolicy.accepts(10_000L, 1_000L, 9_000L, Double.NaN));
    }

    @Test
    void clientTimestampMatchesTaczRelativeBaseContract() {
        long clientBaseTimestamp = 1_000_000L;
        long clientNow = 1_000_250L;
        long sentTimestamp = NetworkTimestampPolicy.clientTimestamp(clientNow, clientBaseTimestamp);

        assertTrue(sentTimestamp >= 0L);
        assertTrue(NetworkTimestampPolicy.accepts(1_000_260L, clientBaseTimestamp,
                sentTimestamp, 50.0D));
        assertFalse(NetworkTimestampPolicy.accepts(1_000_260L, clientBaseTimestamp,
                clientNow, 50.0D));
    }
}
