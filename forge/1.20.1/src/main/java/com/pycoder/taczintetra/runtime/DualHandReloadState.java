package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;

/** 可供权威逻辑使用的协调原语，用于阻止并发换弹。 */
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
