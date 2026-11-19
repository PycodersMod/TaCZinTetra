package com.pycoder.taczintetra.client;

/** Bounded observer for TaCZ's server-synchronized draw cooldown transition. */
final class DevF06DrawCooldownPolicy {
    enum Outcome { WAITING_FOR_SERVER_DRAW, WAITING_FOR_COOLDOWN, READY, TIMEOUT }

    record Result(boolean observedPositiveCooldown, Outcome outcome) { }

    private DevF06DrawCooldownPolicy() { }

    static Result evaluate(boolean observedPositiveCooldown, long cooldownMillis,
                           int elapsedTicks, int maximumWaitTicks) {
        if (cooldownMillis < 0 || elapsedTicks < 0 || maximumWaitTicks <= 0) {
            throw new IllegalArgumentException("Expected non-negative cooldown and elapsed ticks, with a positive bound");
        }
        if (observedPositiveCooldown && cooldownMillis == 0) {
            return new Result(true, Outcome.READY);
        }
        if (elapsedTicks >= maximumWaitTicks) {
            return new Result(observedPositiveCooldown, Outcome.TIMEOUT);
        }
        if (cooldownMillis > 0) {
            return new Result(true, Outcome.WAITING_FOR_COOLDOWN);
        }
        return new Result(false, Outcome.WAITING_FOR_SERVER_DRAW);
    }
}
