package com.pycoder.taczintetra.logic;

/** Resolves magazine/feed consumption independently from projectile count. */
@FunctionalInterface
public interface FeedStrategy {
    FeedStrategy DEFAULT = AmmoConsumptionPolicy::consume;

    AmmoConsumptionPolicy.Result consume(int loadedRounds, int independentShots);
}
