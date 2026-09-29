package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatCurveTest {
    @Test
    void defaultCurveUsesLinearInterpolation() {
        HeatCurve curve = HeatCurve.linear(1.0, 1.2);
        assertEquals(1.1, curve.valueAt(0.5), 0.0001);
    }

    @Test
    void customCurveInterpolatesBetweenSortedPointsAndClampsEnds() {
        HeatCurve curve = new HeatCurve(List.of(
                new HeatCurve.Point(0.0, 1.0),
                new HeatCurve.Point(0.25, 1.05),
                new HeatCurve.Point(1.0, 1.8)));
        assertEquals(1.0, curve.valueAt(-1), 0.0001);
        assertEquals(1.3, curve.valueAt(0.5), 0.0001);
        assertEquals(1.8, curve.valueAt(2), 0.0001);
    }
}
