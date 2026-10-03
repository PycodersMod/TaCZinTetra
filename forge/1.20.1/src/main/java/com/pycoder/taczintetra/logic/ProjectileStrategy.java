package com.pycoder.taczintetra.logic;

/** Resolves barrel pellet multiplication with an explicit safety limit. */
@FunctionalInterface
public interface ProjectileStrategy {
    ProjectileStrategy DEFAULT = (shots, barrel, limit) -> barrel == null
            ? 0 : ShotCountCalculator.totalProjectiles(shots, barrel.pelletsPerRound(), limit);

    int projectileCount(int independentShots, GunBarrelDefinition barrel, int safetyLimit);
}
