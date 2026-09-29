package com.pycoder.taczintetra.logic;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunPartDefinitionTest {
    @Test
    void parsesBarrelAndFeedFields() {
        GunBarrelDefinition barrel = GunBarrelDefinition.from(JsonParser.parseString(
                "{\"projectile_type\":\"tacz:9mm\",\"pellets_per_round\":3,"
                        + "\"damage\":2.5,\"velocity\":1.2,\"pierce\":2,\"heat\":0.4}").getAsJsonObject());
        GunFeedDefinition feed = GunFeedDefinition.from(JsonParser.parseString(
                "{\"feed_type\":\"magazine\",\"resource_base_capacity\":12,"
                        + "\"reload_time_multiplier\":0.8}").getAsJsonObject());
        assertEquals("tacz:9mm", barrel.projectileType());
        assertEquals(3, barrel.pelletsPerRound());
        assertEquals(2.5, barrel.damage());
        assertEquals(2, barrel.pierce());
        assertEquals("magazine", feed.feedType());
        assertEquals(12, feed.resourceBaseCapacity());
        assertEquals(0.8, feed.reloadTimeMultiplier());
    }

    @Test
    void invalidPartValuesUseSafeDefaults() {
        GunBarrelDefinition barrel = GunBarrelDefinition.from(null);
        GunFeedDefinition feed = GunFeedDefinition.from(null);
        assertEquals(1, barrel.pelletsPerRound());
        assertEquals(1.0, barrel.damage());
        assertEquals(1, feed.resourceBaseCapacity());
        assertEquals(1.0, feed.reloadTimeMultiplier());
    }
}
