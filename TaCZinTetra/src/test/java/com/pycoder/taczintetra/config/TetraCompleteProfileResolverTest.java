package com.pycoder.taczintetra.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TetraCompleteProfileResolverTest {
    @Test
    void runtimeStateCarriesAllFastChangingFieldsAndTraits() {
        var state = new TetraItemStackProfileResolver.RuntimeState(
                42.5f, true, 8, 11, Set.of("minecraft:unbreaking"));

        assertEquals(42.5f, state.heat());
        assertTrue(state.overheatLocked());
        assertEquals(8, state.currentAmmo());
        assertEquals(11, state.resourceAmount());
        assertTrue(state.specialTraits().contains("minecraft:unbreaking"));
    }

    @Test
    void runtimeStateSanitizesInvalidValuesWithoutMinecraftBootstrap() {
        var state = new TetraItemStackProfileResolver.RuntimeState(
                Float.NaN, false, -4, -2, null).sanitized();

        assertEquals(0, state.heat());
        assertFalse(state.overheatLocked());
        assertEquals(0, state.currentAmmo());
        assertEquals(0, state.resourceAmount());
        assertTrue(state.specialTraits().isEmpty());
    }
}
