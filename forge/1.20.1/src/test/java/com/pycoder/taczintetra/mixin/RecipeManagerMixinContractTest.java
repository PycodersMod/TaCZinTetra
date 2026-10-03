package com.pycoder.taczintetra.mixin;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 同时按配方 ID 和产出物品限制配方过滤。 */
class RecipeManagerMixinContractTest {
    private static final Path ROOT = Path.of("src/main/java");

    @Test
    void nativeRecipeFilterInspectsRecipeResultForCrossNamespaceRecipes() throws Exception {
        String source = Files.readString(
                ROOT.resolve("com/pycoder/taczintetra/mixin/RecipeManagerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("getResultItem"));
        assertTrue(source.contains("RegistryAccess.EMPTY"));
        assertTrue(source.contains("BuiltInRegistries.ITEM"));
        assertTrue(source.contains("shouldBlockNativeRecipe"));
    }
}
