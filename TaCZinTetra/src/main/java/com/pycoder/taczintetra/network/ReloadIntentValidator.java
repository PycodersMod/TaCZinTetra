package com.pycoder.taczintetra.network;

/** Validates only transport-level reload intent invariants on the server. */
public final class ReloadIntentValidator {
    private ReloadIntentValidator() {
    }

    public static boolean isValid(ReloadIntentMessage message) {
        return message != null && message.stackIdentity() >= 0 && message.sequence() > 0;
    }
}
