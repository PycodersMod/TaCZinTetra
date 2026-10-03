package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;

/** Authoritative-ready coordination primitive that prevents simultaneous reloads. */
public final class DualHandReloadState {
    private InteractionHand activeHand;

    public boolean begin(InteractionHand hand) {
        if (hand == null || activeHand != null) {
            return false;
        }
        activeHand = hand;
        return true;
    }

    public boolean isReloading(InteractionHand hand) {
        return hand != null && activeHand == hand;
    }

    public void complete(InteractionHand hand) {
        release(hand);
    }

    public void interrupt(InteractionHand hand) {
        release(hand);
    }

    private void release(InteractionHand hand) {
        if (activeHand == hand) {
            activeHand = null;
        }
    }
}
