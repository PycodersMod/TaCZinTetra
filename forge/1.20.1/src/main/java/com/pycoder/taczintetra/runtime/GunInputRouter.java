package com.pycoder.taczintetra.runtime;

/** 纯输入路由策略；实际按键绑定由客户端适配器提供。 */
public final class GunInputRouter {
    private GunInputRouter() { }

    /** 仅双手各持一把单手模组枪时，右键才用于副手射击。 */
    public static boolean offhandShootAllowed(boolean mainGun, boolean offhandGun,
                                               boolean mainGunTwoHanded) {
        return mainGun && offhandGun && !mainGunTwoHanded;
    }

    /** 普通主手换弹交给 TaCZ 原生 ReloadKey；仅副手和 Shift 补弹走自定义意图。 */
    public static boolean shouldRouteReloadToCustomHandler(Decision decision) {
        return decision != null && (decision.routeReloadToOffHand() || decision.fillReload());
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
                state != GunInputState.NO_GUN);
    }

    public record Decision(GunInputState state, boolean routeReloadToOffHand,
                           boolean fillReload, boolean mainHandControls) { }
}
