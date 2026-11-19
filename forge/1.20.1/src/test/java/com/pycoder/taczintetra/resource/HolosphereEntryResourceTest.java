package com.pycoder.taczintetra.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HolosphereEntryResourceTest {
    private static final Path ENTRY = Path.of(
            "src/main/resources/assets/tetra/holosphere_entries/taczintetra.json");

    @Test
    void registersTheModularGunInTetraHolosphereCatalog() throws Exception {
        assertTrue(Files.exists(ENTRY));
        JsonObject root = JsonParser.parseString(Files.readString(ENTRY, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals("taczintetra:starter_pistol", root.get("item").getAsString());
        assertEquals(8, root.get("position").getAsInt());
        assertEquals("taczintetra:textures/item/blank.png",
                root.getAsJsonObject("icon").get("textureLocation").getAsString());
    }

    @Test
    void localizesTheHolosphereGunCategorySpecialSlotAndInlayCategory() throws Exception {
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            Path languageFile = Path.of("src/main/resources/assets/taczintetra/lang/" + locale + ".json");
            JsonObject translations = JsonParser.parseString(
                    Files.readString(languageFile, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : new String[]{
                    "tetra.holo.craft.taczintetra",
                    "tetra.slot.taczintetra/special",
                    "tetra.variant_category.taczintetra.label"}) {
                assertTrue(translations.has(key), locale + " is missing translation " + key);
                assertFalse(translations.get(key).getAsString().isBlank(),
                        locale + " has blank translation " + key);
                assertFalse(translations.get(key).getAsString().equals(key),
                        locale + " exposes raw translation key " + key);
            }
        }
    }

    @Test
    void localizesSpecialSocketSchematicBreadcrumbAndDescription() throws Exception {
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            Path languageFile = Path.of("src/main/resources/assets/taczintetra/lang/" + locale + ".json");
            JsonObject translations = JsonParser.parseString(
                    Files.readString(languageFile, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : new String[]{
                    "tetra/schematic/taczintetra/special_socket.name",
                    "tetra/schematic/taczintetra/special_socket.description",
                    "tetra/schematic/taczintetra/special_socket.description_details",
                    "tetra/schematic/taczintetra/special_socket.slot1"}) {
                assertTrue(translations.has(key), locale + " is missing translation " + key);
                assertFalse(translations.get(key).getAsString().isBlank(),
                        locale + " has blank translation " + key);
                assertFalse(translations.get(key).getAsString().equals(key),
                        locale + " exposes raw translation key " + key);
            }
        }
    }
}
