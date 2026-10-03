package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadStartPolicyTest {
    @Test
    void rejectsEveryInvalidEntryBeforeChangingReloadState() {
        assertFalse(ReloadStartPolicy.allowed(false, false, false, false, false, false, false));
        assertFalse(ReloadStartPolicy.allowed(true, true, false, false, false, false, false));
        assertFalse(ReloadStartPolicy.allowed(true, false, true, false, false, false, false));
        assertFalse(ReloadStartPolicy.allowed(true, false, false, true, false, false, false));
        assertFalse(ReloadStartPolicy.allowed(true, false, false, false, true, false, false));
        assertFalse(ReloadStartPolicy.allowed(true, false, false, false, false, true, false));
    }

    @Test
    void allowsTacticalReloadOfFullMagazineOnlyWhenAChamberedRoundNeedsPreserving() {
        assertTrue(ReloadStartPolicy.allowed(true, false, false, false, false, false, false));
        assertTrue(ReloadStartPolicy.allowed(true, false, false, false, false, true, true));
        assertFalse(ReloadStartPolicy.allowed(true, false, false, false, false, true, false));
    }
}
