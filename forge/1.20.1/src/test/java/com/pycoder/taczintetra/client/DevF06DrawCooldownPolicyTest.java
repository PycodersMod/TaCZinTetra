package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevF06DrawCooldownPolicyTest {
    @Test
    void waitsForServerToAcknowledgeTheDrawBeforeObservingItsCooldown() {
        assertEquals(new DevF06DrawCooldownPolicy.Result(false,
                        DevF06DrawCooldownPolicy.Outcome.WAITING_FOR_SERVER_DRAW),
                DevF06DrawCooldownPolicy.evaluate(false, 0, 1, 200));
    }

    @Test
    void waitsForAnObservedServerCooldownToReachZeroBeforeFiring() {
        assertEquals(new DevF06DrawCooldownPolicy.Result(true,
                        DevF06DrawCooldownPolicy.Outcome.WAITING_FOR_COOLDOWN),
                DevF06DrawCooldownPolicy.evaluate(false, 800, 1, 200));
        assertEquals(new DevF06DrawCooldownPolicy.Result(true,
                        DevF06DrawCooldownPolicy.Outcome.WAITING_FOR_COOLDOWN),
                DevF06DrawCooldownPolicy.evaluate(true, 20, 40, 200));
        assertEquals(new DevF06DrawCooldownPolicy.Result(true,
                        DevF06DrawCooldownPolicy.Outcome.READY),
                DevF06DrawCooldownPolicy.evaluate(true, 0, 41, 200));
    }

    @Test
    void timesOutIfTheServerNeverAcknowledgesOrTheCooldownNeverClears() {
        assertEquals(DevF06DrawCooldownPolicy.Outcome.TIMEOUT,
                DevF06DrawCooldownPolicy.evaluate(false, 0, 200, 200).outcome());
        assertEquals(DevF06DrawCooldownPolicy.Outcome.TIMEOUT,
                DevF06DrawCooldownPolicy.evaluate(true, 1, 200, 200).outcome());
    }

    @Test
    void rejectsInvalidObservations() {
        assertThrows(IllegalArgumentException.class,
                () -> DevF06DrawCooldownPolicy.evaluate(false, -1, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06DrawCooldownPolicy.evaluate(false, 0, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> DevF06DrawCooldownPolicy.evaluate(false, 0, 0, 0));
    }
}
