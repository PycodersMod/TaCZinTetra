package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpreadPolicyTest {
    @Test
    void combinesNativePostureSpreadWithFiniteModuleAdjustment() {
        assertEquals(2.75f, SpreadPolicy.total(2.5f, 0.25f), 0.0001f);
        assertEquals(2.5f, SpreadPolicy.total(2.5f, Float.NaN), 0.0001f);
        assertEquals(0f, SpreadPolicy.total(Float.POSITIVE_INFINITY, -1f), 0.0001f);
    }
}
