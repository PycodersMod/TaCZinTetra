package com.pycoder.taczintetra.logic;

/** Rejects malformed client aim values before they reach TaCZ projectile code. */
public final class AimInputPolicy {
    private AimInputPolicy() {
    }

    public static boolean valid(float pitch, float yaw) {
        return Float.isFinite(pitch) && Float.isFinite(yaw);
    }
}
