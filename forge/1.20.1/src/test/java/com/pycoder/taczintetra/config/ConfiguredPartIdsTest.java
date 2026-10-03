package com.pycoder.taczintetra.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguredPartIdsTest {
    @Test
    void collectsUniqueLocalFinishedPartIdsInConfigOrder() {
        ModuleConfig config = ModuleConfig.from(com.google.gson.JsonParser.parseString("""
                {"materials":[
                  {"id":"wood","physical_part_items":["taczintetra:wood_body","taczintetra:wood_barrel"]},
                  {"id":"iron","physical_part_items":["taczintetra:iron_body","taczintetra:wood_body","other:foreign"]}
                ]}
                """).getAsJsonObject());

        assertEquals(List.of("wood_body", "wood_barrel", "iron_body"), ConfiguredPartIds.localPaths(config));
    }
}
