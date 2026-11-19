package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevF06AmmoSyncObservationPolicyTest {
    @Test
    void waitsWhileTheClientStillHasThePreShotCount() {
        assertEquals(DevF06AmmoSyncObservationPolicy.Outcome.WAITING,
                DevF06AmmoSyncObservationPolicy.evaluate(1, 1, 0, 40));
        assertEquals(DevF06AmmoSyncObservationPolicy.Outcome.WAITING,
                DevF06AmmoSyncObservationPolicy.evaluate(1, 1, 39, 40));
    }

    @Test
    void confirmsWhenTheExpectedSingleRoundDecreaseArrives() {
        assertEquals(DevF06AmmoSyncObservationPolicy.Outcome.CONFIRMED,
                DevF06AmmoSyncObservationPolicy.evaluate(1, 0, 1, 40));
    }

    @Test
    void failsWhenTheExpectedUpdateDoesNotArriveBeforeTheBound() {
        assertEquals(DevF06AmmoSyncObservationPolicy.Outcome.FAILED,
                DevF06AmmoSyncObservationPolicy.evaluate(1, 1, 40, 40));
    }

    @Test
    void failsUnexpectedAmmoValuesAndRejectsInvalidInputs() {
        assertEquals(DevF06AmmoSyncObservationPolicy.Outcome.FAILED,
                DevF06AmmoSyncObservationPolicy.evaluate(2, 0, 1, 40));
        assertEquals(DevF06AmmoSyncObservationPolicy.Outcome.FAILED,
                DevF06AmmoSyncObservationPolicy.evaluate(1, -1, 1, 40));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06AmmoSyncObservationPolicy.evaluate(0, 0, 0, 40));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06AmmoSyncObservationPolicy.evaluate(1, 1, -1, 40));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06AmmoSyncObservationPolicy.evaluate(1, 1, 0, 0));
    }
}
