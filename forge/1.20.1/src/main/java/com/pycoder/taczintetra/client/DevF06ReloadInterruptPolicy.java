package com.pycoder.taczintetra.client;

/** Verifies that an interrupted reload left its original modular gun unchanged. */
final class DevF06ReloadInterruptPolicy {
    private DevF06ReloadInterruptPolicy() { }

    static boolean isSameLogicalGunStack(int expectedIdentity, int slotIdentity, boolean slotContainsGun) {
        return expectedIdentity > 0 && expectedIdentity == slotIdentity && slotContainsGun;
    }

    static boolean passes(int ammoBefore, int ammoAfter, int resourceBefore, int resourceAfter,
                          boolean reloadWasPending, boolean pendingAfter,
                          boolean holderStillReloading, boolean sameStack) {
        return ammoBefore >= 0
                && resourceBefore >= 0
                && reloadWasPending
                && !pendingAfter
                && !holderStillReloading
                && sameStack
                && ammoBefore == ammoAfter
                && resourceBefore == resourceAfter;
    }
}
