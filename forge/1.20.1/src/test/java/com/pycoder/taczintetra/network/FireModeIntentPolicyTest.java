package com.pycoder.taczintetra.network;

import com.tacz.guns.api.item.gun.FireMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireModeIntentPolicyTest {
    @Test
    void validatesGunIdentityAndReloadMutualExclusion() {
        assertTrue(FireModeIntentPolicy.canAccept(12, 12, true, false));
        assertFalse(FireModeIntentPolicy.canAccept(12, 13, true, false));
        assertFalse(FireModeIntentPolicy.canAccept(12, 12, false, false));
        assertFalse(FireModeIntentPolicy.canAccept(12, 12, true, true));
    }

    @Test
    void cyclesSupportedModesWithoutUnknown() {
        assertEquals(FireMode.BURST, FireModeIntentPolicy.next(FireMode.SEMI));
        assertEquals(FireMode.AUTO, FireModeIntentPolicy.next(FireMode.BURST));
        assertEquals(FireMode.SEMI, FireModeIntentPolicy.next(FireMode.AUTO));
        assertEquals(FireMode.SEMI, FireModeIntentPolicy.next(FireMode.UNKNOWN));
    }
}
