package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** 每只手各自拥有的可变运行时状态；主手与副手之间不共享状态。 */
public final class PerHandWeaponContext {
    private final InteractionHand hand;
    private int stackIdentity = -1;
    private int currentAmmo;
    private int reserveAmmo;
    private float heat;
    private boolean reloading;
    private long reloadSequence;
    private boolean boltOpen;
    private int cooldownTicks;
    private float recoil;
    private String fireMode = "";
    private String animationState = "";
    private ItemStack reloadStack;

    public PerHandWeaponContext(InteractionHand hand) {
        this.hand = hand;
    }

    public InteractionHand hand() { return hand; }
    public int stackIdentity() { return stackIdentity; }
    public int currentAmmo() { return currentAmmo; }
    public int reserveAmmo() { return reserveAmmo; }
    public float heat() { return heat; }
    public boolean reloading() { return reloading; }
    public long reloadSequence() { return reloadSequence; }
    public boolean boltOpen() { return boltOpen; }
    public int cooldownTicks() { return cooldownTicks; }
    public float recoil() { return recoil; }
    public String fireMode() { return fireMode; }
    public String animationState() { return animationState; }
    public ItemStack reloadStack() { return reloadStack; }

    public void bindReloadStack(ItemStack stack) { reloadStack = stack == null || stack.isEmpty() ? null : stack; }

    public void clearReloadStack() { reloadStack = null; }

    public void setAmmo(int currentAmmo, int reserveAmmo) {
        this.currentAmmo = Math.max(0, currentAmmo);
        this.reserveAmmo = Math.max(0, reserveAmmo);
    }
    public boolean matchesStackIdentity(int identity) { return stackIdentity >= 0 && stackIdentity == identity; }
    public void setStackIdentity(int identity) {
        if (stackIdentity == identity) {
            return;
        }
        stackIdentity = identity;
        setReloading(false);
    }

    public void setHeat(float heat) { this.heat = Float.isFinite(heat) ? Math.max(0, heat) : 0; }
    public void setBoltOpen(boolean boltOpen) { this.boltOpen = boltOpen; }
    public void setCooldownTicks(int cooldownTicks) { this.cooldownTicks = Math.max(0, cooldownTicks); }
    public void setRecoil(float recoil) { this.recoil = Float.isFinite(recoil) ? Math.max(0, recoil) : 0; }
    public void setFireMode(String fireMode) { this.fireMode = fireMode == null ? "" : fireMode; }
    public void setAnimationState(String animationState) {
        this.animationState = animationState == null ? "" : animationState;
    }
    public void setReloading(boolean reloading) {
        if (this.reloading == reloading) {
            return;
        }
        this.reloading = reloading;
        if (reloading) reloadSequence++;
    }
}
