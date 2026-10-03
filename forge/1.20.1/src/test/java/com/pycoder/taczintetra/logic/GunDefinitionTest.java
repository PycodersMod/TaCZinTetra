package com.pycoder.taczintetra.logic;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunDefinitionTest {
    @Test
    void parsesConfiguredFields() {
        GunDefinition definition = GunDefinition.from(JsonParser.parseString(
                "{\"weapon_class\":\"pistol\",\"fire_modes\":[\"semi\",\"burst\"],"
                        + "\"shots_per_trigger\":2,\"loaded_capacity_multiplier\":1.5,"
                        + "\"rounds_per_minute\":300,\"dual_wield\":true,\"heat\":\"hot\"}").getAsJsonObject());
        assertEquals("pistol", definition.weaponClass());
        assertEquals(2, definition.shotsPerTrigger());
        assertEquals(1.5, definition.loadedCapacityMultiplier());
        assertEquals(300, definition.roundsPerMinute());
        assertTrue(definition.dualWield());
        assertEquals("hot", definition.heatId());
    }

    @Test
    void missingAndInvalidValuesUseSafeDefaults() {
        GunDefinition definition = GunDefinition.from(JsonParser.parseString(
                "{\"shots_per_trigger\":-2,\"loaded_capacity_multiplier\":-1,"
                        + "\"rounds_per_minute\":-4,\"fire_modes\":[]}").getAsJsonObject());
        assertEquals("pistol", definition.weaponClass());
        assertEquals(1, definition.shotsPerTrigger());
        assertEquals(1.0, definition.loadedCapacityMultiplier());
        assertEquals(300, definition.roundsPerMinute());
        assertEquals(1, definition.fireModes().size());
    }
}
