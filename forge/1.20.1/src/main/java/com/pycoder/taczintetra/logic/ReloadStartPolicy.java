package com.pycoder.taczintetra.logic;

/** 网络入口与 TaCZ 直接装填入口共用的纯预检规则。 */
public final class ReloadStartPolicy {
    private ReloadStartPolicy() { }

    public static boolean allowed(boolean hasGun, boolean broken, boolean overheatLocked,
                                  boolean handReloading, boolean otherHandReloading,
                                  boolean magazineFull, boolean chamberedRound) {
        return hasGun && !broken && !overheatLocked
                && !handReloading && !otherHandReloading
                && (!magazineFull || chamberedRound);
    }
}
