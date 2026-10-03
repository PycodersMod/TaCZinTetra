package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;

/** 供后续第一/第三人称视觉适配器共用的纯路由规则。 */
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
