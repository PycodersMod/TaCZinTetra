package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.config.ModuleConfig;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentPolicyTest {
    @Test
    void allowsSupportedDurabilityEnchantments() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"enchantments":[
                  {"id":"minecraft:unbreaking","allowed":true},
                  {"id":"minecraft:mending","allowed":true},
                  {"id":"minecraft:vanishing_curse","allowed":true}
                ]}
                """).getAsJsonObject());
        assertTrue(EnchantmentPolicy.isAllowed("minecraft:unbreaking", config));
        assertTrue(EnchantmentPolicy.isAllowed("minecraft:mending", config));
        assertTrue(EnchantmentPolicy.isAllowed("minecraft:vanishing_curse", config));
    }

    @Test
    void rejectsBindingAndMalformedIds() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"enchantments":[{"id":"minecraft:binding_curse","allowed":false}]}
                """).getAsJsonObject());
        assertFalse(EnchantmentPolicy.isAllowed("minecraft:binding_curse", config));
        assertFalse(EnchantmentPolicy.isAllowed(null, config));
        assertFalse(EnchantmentPolicy.isAllowed("binding_curse", config));
    }

    @Test
    void editableCatalogControlsConfiguredEntries() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"enchantments":[
                  {"id":"minecraft:unbreaking","allowed":false},
                  {"id":"minecraft:mending","allowed":true}
                ]}
                """).getAsJsonObject());
        assertFalse(EnchantmentPolicy.isAllowed("minecraft:unbreaking", config));
        assertTrue(EnchantmentPolicy.isAllowed("minecraft:mending", config));
        assertFalse(EnchantmentPolicy.isAllowed("minecraft:vanishing_curse", config));
    }

    @Test
    void allExtensionCatalogsAreParsed() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"enchantments":[{"id":"e","allowed":true}],
                 "repair_agents":[{"id":"r","item":"x:y","stats":{"unit_cost":3}}],
                 "special_inlays":[{"id":"socket","slot":"special","stats":{"capacity":2}}]}
                """).getAsJsonObject());
        assertEquals(1, config.enchantments().size());
        assertEquals("x:y", config.repairAgents().get(0).item());
        assertEquals(2.0, config.specialInlays().get(0).stats().get("capacity"));
    }
}
