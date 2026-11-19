package com.pycoder.taczintetra.client;

/** Readiness for observing TaCZ's native reload key without synthesizing input. */
public final class NativeReloadReadinessPolicy {
    private NativeReloadReadinessPolicy() { }

    public static boolean isReady(int magazineAmmo, int reserveAmmo, boolean hasBulletInBarrel) {
        return reserveAmmo > 0 && (magazineAmmo > 0 || hasBulletInBarrel);
    }
}
