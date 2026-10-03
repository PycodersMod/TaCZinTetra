package com.pycoder.taczintetra.runtime;

/** Pure input routing policy; actual key bindings are supplied by the client adapter. */
public final class GunInputRouter {
    private GunInputRouter() { }

    /** An off-hand modular gun may fire alone, unless the main-hand gun occupies both hands. */
    public static boolean offhandShootAllowed(boolean mainGun, boolean offhandGun,
                                               boolean mainGunTwoHanded) {
        return offhandGun && !mainGunTwoHanded;
    }

    public static boolean adsAllowed(boolean mainGun, boolean offHandPistol, boolean mainGunTwoHanded) {
        return !mainGun || mainGunTwoHanded || !offHandPistol;
    }

    public static Decision resolve(boolean mainGun, boolean offHandPistol,
                                   boolean mainGunTwoHanded, boolean ctrl, boolean shift) {
        GunInputState state = !mainGun ? GunInputState.NO_GUN
                : mainGunTwoHanded ? GunInputState.TWO_HANDED_GUN
                : offHandPistol ? GunInputState.DUAL_PISTOL
                : GunInputState.SINGLE_PISTOL;
        boolean offHandAction = state == GunInputState.DUAL_PISTOL && ctrl;
        boolean fill = shift;
        return new Decision(state, offHandAction, fill,
                state == GunInputState.SINGLE_PISTOL || state == GunInputState.TWO_HANDED_GUN);
    }

    public record Decision(GunInputState state, boolean routeReloadToOffHand,
                           boolean fillReload, boolean mainHandControls) { }
}
