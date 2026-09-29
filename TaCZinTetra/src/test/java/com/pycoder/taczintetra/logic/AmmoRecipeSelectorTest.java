package com.pycoder.taczintetra.logic;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmmoRecipeSelectorTest {
    @Test
    void selectsByProjectileAndFeedType() {
        AmmoRecipeDefinition tube = new AmmoRecipeDefinition("tacz:9mm", "tube", 1, Map.of("shell", 2));
        AmmoRecipeDefinition magazine = new AmmoRecipeDefinition("tacz:9mm", "magazine", 2, Map.of("shell", 1));
        Map<ResourceLocation, AmmoRecipeDefinition> recipes = Map.of(
                ResourceLocation.parse("taczintetra:tube"), tube,
                ResourceLocation.parse("taczintetra:magazine"), magazine);
        assertEquals(tube, AmmoRecipeSelector.select(recipes, "tacz:9mm", "tube"));
        assertNull(AmmoRecipeSelector.select(recipes, "tacz:12g", "tube"));
    }
}
