package com.pycoder.taczintetra.logic;

/** 对 TaCZ 虚拟弹药 NBT 状态进行防溢出的规范化。 */
public final class AmmoStatePolicy {
    private AmmoStatePolicy() {
    }

    public static int sanitizeMax(int value, int fallback) {
        return value > 0 ? value : Math.max(0, fallback);
    }

    public static int addSaturated(int current, int delta) {
        long result = (long) Math.max(0, current) + delta;
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, result));
    }
}
