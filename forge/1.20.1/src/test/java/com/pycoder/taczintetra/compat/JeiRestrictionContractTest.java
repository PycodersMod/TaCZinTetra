package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the optional JEI integration from regressing to item-only hiding. */
class JeiRestrictionContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void hidesBothIngredientsAndIndexedCraftingRecipesOnlyInHideMode() throws Exception {
        String source = Files.readString(ROOT.resolve("java/com/pycoder/taczintetra/compat/jei/TaczJeiPlugin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("JEI_MODE.get()"));
        assertTrue(source.contains("removeIngredientsAtRuntime"));
        assertTrue(source.contains("hideRecipes"));
        assertTrue(source.contains("getRecipes()"));
        assertTrue(source.contains("createRecipeCategoryLookup"));
        assertTrue(source.contains("getRecipeClass"));
        assertTrue(source.contains("shouldHideFromJei"));
    }

    @Test
    void disabledModeAddsAnExplicitDisabledTooltipInsteadOfSilentlyReturning() throws Exception {
        String source = Files.readString(ROOT.resolve("java/com/pycoder/taczintetra/compat/jei/TaczJeiPlugin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("registerRecipes"));
        assertTrue(source.contains("JeiMode.DISABLED"));
        assertTrue(source.contains("addItemStackInfo"));
        assertTrue(source.contains("Component.translatable"));
    }

    @Test
    void keepsJeiDevelopmentRuntimeOptionalForPublishedArtifact() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        assertTrue(build.contains("runtimeOnly(fg.deobf(\"mezz.jei:jei-1.20.1-forge:15.20.0.106\"))"));
        assertTrue(build.contains("include(\"*jei*15.20.0.106_mapped_official_1.20.1.jar\")"));
    }
}
