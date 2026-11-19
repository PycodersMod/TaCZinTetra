package com.pycoder.taczintetra.client;

/** Bounded gate for starting a host-side diagnostic only after a LAN guest joins. */
final class DevF06GuestJoinGatePolicy {
    enum Outcome { WAITING_FOR_GUEST, READY, TIMEOUT }

    private DevF06GuestJoinGatePolicy() { }

    static Outcome evaluate(int onlinePlayerCount, int elapsedTicks, int maximumWaitTicks) {
        if (onlinePlayerCount < 0 || elapsedTicks < 0 || maximumWaitTicks <= 0) {
            throw new IllegalArgumentException("Expected non-negative player count and elapsed ticks, with a positive bound");
        }
        if (onlinePlayerCount >= 2) return Outcome.READY;
        if (elapsedTicks >= maximumWaitTicks) return Outcome.TIMEOUT;
        return Outcome.WAITING_FOR_GUEST;
    }
}
