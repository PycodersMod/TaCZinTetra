package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmmoConsumptionPolicyTest {
    @Test
    void consumesIndependentShotsNotPellets() {
        AmmoConsumptionPolicy.Result result = AmmoConsumptionPolicy.consume(8, 2);
        assertEquals(6, result.remainingLoadedRounds());
        assertEquals(2, result.consumedRounds());
    }

    @Test
    void neverConsumesMoreThanLoaded() {
        AmmoConsumptionPolicy.Result result = AmmoConsumptionPolicy.consume(1, 4);
        assertEquals(0, result.remainingLoadedRounds());
        assertEquals(1, result.consumedRounds());
    }
}
