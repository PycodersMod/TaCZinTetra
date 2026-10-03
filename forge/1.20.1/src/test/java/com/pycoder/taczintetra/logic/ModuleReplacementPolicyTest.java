package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModuleReplacementPolicyTest {
    @Test
    void bodyAndBarrelClearLoadedButPreserveReserve() {
        ModuleReplacementPolicy.Clearance body = ModuleReplacementPolicy.forSlot("body");
        assertTrue(body.clearLoaded());
        assertTrue(body.clearHoning());
        assertTrue(body.clearStock());
        assertTrue(body.clearMinorPart());
        assertFalse(body.clearGrip());
        assertFalse(body.clearOptic());
        assertFalse(body.clearReserve());
        assertFalse(body.returnReplacedPart());

        ModuleReplacementPolicy.Clearance magazine = ModuleReplacementPolicy.forSlot("magazine");
        assertTrue(magazine.clearLoaded());
        assertTrue(magazine.clearReserve());
        assertTrue(magazine.clearGrip());
        assertTrue(magazine.clearMinorPart());
        assertFalse(magazine.clearStock());
        assertFalse(magazine.returnReplacedPart());
    }

    @Test
    void ordinaryHoningDoesNotClearAmmo() {
        ModuleReplacementPolicy.Clearance honing = ModuleReplacementPolicy.forSlot("honing");
        assertFalse(honing.clearLoaded());
        assertFalse(honing.clearReserve());
    }
}
