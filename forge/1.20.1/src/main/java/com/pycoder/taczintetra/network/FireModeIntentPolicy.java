package com.pycoder.taczintetra.network;

import com.tacz.guns.api.item.gun.FireMode;

/** 服务端验证并以确定方式切换某只手的开火模式。 */
public final class FireModeIntentPolicy {
    private FireModeIntentPolicy() { }

    public static boolean canAccept(int heldStackIdentity, int messageIdentity,
                                    boolean hasGun, boolean otherHandReloading) {
        return hasGun && !otherHandReloading && heldStackIdentity == messageIdentity;
    }

    public static FireMode next(FireMode current) {
        return switch (current == null ? FireMode.SEMI : current) {
            case SEMI -> FireMode.BURST;
            case BURST -> FireMode.AUTO;
            case AUTO, UNKNOWN -> FireMode.SEMI;
        };
    }
}
