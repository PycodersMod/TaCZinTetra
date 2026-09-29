package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShotCountCalculatorTest {
    @Test
    void separatesIndependentShotsFromPellets() {
        assertEquals(6, ShotCountCalculator.totalProjectiles(2, 3, 64));
    }

    @Test
    void clampsInvalidValuesAndSafetyLimit() {
        assertEquals(64, ShotCountCalculator.totalProjectiles(20, 20, 64));
        assertEquals(0, ShotCountCalculator.totalProjectiles(-1, 3, 64));
        assertEquals(0, ShotCountCalculator.totalProjectiles(2, 3, 0));
    }
}
