package com.pycoder.taczintetra.logic;

/** Clamps Minecraft-style item damage without deciding Tetra broken semantics. */
public final class DurabilityStateRevalidator {
    private DurabilityStateRevalidator() {
    }

    public static State revalidate(int maxDurability, int currentDamage) {
        int max = Math.max(0, maxDurability);
        return new State(max, Math.max(0, Math.min(currentDamage, max)));
    }

    public record State(int maxDurability, int currentDamage) {
    }
}
