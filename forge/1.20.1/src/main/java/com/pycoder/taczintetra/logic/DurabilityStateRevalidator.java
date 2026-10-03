package com.pycoder.taczintetra.logic;

/** 将物品损伤值限制在 Minecraft 规则范围内，不在此处判定 Tetra 的损坏语义。 */
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
