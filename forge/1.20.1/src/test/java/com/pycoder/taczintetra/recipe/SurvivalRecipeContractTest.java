package com.pycoder.taczintetra.recipe;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 确保独立的默认弹药获取链不会意外消失。 */
class SurvivalRecipeContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Pattern INGREDIENT_TOKEN = Pattern.compile("\\\"(item|tag)\\\":\\\"([^\\\"]+)\\\"");

    @Test
    void registersAConfigurableAmmoRecipeSerializer() throws Exception {
        String registry = Files.readString(
                MAIN.resolve("java/com/pycoder/taczintetra/registry/ModRecipeSerializers.java"),
                StandardCharsets.UTF_8);
        assertTrue(registry.contains("AMMO_RESOURCE"));
        assertTrue(registry.contains("AmmoResourceRecipeSerializer"));
    }

    @Test
    void shipsOneDefaultRecipeForEveryConfiguredProjectile() throws Exception {
        List<String> projectiles = List.of("9mm", "45acp", "556", "762", "338", "12g", "40mm");
        for (String projectile : projectiles) {
            Path recipe = MAIN.resolve("resources/data/taczintetra/recipes/ammo_" + projectile + ".json");
            assertTrue(Files.exists(recipe), "missing default ammo recipe: " + projectile);
            String source = Files.readString(recipe, StandardCharsets.UTF_8);
            assertTrue(source.contains("tacz:ammo"));
            assertTrue(source.contains("tacz:" + projectile) || (projectile.equals("762") && source.contains("tacz:762x39"))
                    || (projectile.equals("338") && source.contains("tacz:338_lapua_magnum")));
            assertTrue(source.contains("ammo_id"));
        }
    }

    @Test
    void defaultAmmoRecipesHaveDistinctShapelessInputs() throws Exception {
        List<String> projectiles = List.of("9mm", "45acp", "556", "762", "338", "12g", "40mm");
        Set<String> signatures = new HashSet<>();
        for (String projectile : projectiles) {
            String source = Files.readString(
                    MAIN.resolve("resources/data/taczintetra/recipes/ammo_" + projectile + ".json"),
                    StandardCharsets.UTF_8);
            int start = source.indexOf("\"ingredients\"");
            int end = source.indexOf("\"result\"");
            assertTrue(start >= 0 && end > start, "missing ingredient section: " + projectile);
            Matcher matcher = INGREDIENT_TOKEN.matcher(source.substring(start, end));
            List<String> tokens = new java.util.ArrayList<>();
            while (matcher.find()) tokens.add(matcher.group(1) + ":" + matcher.group(2));
            tokens.sort(String::compareTo);
            assertTrue(signatures.add(String.join("|", tokens)),
                    "duplicate shapeless input makes ammo recipes ambiguous: " + projectile);
        }
    }
}
