package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceCapacityCalculatorTest {
    @Test
    void appliesCapacityHoningBeforeMaterialMultiplier() {
        assertEquals(30, ResourceCapacityCalculator.reserveCapacity(20, 5, 5));
        assertEquals(45, ResourceCapacityCalculator.resourceMax(20, 5, 5, 1.5));
    }

    @Test
    void clampsInvalidValuesToSafeMinimums() {
        assertEquals(0, ResourceCapacityCalculator.reserveCapacity(-1, -2, -3));
        assertEquals(1, ResourceCapacityCalculator.resourceMax(0, 0, 0, -1));
    }

    @Test
    void computesLoadedCapacityFromReserveBaseAndBodyMultiplier() {
        assertEquals(15, ResourceCapacityCalculator.loadedAmmoCapacity(10, 1.5));
        assertEquals(0, ResourceCapacityCalculator.loadedAmmoCapacity(-4, 2));
        assertEquals(10, ResourceCapacityCalculator.loadedAmmoCapacity(10, 0));
    }
}
