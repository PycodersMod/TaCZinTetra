package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.runtime.GunInputRouter;
import com.pycoder.taczintetra.runtime.GunInputState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GunInputRouterTest {
    @Test void mainHandHasPriorityAndCtrlRoutesDualReload() {
        assertEquals(GunInputState.NO_GUN, GunInputRouter.resolve(false, true, false, true, false).state());
        GunInputRouter.Decision dual = GunInputRouter.resolve(true, true, false, true, true);
        assertEquals(GunInputState.DUAL_PISTOL, dual.state());
        assertTrue(dual.routeReloadToOffHand());
        assertTrue(dual.fillReload());
        assertFalse(GunInputRouter.resolve(true, true, true, true, false).routeReloadToOffHand());
    }

    @Test void disablesAdsOnlyForDualPistols() {
        assertFalse(GunInputRouter.adsAllowed(true, true, false));
        assertTrue(GunInputRouter.adsAllowed(true, false, false));
        assertTrue(GunInputRouter.adsAllowed(true, true, true));
        assertTrue(GunInputRouter.adsAllowed(false, true, false));
    }

    @Test void allowsOffhandGunToShootWithoutMainhandGun() {
        assertTrue(GunInputRouter.offhandShootAllowed(false, true, false));
        assertTrue(GunInputRouter.offhandShootAllowed(true, true, false));
        assertFalse(GunInputRouter.offhandShootAllowed(true, true, true));
        assertFalse(GunInputRouter.offhandShootAllowed(false, false, false));
    }
}
