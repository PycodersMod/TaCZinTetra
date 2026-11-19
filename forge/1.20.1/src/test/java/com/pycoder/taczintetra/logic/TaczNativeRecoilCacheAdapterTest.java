package com.pycoder.taczintetra.logic;

import com.tacz.guns.api.modifier.ParameterizedCachePair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaczNativeRecoilCacheAdapterTest {
    @Test
    void scalesTaCZPitchAndYawCurveParametersFromTheCurrentTetraProfile() {
        var base = ParameterizedCachePair.of(1.0f, 0.75f);

        var scaled = TaczNativeRecoilCacheAdapter.scale(base, 1.6);

        assertEquals(1.6f, scaled.left().getDefaultValue(), 0.0001f);
        assertEquals(1.2f, scaled.right().getDefaultValue(), 0.0001f);
    }

    @Test
    void invalidMultipliersLeaveTheNativeTaCZCacheUnchanged() {
        var base = ParameterizedCachePair.of(1.0f, 0.75f);

        assertEquals(base, TaczNativeRecoilCacheAdapter.scale(base, Double.NaN));
        assertEquals(base, TaczNativeRecoilCacheAdapter.scale(base, -1));
    }

    @Test
    void zeroMultiplierDisablesNativeCameraRecoil() {
        var scaled = TaczNativeRecoilCacheAdapter.scale(ParameterizedCachePair.of(1.0f, 0.75f), 0);

        assertEquals(0.0f, scaled.left().getDefaultValue(), 0.0001f);
        assertEquals(0.0f, scaled.right().getDefaultValue(), 0.0001f);
    }
}
