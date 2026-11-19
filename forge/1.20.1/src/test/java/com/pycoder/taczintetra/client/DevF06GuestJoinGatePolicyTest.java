package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevF06GuestJoinGatePolicyTest {
    @Test
    void waitsUntilHostAndGuestAreBothOnline() {
        assertEquals(DevF06GuestJoinGatePolicy.Outcome.WAITING_FOR_GUEST,
                DevF06GuestJoinGatePolicy.evaluate(1, 1, 2400));
        assertEquals(DevF06GuestJoinGatePolicy.Outcome.READY,
                DevF06GuestJoinGatePolicy.evaluate(2, 2, 2400));
        assertEquals(DevF06GuestJoinGatePolicy.Outcome.READY,
                DevF06GuestJoinGatePolicy.evaluate(3, 2400, 2400));
    }

    @Test
    void timesOutWhenNoGuestJoinsWithinTheBound() {
        assertEquals(DevF06GuestJoinGatePolicy.Outcome.TIMEOUT,
                DevF06GuestJoinGatePolicy.evaluate(1, 2400, 2400));
    }

    @Test
    void rejectsInvalidObservations() {
        assertThrows(IllegalArgumentException.class,
                () -> DevF06GuestJoinGatePolicy.evaluate(-1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06GuestJoinGatePolicy.evaluate(1, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06GuestJoinGatePolicy.evaluate(1, 0, 0));
    }
}
