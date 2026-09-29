package com.pycoder.taczintetra.logic;

import com.tacz.guns.api.item.gun.FireMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireModePolicyTest {
    @Test
    void onlyModesExposedByNativeGunDataAreAccepted() {
        List<FireMode> allowed = List.of(FireMode.SEMI);

        assertTrue(FireModePolicy.isAllowed(FireMode.SEMI, allowed));
        assertFalse(FireModePolicy.isAllowed(FireMode.BURST, allowed));
        assertFalse(FireModePolicy.isAllowed(FireMode.AUTO, allowed));
    }

    @Test
    void cyclingUsesTheNativeAllowedModeListInsteadOfGlobalModeCycle() {
        assertEquals(FireMode.SEMI,
                FireModePolicy.next(FireMode.SEMI, List.of(FireMode.SEMI)));
        assertEquals(FireMode.AUTO,
                FireModePolicy.next(FireMode.SEMI, List.of(FireMode.SEMI, FireMode.AUTO)));
        assertEquals(FireMode.SEMI,
                FireModePolicy.next(FireMode.AUTO, List.of(FireMode.SEMI, FireMode.AUTO)));
    }
}
