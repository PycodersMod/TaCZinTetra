package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.runtime.PerHandWeaponContext;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

    class PerHandWeaponContextTest {
    @Test void handStateIsIndependentAndSanitized() {
        PerHandWeaponContext main = new PerHandWeaponContext(InteractionHand.MAIN_HAND);
        PerHandWeaponContext off = new PerHandWeaponContext(InteractionHand.OFF_HAND);
        main.setAmmo(-1, 8);
        main.setHeat(Float.NaN);
        main.setReloading(true);
        assertEquals(0, main.currentAmmo());
        assertEquals(0, main.heat());
        assertEquals(1, main.reloadSequence());
        assertEquals(InteractionHand.OFF_HAND, off.hand());
        assertEquals(0, off.reserveAmmo());
    }

    @Test void repeatedReloadingStateDoesNotCreateASecondSequence() {
        PerHandWeaponContext context = new PerHandWeaponContext(InteractionHand.MAIN_HAND);
        context.setReloading(true);
        context.setReloading(true);
        assertEquals(1, context.reloadSequence());
    }

    @Test void combatStateDoesNotCrossHands() {
        PerHandWeaponContext main = new PerHandWeaponContext(InteractionHand.MAIN_HAND);
        PerHandWeaponContext off = new PerHandWeaponContext(InteractionHand.OFF_HAND);
        main.setBoltOpen(true);
        main.setCooldownTicks(4);
        main.setRecoil(0.75f);
        main.setFireMode("burst");
        main.setAnimationState("shoot");

        assertTrue(main.boltOpen());
        assertEquals(4, main.cooldownTicks());
        assertEquals(0.75f, main.recoil());
        assertEquals("burst", main.fireMode());
        assertEquals("shoot", main.animationState());
        assertFalse(off.boltOpen());
        assertEquals(0, off.cooldownTicks());
        assertEquals(0, off.recoil());
        assertEquals("", off.fireMode());
        assertEquals("", off.animationState());
    }
}
