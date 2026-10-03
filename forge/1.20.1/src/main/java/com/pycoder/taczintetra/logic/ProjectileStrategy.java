package com.pycoder.taczintetra.logic;

/** 使用明确的安全上限解析枪管弹丸倍率。 */
@FunctionalInterface
public interface ProjectileStrategy {
    ProjectileStrategy DEFAULT = (shots, barrel, limit) -> barrel == null
            ? 0 : ShotCountCalculator.totalProjectiles(shots, barrel.pelletsPerRound(), limit);

    int projectileCount(int independentShots, GunBarrelDefinition barrel, int safetyLimit);
}
