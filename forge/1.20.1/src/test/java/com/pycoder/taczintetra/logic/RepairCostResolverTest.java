package com.pycoder.taczintetra.logic;

import com.google.gson.JsonParser;
import com.pycoder.taczintetra.config.ModuleConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepairCostResolverTest {
    @Test
    void resolvesTetraNamespacedMaterialPathAgainstShortCatalogId() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {
                  "materials":[{"id":"iron","repair_agent":"iron_repair"}],
                  "bodies":[{"id":"pistol","stats":{"repair_count":2}}],
                  "repair_agents":[{"id":"iron_repair","item":"taczintetra:iron_repair"}]
                }
                """).getAsJsonObject());

        RepairCostResolver.Result result = RepairCostResolver.resolve(
                config, "body", "pistol", "taczintetra/iron/");

        assertEquals("iron_repair", result.repairAgent());
        assertEquals("taczintetra:iron_repair", result.repairItem());
        assertEquals(2, result.count());
    }

    @Test
    void resolvesNamespacedSlotPathAgainstConfiguredRepairCount() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {
                  "materials":[{"id":"iron","repair_agent":"iron_repair"}],
                  "bodies":[{"id":"pistol","stats":{"repair_count":3}}],
                  "repair_agents":[{"id":"iron_repair","item":"taczintetra:iron_repair"}]
                }
                """).getAsJsonObject());

        RepairCostResolver.Result result = RepairCostResolver.resolve(
                config, "taczintetra/body/", "pistol", "iron");

        assertEquals(3, result.count());
    }

    @Test
    void usesRepairAgentUnitCostWhenPartHasNoOverride() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {
                  "materials":[{"id":"iron","repair_agent":"iron_repair"}],
                  "bodies":[{"id":"pistol","stats":{}}],
                  "repair_agents":[{"id":"iron_repair","item":"taczintetra:iron_repair","stats":{"unit_cost":4}}]
                }
                """).getAsJsonObject());

        RepairCostResolver.Result result = RepairCostResolver.resolve(
                config, "body", "pistol", "iron");

        assertEquals(4, result.count());
    }

    @Test
    void resolvesNamespacedRepairAgentIdAgainstCatalogEntry() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {
                  "materials":[{"id":"iron","repair_agent":"taczintetra:iron_repair"}],
                  "bodies":[{"id":"pistol","stats":{}}],
                  "repair_agents":[{"id":"taczintetra/iron_repair","item":" taczintetra:iron_repair ","stats":{"unit_cost":3}}]
                }
                """).getAsJsonObject());

        RepairCostResolver.Result result = RepairCostResolver.resolve(
                config, "body", "pistol", "iron");

        assertEquals("taczintetra:iron_repair", result.repairAgent());
        assertEquals("taczintetra:iron_repair", result.repairItem());
        assertEquals(3, result.count());
    }
}
