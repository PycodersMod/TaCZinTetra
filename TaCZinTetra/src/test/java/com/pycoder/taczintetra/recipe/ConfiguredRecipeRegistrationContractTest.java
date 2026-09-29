package com.pycoder.taczintetra.recipe;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredRecipeRegistrationContractTest {
    @Test
    void starterRecipeUsesARegisteredConfiguredSerializer() throws Exception {
        String recipe = Files.readString(Path.of(
                "src/main/resources/data/taczintetra/recipes/starter_pistol.json"),
                StandardCharsets.UTF_8);
        String registry = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/registry/ModRecipeSerializers.java"),
                StandardCharsets.UTF_8);
        assertTrue(recipe.contains("taczintetra:configured_shaped"));
        assertTrue(registry.contains("CONFIGURED_SHAPED"));
    }
}
