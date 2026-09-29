package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Owns both hand contexts and coordinates reload ownership. */
public final class DualHandRuntimeState {
    private final PerHandWeaponContext mainHand = new PerHandWeaponContext(InteractionHand.MAIN_HAND);
    private final PerHandWeaponContext offHand = new PerHandWeaponContext(InteractionHand.OFF_HAND);
    private final DualHandReloadState reloadState = new DualHandReloadState();

    public PerHandWeaponContext context(InteractionHand hand) {
        if (hand == null) {
            return null;
        }
        return hand == InteractionHand.OFF_HAND ? offHand : mainHand;
    }

    public boolean beginReload(PerHandWeaponContext context) {
        return beginReload(context, null);
    }

    public boolean beginReload(PerHandWeaponContext context, ItemStack stack) {
        if (context == null || context != context(context.hand())) {
            return false;
        }
        if (!reloadState.begin(context.hand())) {
            return false;
        }
        context.bindReloadStack(stack);
        context.setReloading(true);
        return true;
    }

    public boolean updateStackIdentity(InteractionHand hand, int identity) {
        PerHandWeaponContext context = context(hand);
        if (context == null) return false;
        boolean interrupted = false;
        if (context.reloading() && context.stackIdentity() != identity) {
            release(context, false);
            interrupted = true;
        }
        context.setStackIdentity(identity);
        return interrupted;
    }

    public void completeReload(PerHandWeaponContext context) {
        release(context, true);
    }

    public void interruptReload(PerHandWeaponContext context) {
        release(context, false);
    }

    public void reconcile() {
        if (reloadState.isReloading(InteractionHand.MAIN_HAND) && !mainHand.reloading()) {
            reloadState.interrupt(InteractionHand.MAIN_HAND);
        }
        if (reloadState.isReloading(InteractionHand.OFF_HAND) && !offHand.reloading()) {
            reloadState.interrupt(InteractionHand.OFF_HAND);
        }
    }

    private void release(PerHandWeaponContext context, boolean complete) {
        if (context == null || !reloadState.isReloading(context.hand())) {
            return;
        }
        if (complete) {
            reloadState.complete(context.hand());
        } else {
            reloadState.interrupt(context.hand());
            ReloadRuntimeSettlementService.clearRuntimeState(context.reloadStack());
        }
        context.setReloading(false);
        context.clearReloadStack();
    }
}
