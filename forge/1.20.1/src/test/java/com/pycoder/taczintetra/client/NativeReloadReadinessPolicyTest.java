package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeReloadReadinessPolicyTest {
    @Test
    void magazineRoundWithReserveIsReady() {
        assertTrue(NativeReloadReadinessPolicy.isReady(1, 15, false));
    }

    @Test
    void chamberedRoundWithEmptyMagazineAndReserveIsReady() {
        assertTrue(NativeReloadReadinessPolicy.isReady(0, 15, true));
    }

    @Test
    void emptyMagazineAndEmptyChamberIsNotReady() {
        assertFalse(NativeReloadReadinessPolicy.isReady(0, 15, false));
    }

    @Test
    void noReserveIsNotReadyEvenWhenRoundIsChambered() {
        assertFalse(NativeReloadReadinessPolicy.isReady(0, 0, true));
    }
}
