package com.pycoder.taczintetra.network;

/** Server-side replay gate; actual reload settlement remains a separate step. */
public final class ReloadIntentServerGate {
    private long mainHandSequence;
    private long offHandSequence;
    private long mainHandBeforeReservation;
    private long offHandBeforeReservation;

    public boolean accept(ReloadIntentMessage intent, int heldStackIdentity, boolean hasGun,
                          boolean otherHandReloading, boolean broken, boolean overheatLocked) {
        long last = intent != null && intent.offHand() ? offHandSequence : mainHandSequence;
        if (!ReloadIntentPolicy.canAccept(intent, heldStackIdentity, last, hasGun,
                otherHandReloading, broken, overheatLocked)) {
            return false;
        }
        if (intent.offHand()) {
            offHandBeforeReservation = offHandSequence;
            offHandSequence = intent.sequence();
        } else {
            mainHandBeforeReservation = mainHandSequence;
            mainHandSequence = intent.sequence();
        }
        return true;
    }

    /** Rolls back the most recent reservation when the lifecycle start fails. */
    public void rollback(ReloadIntentMessage intent) {
        if (intent == null) return;
        if (intent.offHand() && offHandSequence == intent.sequence()) {
            offHandSequence = offHandBeforeReservation;
        } else if (!intent.offHand() && mainHandSequence == intent.sequence()) {
            mainHandSequence = mainHandBeforeReservation;
        }
    }
}
