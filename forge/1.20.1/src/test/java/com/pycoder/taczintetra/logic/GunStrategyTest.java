package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunStrategyTest {
    @Test
    void bodyStrategyUsesConfiguredIndependentShots() {
        GunDefinition body = new GunDefinition("pistol", java.util.List.of("semi"),
                2, 1.0, 300, false, "taczintetra:standard");
        assertEquals(2, FireControlStrategy.DEFAULT.independentShots(body));
    }

    @Test
    void feedStrategyConsumesRoundsOncePerIndependentShot() {
        assertEquals(2, FeedStrategy.DEFAULT.consume(5, 3).remainingLoadedRounds());
        assertEquals(3, FeedStrategy.DEFAULT.consume(5, 3).consumedRounds());
    }

    @Test
    void barrelStrategyAppliesPelletCountAndSafetyLimit() {
        GunBarrelDefinition barrel = new GunBarrelDefinition(
                "tacz:9mm", 12, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);
        assertEquals(24, ProjectileStrategy.DEFAULT.projectileCount(2, barrel, 24));
        assertEquals(10, ProjectileStrategy.DEFAULT.projectileCount(2, barrel, 10));
    }
}
