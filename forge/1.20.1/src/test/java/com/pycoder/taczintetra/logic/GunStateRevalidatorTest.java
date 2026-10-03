package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunStateRevalidatorTest {
    @Test
    void clampsAmmoAndStoredResourcesAndSelectsLegalMode() {
        GunStateRevalidator.State state = new GunStateRevalidator.State(8, 12, 16, 20,
                List.of("semi", "burst"), "auto");
        GunStateRevalidator.State result = GunStateRevalidator.revalidate(state);
        assertEquals(8, result.currentAmmo());
        assertEquals(16, result.stored());
        assertEquals("semi", result.fireMode());
    }

    @Test
    void missingOrInvalidStateUsesSafeDefaults() {
        GunStateRevalidator.State result = GunStateRevalidator.revalidate(null);
        assertEquals(0, result.currentAmmo());
        assertEquals(0, result.stored());
        assertEquals("semi", result.fireMode());
    }

    @Test
    void nullAndBlankModesAreRemovedAndAlwaysLeaveAValidMode() {
        GunStateRevalidator.State state = new GunStateRevalidator.State(4, 1, 4, 2,
                java.util.Arrays.asList(null, " ", "burst"), null);
        GunStateRevalidator.State result = GunStateRevalidator.revalidate(state);
        assertEquals(List.of("burst"), result.fireModes());
        assertEquals("burst", result.fireMode());
    }
}
