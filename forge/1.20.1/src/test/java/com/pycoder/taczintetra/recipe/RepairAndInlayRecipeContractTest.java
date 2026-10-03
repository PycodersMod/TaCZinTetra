package com.pycoder.taczintetra.recipe;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 确保生存模式下可按默认方式获取修理材料和特殊嵌片。 */
class RepairAndInlayRecipeContractTest {
    private static final Path ROOT = Path.of("src/main/resources/data/taczintetra/recipes");

    @Test
    void shipsAllFiveMaterialRepairAgentRecipes() throws Exception {
        for (String material : List.of("wood", "stone", "iron", "gold", "netherite")) {
            Path recipe = ROOT.resolve("repair_" + material + ".json");
            assertTrue(Files.exists(recipe), "missing repair recipe: " + material);
            String source = Files.readString(recipe, StandardCharsets.UTF_8);
            assertTrue(source.contains("taczintetra:" + material + "_repair"));
            assertTrue(source.contains("ingredients"));
        }
    }

    @Test
    void shipsARecipeForTheSpecialInlayItem() throws Exception {
        Path recipe = ROOT.resolve("special_inlay.json");
        assertTrue(Files.exists(recipe));
        String source = Files.readString(recipe, StandardCharsets.UTF_8);
        assertTrue(source.contains("taczintetra:special_inlay"));
        assertTrue(source.contains("ingredients"));
    }
}
