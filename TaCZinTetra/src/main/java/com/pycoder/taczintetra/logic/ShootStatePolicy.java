package com.pycoder.taczintetra.logic;

/** Mirrors the TaCZ state gates that must precede a modular shot. */
public final class ShootStatePolicy {
    private ShootStatePolicy() {
    }

    public static boolean allowed(boolean reloading, boolean bolting, float sprintTime) {
        return !reloading && !bolting && Float.isFinite(sprintTime) && sprintTime <= 0.0f;
    }
}
