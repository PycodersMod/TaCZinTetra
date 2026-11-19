package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 防止可选 JEI 集成退化为仅隐藏物品。 */
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
    void jeiResultFilterBypassesRegistryBoundTrimRecipes() throws Exception {
        String source = Files.readString(ROOT.resolve("java/com/pycoder/taczintetra/compat/jei/TaczJeiPlugin.java"),
                StandardCharsets.UTF_8);
        int guard = source.indexOf("requiresDynamicRegistryForResult(recipe)");
        int resultInspection = source.indexOf("recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY)");

        assertTrue(guard >= 0, "JEI result filtering must share the registry-bound recipe guard");
        assertTrue(resultInspection > guard,
                "JEI must bypass result resolution for recipes requiring dynamic registries");
        assertTrue(source.substring(guard, resultInspection).contains("original.accept(recipe)")
                        || source.substring(guard, resultInspection).contains("continue;"),
                "registry-bound recipes must stay visible to JEI");
    }

    @Test
    void sharedGuardTargetsSmithingTrimRecipes() throws Exception {
        String source = Files.readString(
                ROOT.resolve("java/com/pycoder/taczintetra/compat/NativeTaczRestrictionService.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("boolean requiresDynamicRegistryForResult(Recipe<?> recipe)"));
        assertTrue(source.contains("return recipe instanceof SmithingTrimRecipe;"));
    }

    @Test
    void keepsJeiDevelopmentRuntimeOptionalForPublishedArtifact() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        assertTrue(build.contains("runtimeOnly(fg.deobf(\"mezz.jei:jei-1.20.1-forge:15.20.0.106\"))"));
        assertTrue(build.contains("include(\"*jei*15.20.0.106_mapped_official_1.20.1.jar\")"));
    }
}
