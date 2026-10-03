package com.pycoder.taczintetra.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
