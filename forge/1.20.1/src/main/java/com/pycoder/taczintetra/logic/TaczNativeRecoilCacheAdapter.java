package com.pycoder.taczintetra.logic;

import com.tacz.guns.api.modifier.ParameterizedCachePair;

/** 将 Tetra 后坐力倍率接入 TaCZ 的原生参数化曲线缓存。 */
public final class TaczNativeRecoilCacheAdapter {
    private TaczNativeRecoilCacheAdapter() { }

    public static ParameterizedCachePair<Float, Float> scale(ParameterizedCachePair<Float, Float> base,
                                                               double multiplier) {
        if (base == null || !Double.isFinite(multiplier) || multiplier < 0
                || !Float.isFinite((float) multiplier)) return base;
        Float pitch = base.left().getDefaultValue();
        Float yaw = base.right().getDefaultValue();
        if (pitch == null || yaw == null || !Float.isFinite(pitch) || !Float.isFinite(yaw)) return base;
        float factor = (float) multiplier;
        float scaledPitch = pitch * factor;
        float scaledYaw = yaw * factor;
        if (!Float.isFinite(scaledPitch) || !Float.isFinite(scaledYaw)) return base;
        return ParameterizedCachePair.of(scaledPitch, scaledYaw);
    }
}
