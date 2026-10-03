package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShootStatePolicyTest {
    @Test
    void onlyStableShooterStateMayEnterModularShot() {
        assertTrue(ShootStatePolicy.allowed(false, false, 0.0f));
        assertFalse(ShootStatePolicy.allowed(true, false, 0.0f));
        assertFalse(ShootStatePolicy.allowed(false, true, 0.0f));
        assertFalse(ShootStatePolicy.allowed(false, false, 0.01f));
    }

    @Test
    void malformedSprintStateIsRejected() {
        assertFalse(ShootStatePolicy.allowed(false, false, Float.NaN));
        assertFalse(ShootStatePolicy.allowed(false, false, Float.POSITIVE_INFINITY));
    }
}
