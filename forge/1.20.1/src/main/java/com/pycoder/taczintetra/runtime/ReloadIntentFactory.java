package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.network.ReloadIntentMessage;

/** Converts the already-resolved client input into a transport-only intent. */
public final class ReloadIntentFactory {
    private ReloadIntentFactory() {
    }

    public static ReloadIntentMessage create(GunInputRouter.Decision decision,
                                             int stackIdentity, int sequence) {
        if (decision == null) {
            throw new IllegalArgumentException("decision");
        }
        return new ReloadIntentMessage(decision.routeReloadToOffHand(),
                !decision.fillReload(), stackIdentity, sequence);
    }
}
