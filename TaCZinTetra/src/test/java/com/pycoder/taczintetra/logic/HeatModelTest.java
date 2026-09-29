package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatModelTest {
    @Test
    void coolsUsingNewtonModel() {
        double result = HeatModel.cool(100, 20, 0.5, 2, 0.001);
        assertEquals(49.43, result, 0.01);
    }

    @Test
    void epsilonSnapsToAmbient() {
        assertEquals(20, HeatModel.cool(20.0001, 20, 1, 1, 0.001));
    }

    @Test
    void overheatLocksAtThresholdAndUnlocksNearAmbient() {
        assertTrue(HeatModel.isOverheated(100, 100, 0.001));
        assertTrue(HeatModel.canUnlock(20.01, 20, 0.02));
    }

    @Test
    void invalidCoolingInputsNeverProduceNan() {
        assertEquals(20, HeatModel.cool(Double.NaN, 20, 1, 1, 0.001));
        assertEquals(100, HeatModel.cool(100, 20, Double.NaN, 1, 0.001));
        assertEquals(36.79, HeatModel.cool(100, Double.NaN, 1, 1, 0.001), 0.01);
    }

    @Test
    void invalidThresholdDoesNotLockAndInvalidEpsilonUsesZero() {
        assertTrue(!HeatModel.isOverheated(100, Double.NEGATIVE_INFINITY, 0.1));
        assertTrue(HeatModel.isOverheated(100, 100, Double.NaN));
    }
}
