package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatEffectPolicyTest {
    @Test
    void customCurveOverridesLinearHeatMultiplier() {
        HeatCurve curve = new HeatCurve(java.util.List.of(
                new HeatCurve.Point(0, 1), new HeatCurve.Point(0.5, 0.6), new HeatCurve.Point(1, 0.2)));
        assertEquals(0.6, HeatEffectPolicy.curveMultiplier(50, 100, curve, 1, 0.2), 0.001);
    }

    @Test
    void heatRaisesRpmAndInaccuracyWithinConfiguredBounds() {
        assertEquals(1.0, HeatEffectPolicy.rpmMultiplier(0, 100, 1.0, 1.2), 0.001);
        assertEquals(1.1, HeatEffectPolicy.rpmMultiplier(50, 100, 1.0, 1.2), 0.001);
        assertEquals(1.2, HeatEffectPolicy.rpmMultiplier(100, 100, 1.0, 1.2), 0.001);
        assertEquals(2.0, HeatEffectPolicy.inaccuracyMultiplier(100, 100, 1.0, 2.0), 0.001);
    }

    @Test
    void invalidThresholdAndColdMultiplierNeverProduceNan() {
        assertEquals(1.0, HeatEffectPolicy.rpmMultiplier(10, 0, Double.NaN, 2), 0.001);
        assertEquals(1.0, HeatEffectPolicy.inaccuracyMultiplier(10, Double.NaN, Double.NaN, 2), 0.001);
    }

    @Test
    void coolingCoefficientMultipliesBarrelMaterialAndHoningFactors() {
        assertEquals(6.6, HeatEffectPolicy.coolingCoefficient(4, 1.5, 0.1), 0.001);
    }
}
