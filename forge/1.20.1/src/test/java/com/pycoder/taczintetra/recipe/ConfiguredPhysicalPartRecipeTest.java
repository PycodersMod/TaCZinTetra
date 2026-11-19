package com.pycoder.taczintetra.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredPhysicalPartRecipeTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void everyDefaultTetraPhysicalPartHasASurvivalCraftingRecipe() throws Exception {
        JsonObject defaults = JsonParser.parseString(Files.readString(
                RESOURCES.resolve("defaultconfigs/taczintetra.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> outputs = new HashSet<>();

        for (var material : defaults.getAsJsonArray("materials")) {
            JsonArray parts = material.getAsJsonObject().getAsJsonArray("physical_part_items");
            for (var part : parts) {
                String id = part.getAsString();
                String path = id.substring("taczintetra:".length());
                Path recipePath = RESOURCES.resolve("data/taczintetra/recipes/parts/" + path + ".json");
                assertTrue(Files.exists(recipePath), "no survival recipe for configured physical part " + id);

                JsonObject recipe = JsonParser.parseString(Files.readString(recipePath, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), id);
                assertEquals(id, recipe.getAsJsonObject("result").get("item").getAsString());
                JsonObject keys = recipe.getAsJsonObject("key");
                JsonArray pattern = recipe.getAsJsonArray("pattern");
                assertTrue(keys.size() > 0, "recipe has no ingredients: " + id);
                assertTrue(pattern.size() > 0, "recipe has no pattern: " + id);
                for (var row : pattern) {
                    for (char symbol : row.getAsString().toCharArray()) {
                        if (symbol != ' ') {
                            assertTrue(keys.has(String.valueOf(symbol)),
                                    "recipe pattern references an undefined ingredient symbol: " + id + "/" + symbol);
                        }
                    }
                }
                assertTrue(outputs.add(id), "duplicate configured physical-part recipe output: " + id);
            }
        }

        assertFalse(outputs.isEmpty());
    }

    @Test
    void woodMagazineRecipeDoesNotShadowTheVanillaCraftingTableRecipe() throws Exception {
        JsonObject recipe = JsonParser.parseString(Files.readString(
                RESOURCES.resolve("data/taczintetra/recipes/parts/wood_magazine.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray pattern = recipe.getAsJsonArray("pattern");

        assertFalse(pattern.size() == 2
                        && pattern.get(0).getAsString().equals("PP")
                        && pattern.get(1).getAsString().equals("PP"),
                "four planks in a square are reserved for the vanilla crafting table result");
        assertEquals(3, pattern.toString().chars().filter(character -> character == 'P').count(),
                "the early wood magazine stays craftable in the 2x2 inventory grid with three planks");
    }
}
