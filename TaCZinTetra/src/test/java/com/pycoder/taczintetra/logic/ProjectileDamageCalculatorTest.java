package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectileDamageCalculatorTest {
    @Test
    void dividesEachRoundDamageAcrossPelletsNotIndependentShots() {
        assertEquals(4.0, ProjectileDamageCalculator.damagePerPellet(12, 3), 0.001);
        assertEquals(24.0, ProjectileDamageCalculator.totalDamage(12, 3, 2), 0.001);
    }

    @Test
    void invalidPelletCountIsSafe() {
        assertEquals(0.01, ProjectileDamageCalculator.damagePerPellet(12, 0), 0.001);
    }
}
