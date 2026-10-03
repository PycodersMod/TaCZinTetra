package com.pycoder.taczintetra.logic;

/** 复现模组射击前必须通过的 TaCZ 状态门禁。 */
public final class ShootStatePolicy {
    private ShootStatePolicy() {
    }

    public static boolean allowed(boolean reloading, boolean bolting, float sprintTime) {
        return !reloading && !bolting && Float.isFinite(sprintTime) && sprintTime <= 0.0f;
    }
}
