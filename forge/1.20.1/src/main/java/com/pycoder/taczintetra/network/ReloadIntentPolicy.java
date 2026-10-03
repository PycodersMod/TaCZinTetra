package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.logic.ReloadStartPolicy;

/** Server-side policy core; the runtime adapter supplies authoritative player state. */
public final class ReloadIntentPolicy {
    private ReloadIntentPolicy() {
    }

    public static boolean canAccept(ReloadIntentMessage intent, int heldStackIdentity,
                                    long lastAcceptedSequence, boolean hasGun,
                                    boolean otherHandReloading, boolean broken,
                                    boolean overheatLocked) {
        return ReloadIntentValidator.isValid(intent)
                && intent.stackIdentity() == heldStackIdentity
                && intent.sequence() > lastAcceptedSequence
                && ReloadStartPolicy.allowed(hasGun, broken, overheatLocked,
                false, otherHandReloading, false, false);
    }
}
