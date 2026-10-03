package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DualHandReloadStateTest {
    @Test
    void onlyOneHandCanReloadAtATime() {
        DualHandReloadState state = new DualHandReloadState();
        assertTrue(state.begin(InteractionHand.MAIN_HAND));
        assertFalse(state.begin(InteractionHand.OFF_HAND));
        assertTrue(state.isReloading(InteractionHand.MAIN_HAND));
        state.complete(InteractionHand.MAIN_HAND);
        assertTrue(state.begin(InteractionHand.OFF_HAND));
    }

    @Test
    void interruptionReleasesTheLock() {
        DualHandReloadState state = new DualHandReloadState();
        assertTrue(state.begin(InteractionHand.OFF_HAND));
        state.interrupt(InteractionHand.OFF_HAND);
        assertFalse(state.isReloading(InteractionHand.OFF_HAND));
        assertTrue(state.begin(InteractionHand.MAIN_HAND));
    }
}
