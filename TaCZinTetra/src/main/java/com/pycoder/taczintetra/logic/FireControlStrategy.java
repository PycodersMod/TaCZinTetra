package com.pycoder.taczintetra.logic;

/** Resolves body-controlled independent trigger shots. */
@FunctionalInterface
public interface FireControlStrategy {
    FireControlStrategy DEFAULT = definition -> definition == null ? 0 : definition.shotsPerTrigger();

    int independentShots(GunDefinition definition);
}
