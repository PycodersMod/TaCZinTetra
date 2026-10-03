package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TetraRepairRuntimeHandlerTest {
    @Test
    void onlyFiltersRepairAgentDefinitionsWhenTheRestrictionIsEnabled() {
        assertFalse(TetraRepairRuntimeHandler.shouldFilterRepairDefinitions(false, "taczintetra:iron_repair"));
        assertFalse(TetraRepairRuntimeHandler.shouldFilterRepairDefinitions(true, ""));
        assertTrue(TetraRepairRuntimeHandler.shouldFilterRepairDefinitions(true, "taczintetra:iron_repair"));
    }
}
