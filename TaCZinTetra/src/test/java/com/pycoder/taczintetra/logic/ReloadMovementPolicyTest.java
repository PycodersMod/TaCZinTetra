package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadMovementPolicyTest {
    @Test
    void usesDifferentMovementBaseForSingleAndFillReload() {
        assertEquals(0.8, ReloadMovementPolicy.speed(1.0, 0.3, false), 0.001);
        assertEquals(0.24, ReloadMovementPolicy.speed(1.0, 0.3, true), 0.001);
    }

    @Test
    void nonFiniteMovementInputsFallBackToZero() {
        assertEquals(0, ReloadMovementPolicy.speed(Double.NaN, 0.3, false));
        assertEquals(0, ReloadMovementPolicy.speed(1.0, Double.NaN, true));
    }
}
