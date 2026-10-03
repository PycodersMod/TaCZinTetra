package com.pycoder.taczintetra.logic;

/** 独立于弹丸数量解析弹匣/供弹消耗。 */
@FunctionalInterface
public interface FeedStrategy {
    FeedStrategy DEFAULT = AmmoConsumptionPolicy::consume;

    AmmoConsumptionPolicy.Result consume(int loadedRounds, int independentShots);
}
