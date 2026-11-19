package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevF06ReloadInterruptPolicyTest {
    @Test
    void logicalGunIdentitySurvivesItemStackObjectReplacement() {
        assertTrue(DevF06ReloadInterruptPolicy.isSameLogicalGunStack(608976908, 608976908, true));
    }

    @Test
    void logicalGunIdentityRejectsDifferentIdentityOrNonGunSlot() {
        assertFalse(DevF06ReloadInterruptPolicy.isSameLogicalGunStack(608976908, 608976909, true));
        assertFalse(DevF06ReloadInterruptPolicy.isSameLogicalGunStack(608976908, 608976908, false));
        assertFalse(DevF06ReloadInterruptPolicy.isSameLogicalGunStack(0, 0, true));
    }

    @Test
    void interruptionPassesOnlyWhenTheSameStackKeepsAmmoAndResourcesAndClearsPendingState() {
        assertTrue(DevF06ReloadInterruptPolicy.passes(1, 1, 15, 15,
                true, false, false, true));
        assertFalse(DevF06ReloadInterruptPolicy.passes(1, 0, 15, 15,
                true, false, false, true));
        assertFalse(DevF06ReloadInterruptPolicy.passes(1, 1, 15, 14,
                true, false, false, true));
        assertFalse(DevF06ReloadInterruptPolicy.passes(1, 1, 15, 15,
                false, false, false, true));
        assertFalse(DevF06ReloadInterruptPolicy.passes(1, 1, 15, 15,
                true, true, false, true));
        assertFalse(DevF06ReloadInterruptPolicy.passes(1, 1, 15, 15,
                true, false, true, true));
        assertFalse(DevF06ReloadInterruptPolicy.passes(1, 1, 15, 15,
                true, false, false, false));
    }
}
