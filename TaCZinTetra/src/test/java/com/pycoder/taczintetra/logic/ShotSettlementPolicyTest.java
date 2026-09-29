package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShotSettlementPolicyTest {
    @Test
    void consumesOnlyRoundsRepresentedBySuccessfullyCreatedProjectiles() {
        assertEquals(1, ShotSettlementPolicy.consumedRounds(3, 24, 8));
        assertEquals(2, ShotSettlementPolicy.consumedRounds(3, 24, 9));
        assertEquals(3, ShotSettlementPolicy.consumedRounds(3, 24, 24));
    }

    @Test
    void clampsInvalidOrOversizedSpawnCountsSafely() {
        assertEquals(0, ShotSettlementPolicy.consumedRounds(3, 24, 0));
        assertEquals(0, ShotSettlementPolicy.consumedRounds(3, 24, -1));
        assertEquals(3, ShotSettlementPolicy.consumedRounds(3, 24, 99));
        assertEquals(0, ShotSettlementPolicy.consumedRounds(0, 24, 8));
    }
}
