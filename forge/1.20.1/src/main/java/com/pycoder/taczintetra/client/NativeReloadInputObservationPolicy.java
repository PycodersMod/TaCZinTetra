package com.pycoder.taczintetra.client;

import org.lwjgl.glfw.GLFW;

/** Pure diagnostic gates for observing TaCZ's native reload-key path. */
public final class NativeReloadInputObservationPolicy {
    private NativeReloadInputObservationPolicy() { }

    public static boolean shouldObserve(boolean enabled, int action, boolean reloadKeyMatched) {
        return enabled && action == GLFW.GLFW_PRESS && reloadKeyMatched;
    }

    public static boolean nativeGateOpen(boolean inGame, boolean playerPresent, boolean spectator,
                                         boolean mainHandIsGun, boolean usesInventoryAmmo) {
        return inGame && playerPresent && !spectator && mainHandIsGun && !usesInventoryAmmo;
    }
}
