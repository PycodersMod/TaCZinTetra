package com.pycoder.taczintetra.logic;

import com.tacz.guns.api.item.gun.FireMode;

import java.util.List;

/** Keeps the runtime fire mode constrained by the native TaCZ gun definition. */
public final class FireModePolicy {
    private FireModePolicy() { }

    public static boolean isAllowed(FireMode current, List<FireMode> allowedModes) {
        return current != null && allowedModes != null && !allowedModes.isEmpty()
                && current != FireMode.UNKNOWN && allowedModes.contains(current);
    }

    public static FireMode next(FireMode current, List<FireMode> allowedModes) {
        if (allowedModes == null || allowedModes.isEmpty()) return FireMode.SEMI;
        int currentIndex = allowedModes.indexOf(current);
        if (currentIndex < 0) return allowedModes.get(0);
        return allowedModes.get((currentIndex + 1) % allowedModes.size());
    }
}
