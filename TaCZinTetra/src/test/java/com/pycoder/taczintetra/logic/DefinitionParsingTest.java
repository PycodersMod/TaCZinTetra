package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.config.ModuleConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefinitionParsingTest {
    @Test
    void gunDefinitionRemovesDuplicateFireModesAtParseBoundary() {
        JsonObject object = new JsonObject();
        JsonArray modes = new JsonArray();
        modes.add("semi");
        modes.add("semi");
        modes.add("burst");
        object.add("fire_modes", modes);

        assertEquals(java.util.List.of("semi", "burst"), GunDefinition.from(object).fireModes());
    }
    @Test void materialDefaultsAndItemsAreSafe() {
        GunMaterialDefinition value = GunMaterialDefinition.from(JsonParser.parseString("{\"physical_part_items\":[\"a:b\"],\"resource_capacity_multiplier\":2,\"stat_modifiers\":{\"damage\":1.25}}").getAsJsonObject());
        assertEquals(List.of("a:b"), value.physicalPartItems());
        assertEquals(2, value.capacityMultiplier());
        assertEquals(1, value.thermalConductivity());
        assertEquals(1.25, value.statModifiers().get("damage"));
    }

    @Test void ammoCostsIgnoreInvalidAndNegativeEntries() {
        AmmoRecipeDefinition value = AmmoRecipeDefinition.from(JsonParser.parseString("{\"projectile\":\"a:b\",\"feed_type\":\"tube\",\"batch_size\":4,\"cost\":{\"x:y\":2,\"bad\":-1}}").getAsJsonObject());
        assertEquals(4, value.batchSize());
        assertEquals("tube", value.feedType());
        assertEquals(Map.of("x:y", 2), value.cost());
    }

    @Test void repairAndChannelDefaultsAreSafe() {
        assertEquals(new RepairAgentDefinition("", 1), RepairAgentDefinition.from(null));
        ResourceChannelDefinition channel = ResourceChannelDefinition.from(JsonParser.parseString("{\"type\":\"energy\",\"external\":true}").getAsJsonObject());
        assertEquals("energy", channel.type());
        assertTrue(channel.external());
    }

    @Test void repairCostUsesMaterialAgentAndPartConfiguredCount() {
        ModuleConfig config = ModuleConfig.from(com.google.gson.JsonParser.parseString("""
                {"materials":[{"id":"iron","repair_agent":"iron_repair"}],
                 "repair_agents":[{"id":"iron_repair","item":"example:custom_repair"}],
                 "bodies":[{"id":"rifle","stats":{"repair_count":3}}]}
                """).getAsJsonObject());
        RepairCostResolver.Result result = RepairCostResolver.resolve(config, "body", "rifle", "iron");
        assertEquals("iron_repair", result.repairAgent());
        assertEquals("example:custom_repair", result.repairItem());
        assertEquals(3, result.count());
    }

    @Test void reloadPlanUsesAllResourceLimits() {
        AmmoRecipeDefinition recipe = AmmoRecipeDefinition.from(JsonParser.parseString("{\"batch_size\":4,\"cost\":{\"a\":2,\"b\":1}}").getAsJsonObject());
        ResourceAccount resources = ResourceAccount.empty().insert("a", 6, 6).account().insert("b", 10, 10).account();
        AmmoBatchCalculator.Result result = ReloadPlanCalculator.plan(20, 0, recipe, resources, true);
        assertEquals(3, result.paidBatches());
        assertEquals(12, result.loadedRounds());
    }
}
