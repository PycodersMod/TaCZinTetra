package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerHandWeaponContextIdentityTest {
    @Test
    void changingTheBoundStackInterruptsReload() {
        PerHandWeaponContext context = new PerHandWeaponContext(InteractionHand.MAIN_HAND);
        context.setStackIdentity(12);
        context.setReloading(true);

        assertTrue(context.matchesStackIdentity(12));
        assertFalse(context.matchesStackIdentity(13));
        context.setStackIdentity(13);
        assertFalse(context.reloading());
    }
}
