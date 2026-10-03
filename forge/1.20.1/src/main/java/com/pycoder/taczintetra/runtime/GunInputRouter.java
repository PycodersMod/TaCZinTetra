package com.pycoder.taczintetra.runtime;

/** 纯输入路由策略；实际按键绑定由客户端适配器提供。 */
public final class GunInputRouter {
    private GunInputRouter() { }

    /** 副手模组枪械可以单独射击，除非主手枪械占用双手。 */
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
