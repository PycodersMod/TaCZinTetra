package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimInputPolicyTest {
    @Test
    void acceptsFiniteAngles() {
        assertTrue(AimInputPolicy.valid(0.0f, 180.0f));
        assertTrue(AimInputPolicy.valid(-90.0f, -180.0f));
    }

    @Test
    void rejectsNonFiniteAngles() {
        assertFalse(AimInputPolicy.valid(Float.NaN, 0.0f));
        assertFalse(AimInputPolicy.valid(0.0f, Float.POSITIVE_INFINITY));
    }
}
