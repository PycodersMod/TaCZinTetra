package com.pycoder.taczintetra.logic;

/** 在客户端瞄准值进入 TaCZ 弹丸代码前拒绝格式错误的值。 */
public final class AimInputPolicy {
    private AimInputPolicy() {
    }

    public static boolean valid(float pitch, float yaw) {
        return Float.isFinite(pitch) && Float.isFinite(yaw);
    }
}
