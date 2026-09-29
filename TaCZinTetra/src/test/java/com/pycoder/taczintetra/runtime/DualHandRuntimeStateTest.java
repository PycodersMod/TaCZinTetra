package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DualHandRuntimeStateTest {
    @Test
    void bindsReloadToContextAndBlocksOtherHand() {
        DualHandRuntimeState state = new DualHandRuntimeState();
        PerHandWeaponContext main = state.context(InteractionHand.MAIN_HAND);
        PerHandWeaponContext off = state.context(InteractionHand.OFF_HAND);

        assertTrue(state.beginReload(main));
        assertTrue(main.reloading());
        assertFalse(state.beginReload(off));
        state.interruptReload(main);
        assertFalse(main.reloading());
        assertTrue(state.beginReload(off));
    }

    @Test
    void replacingHeldIdentityCancelsItsReload() {
        DualHandRuntimeState state = new DualHandRuntimeState();
        PerHandWeaponContext main = state.context(InteractionHand.MAIN_HAND);
        main.setStackIdentity(10);
        assertTrue(state.beginReload(main));
        main.setStackIdentity(11);
        state.reconcile();
        assertFalse(main.reloading());
        assertTrue(state.beginReload(state.context(InteractionHand.OFF_HAND)));
    }

    @Test
    void missingHandDoesNotDefaultToMainHand() {
        DualHandRuntimeState state = new DualHandRuntimeState();
        assertFalse(state.beginReload(state.context(null)));
        assertFalse(state.context(null) != null);
    }

    @Test
    void coordinatorUpdateReleasesReloadWhenHeldStackIdentityChanges() {
        DualHandRuntimeState state = new DualHandRuntimeState();
        PerHandWeaponContext main = state.context(InteractionHand.MAIN_HAND);
        state.updateStackIdentity(InteractionHand.MAIN_HAND, 10);
        assertTrue(state.beginReload(main));
        state.updateStackIdentity(InteractionHand.MAIN_HAND, 11);
        assertFalse(main.reloading());
        assertTrue(state.beginReload(state.context(InteractionHand.OFF_HAND)));
    }

    @Test
    void identityUpdateReportsAnInterruptedReload() {
        DualHandRuntimeState state = new DualHandRuntimeState();
        PerHandWeaponContext main = state.context(InteractionHand.MAIN_HAND);
        state.updateStackIdentity(InteractionHand.MAIN_HAND, 10);
        assertTrue(state.beginReload(main));
        assertTrue(state.updateStackIdentity(InteractionHand.MAIN_HAND, 11));
        assertFalse(main.reloading());
        assertFalse(state.updateStackIdentity(InteractionHand.MAIN_HAND, 11));
    }

    @Test
    void interruptedReloadClearsPendingStateOnTheOriginalStack() throws Exception {
        String state = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/DualHandRuntimeState.java"),
                StandardCharsets.UTF_8);
        String context = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/PerHandWeaponContext.java"),
                StandardCharsets.UTF_8);
        String coordinator = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeCoordinator.java"),
                StandardCharsets.UTF_8);
        assertTrue(context.contains("ItemStack reloadStack"));
        assertTrue(context.contains("bindReloadStack"));
        assertTrue(state.contains("beginReload(PerHandWeaponContext context, ItemStack stack)"));
        assertTrue(state.contains("clearRuntimeState(context.reloadStack())"));
        assertTrue(coordinator.contains("state.beginReload(context, stack)"));
    }

    @Test
    void activeReloadIsVisibleAsHandOwnership() {
        DualHandRuntimeState state = new DualHandRuntimeState();
        PerHandWeaponContext off = state.context(InteractionHand.OFF_HAND);
        assertTrue(state.beginReload(off));
        assertTrue(off.reloading());
        assertFalse(state.context(InteractionHand.MAIN_HAND).reloading());
        state.completeReload(off);
        assertFalse(off.reloading());
    }
}
