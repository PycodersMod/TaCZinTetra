package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.network.ReloadIntentMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadIntentFactoryTest {
    @Test
    void convertsRouterDecisionIntoSingleOrFillIntent() {
        GunInputRouter.Decision single = GunInputRouter.resolve(true, false, false, false, false);
        ReloadIntentMessage singleIntent = ReloadIntentFactory.create(single, 11, 3);
        assertFalse(singleIntent.offHand());
        assertTrue(singleIntent.singleBatch());

        GunInputRouter.Decision dualOffHandFill = GunInputRouter.resolve(true, true, false, true, true);
        ReloadIntentMessage fillIntent = ReloadIntentFactory.create(dualOffHandFill, 22, 4);
        assertTrue(fillIntent.offHand());
        assertFalse(fillIntent.singleBatch());
        assertTrue(fillIntent.stackIdentity() == 22);
        assertTrue(fillIntent.sequence() == 4);
    }
}
