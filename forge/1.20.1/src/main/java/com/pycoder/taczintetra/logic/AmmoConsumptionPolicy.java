package com.pycoder.taczintetra.logic;

/** 每次独立扣动扳机消耗一发已装填弹药，不按弹丸数量消耗。 */
public final class AmmoConsumptionPolicy {
    private AmmoConsumptionPolicy() {
    }

    public static Result consume(int loadedRounds, int independentShots) {
        int available = Math.max(0, loadedRounds);
        int requested = Math.max(0, independentShots);
        int consumed = Math.min(available, requested);
        return new Result(available - consumed, consumed);
    }

    public record Result(int remainingLoadedRounds, int consumedRounds) {
    }
}
