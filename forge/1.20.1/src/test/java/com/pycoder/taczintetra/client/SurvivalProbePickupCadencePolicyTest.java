package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalProbePickupCadencePolicyTest {
    @Test
    void allowsInitialAssistAndThenWaitsForTheConfiguredIncreasingTickInterval() {
        assertTrue(SurvivalProbePickupCadencePolicy.shouldAssist(0, -600, 25));
        assertFalse(SurvivalProbePickupCadencePolicy.shouldAssist(24, 0, 25));
        assertTrue(SurvivalProbePickupCadencePolicy.shouldAssist(25, 0, 25));
        assertTrue(SurvivalProbePickupCadencePolicy.shouldAssist(50, 25, 25));
    }

    @Test
    void rejectsInvalidTickAndIntervalInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> SurvivalProbePickupCadencePolicy.shouldAssist(-1, 0, 25));
        assertThrows(IllegalArgumentException.class,
                () -> SurvivalProbePickupCadencePolicy.shouldAssist(10, 0, 0));
    }
}
