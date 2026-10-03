package com.pycoder.taczintetra.runtime;

/** Generates positive reload intent sequence numbers without signed overflow. */
public final class ReloadSequence {
    private ReloadSequence() {
    }

    public static int next(int current) {
        return current >= Integer.MAX_VALUE ? 1 : Math.max(0, current) + 1;
    }
}
