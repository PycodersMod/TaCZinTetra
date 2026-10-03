package com.pycoder.taczintetra.logic;

/** 解析由枪身控制的独立扳机射击。 */
@FunctionalInterface
public interface FireControlStrategy {
    FireControlStrategy DEFAULT = definition -> definition == null ? 0 : definition.shotsPerTrigger();

    int independentShots(GunDefinition definition);
}
