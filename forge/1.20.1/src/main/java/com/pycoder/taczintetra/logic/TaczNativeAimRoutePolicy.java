package com.pycoder.taczintetra.logic;

/** Extends TaCZ's native AimKey eligibility to Tetra guns without duplicating its aim behavior. */
public final class TaczNativeAimRoutePolicy {
    private TaczNativeAimRoutePolicy() { }

    public static boolean allowMainHandAim(boolean nativeTaCZGun, boolean modularGun,
                                            boolean offHandModularGun, boolean mainHandTwoHanded) {
        if (nativeTaCZGun) return true;
        return modularGun && (!offHandModularGun || mainHandTwoHanded);
    }
}
