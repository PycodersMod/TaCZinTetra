package com.pycoder.taczintetra.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadIntentPolicyTest {
    @Test
    void acceptsOnlyNewIntentForAuthoritativeHeldGun() {
        ReloadIntentMessage intent = new ReloadIntentMessage(false, true, 7, 3);
        assertTrue(ReloadIntentPolicy.canAccept(intent, 7, 2, true, false, false, false));
    }

    @Test
    void rejectsMismatchReplayAndUnsafeStates() {
        ReloadIntentMessage intent = new ReloadIntentMessage(false, false, 7, 3);
        assertFalse(ReloadIntentPolicy.canAccept(intent, 8, 2, true, false, false, false));
        assertFalse(ReloadIntentPolicy.canAccept(intent, 7, 3, true, false, false, false));
        assertFalse(ReloadIntentPolicy.canAccept(intent, 7, 2, true, true, false, false));
        assertFalse(ReloadIntentPolicy.canAccept(intent, 7, 2, true, false, true, false));
        assertFalse(ReloadIntentPolicy.canAccept(intent, 7, 2, true, false, false, true));
    }
}
