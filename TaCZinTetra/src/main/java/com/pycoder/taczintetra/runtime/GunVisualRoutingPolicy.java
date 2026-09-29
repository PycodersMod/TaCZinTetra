package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;

/** Pure routing rules shared by future first/third-person visual adapters. */
public final class GunVisualRoutingPolicy {
    private GunVisualRoutingPolicy() {
    }

    public static Decision resolve(InteractionHand hand, boolean holdsModularGun, boolean dualPistol) {
        if (hand == null) {
            throw new IllegalArgumentException("hand cannot be null");
        }
        return new Decision(holdsModularGun, !dualPistol,
                hand == InteractionHand.OFF_HAND ? "off_hand" : "main_hand");
    }

    public record Decision(boolean createModelInstance, boolean adsAllowed, String animationChannel) {
    }
}
