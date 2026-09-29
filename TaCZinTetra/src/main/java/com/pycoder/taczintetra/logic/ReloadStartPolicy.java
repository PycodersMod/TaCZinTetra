package com.pycoder.taczintetra.logic;

/** Pure preflight rules shared by network and direct TaCZ reload entry points. */
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
