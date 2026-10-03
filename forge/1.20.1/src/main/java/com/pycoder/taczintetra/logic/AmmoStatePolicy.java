package com.pycoder.taczintetra.logic;

/** Overflow-safe normalization for TaCZ dummy-ammo NBT state. */
public final class AmmoStatePolicy {
    private AmmoStatePolicy() {
    }

    public static int sanitizeMax(int value, int fallback) {
        return value > 0 ? value : Math.max(0, fallback);
    }

    public static int addSaturated(int current, int delta) {
        long result = (long) Math.max(0, current) + delta;
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, result));
    }
}
