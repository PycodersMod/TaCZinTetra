package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalProbeRecoveryPolicyTest {
    @Test
    void recoversAnAuthoritativelyDeadIntegratedServerPlayerAtAnyProbePhase() {
        assertTrue(SurvivalProbeRecoveryPolicy.shouldRecover(false, false));
        assertFalse(SurvivalProbeRecoveryPolicy.shouldRecover(false, true));
    }

    @Test
    void doesNotRepeatRecoveryOrRecoverWhenBothPlayersAreAlive() {
        assertFalse(SurvivalProbeRecoveryPolicy.shouldRecover(true, false));
        assertFalse(SurvivalProbeRecoveryPolicy.shouldRecover(false, true));
    }

    @Test
    void resetsTheProbeAfterTheClientReceivesItsRespawnedPlayer() {
        assertTrue(SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(true, true, true));
        assertFalse(SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(true, false, true));
        assertFalse(SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(false, true, true));
        assertFalse(SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(true, true, false));
    }
}
