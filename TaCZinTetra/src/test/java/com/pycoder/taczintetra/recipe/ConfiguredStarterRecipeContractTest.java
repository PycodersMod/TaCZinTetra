package com.pycoder.taczintetra.recipe;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredStarterRecipeContractTest {
    @Test
    void starterResultAppliesAllConfiguredMainModules() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/recipe/ConfiguredStarterRecipe.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("INITIAL_BODY.get()"));
        assertTrue(source.contains("INITIAL_BARREL.get()"));
        assertTrue(source.contains("INITIAL_MAGAZINE.get()"));
        assertTrue(source.contains("INITIAL_BODY_MATERIAL.get()"));
        assertTrue(source.contains("IModularItem.putModuleInSlot"));
        assertTrue(source.contains("GunModuleSlots.BODY"));
        assertTrue(source.contains("GunModuleSlots.BARREL"));
        assertTrue(source.contains("GunModuleSlots.MAGAZINE"));
    }
}
