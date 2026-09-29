package com.pycoder.taczintetra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TetraModuleSelectionTest {
    @Test
    void extractsVariantAndMaterialFromTetraModuleKey() {
        TetraModuleSelection selection = TetraModuleSelection.from("taczintetra:body", "pistol/", "taczintetra/iron/");
        assertEquals("pistol", selection.variantId());
        assertEquals("iron", selection.materialId());
    }

    @Test
    void rejectsForeignOrMalformedKeys() {
        assertNull(TetraModuleSelection.from("tetra:body/basic_axe/wood", "basic_axe/", "taczintetra/wood/"));
        assertNull(TetraModuleSelection.from("taczintetra:body", "", "taczintetra/wood/"));
        assertNull(TetraModuleSelection.from("taczintetra:body", "pistol/", ""));
    }

    @Test
    void acceptsTetraPathModuleKeys() {
        TetraModuleSelection selection = TetraModuleSelection.from(
                "taczintetra/body", "pistol/", "taczintetra/iron/");
        assertEquals("iron", selection.materialId());
    }
}
