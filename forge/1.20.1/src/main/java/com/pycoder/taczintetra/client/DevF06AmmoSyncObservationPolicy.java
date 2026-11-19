package com.pycoder.taczintetra.client;

/** Bounds the dev probe's wait for the authoritative single-round ammo update. */
final class DevF06AmmoSyncObservationPolicy {
    enum Outcome { WAITING, CONFIRMED, FAILED }

    private DevF06AmmoSyncObservationPolicy() { }

    static Outcome evaluate(int ammoBefore, int observedAmmo, int elapsedTicks, int maximumWaitTicks) {
        if (ammoBefore <= 0 || elapsedTicks < 0 || maximumWaitTicks <= 0) {
            throw new IllegalArgumentException("Expected positive ammo and wait bound, with non-negative elapsed ticks");
        }
        if (observedAmmo == ammoBefore - 1) return Outcome.CONFIRMED;
        if (observedAmmo != ammoBefore) return Outcome.FAILED;
        return elapsedTicks >= maximumWaitTicks ? Outcome.FAILED : Outcome.WAITING;
    }
}
