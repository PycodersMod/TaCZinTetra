package com.pycoder.taczintetra.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfiguredAmmoRecipeResolverTest {
    @Test
    void selectsRecipeByBarrelProjectileAndFeedType() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"ammo_recipes":[
                  {"projectile":"tacz:9mm","feed_type":"magazine","batch_size":3,"cost":{"tacz:9mm":2}},
                  {"projectile":"tacz:9mm","feed_type":"internal","batch_size":1,"cost":{"tacz:9mm":1}}
                ]}
                """).getAsJsonObject());

        var recipe = ConfiguredAmmoRecipeResolver.select(config, "tacz:9mm", "magazine");

        assertEquals(3, recipe.batchSize());
        assertEquals(2, recipe.cost().get("tacz:9mm"));
        assertNull(ConfiguredAmmoRecipeResolver.select(config, "tacz:9mm", "drum"));
    }

    @Test
    void resolvesProjectileIdFromTheSelectedBarrelInsteadOfTheNativeTemplate() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {
                  "materials":[{"id":"wood"}],
                  "bodies":[{"id":"pistol","materials":["wood"]}],
                  "barrels":[{"id":"40mm","materials":["wood"],"projectile_type":"tacz:40mm"}],
                  "magazines":[{"id":"standard","materials":["wood"],"feed_type":"magazine"}]
                }
                """).getAsJsonObject());
        var modules = new TetraItemStackProfileResolver.SelectedModules(
                new TetraModuleSelection("pistol", "wood"),
                new TetraModuleSelection("40mm", "wood"),
                new TetraModuleSelection("standard", "wood"));

        assertEquals("tacz:40mm", ConfiguredAmmoRecipeResolver.projectileId(config, modules));
    }
}
