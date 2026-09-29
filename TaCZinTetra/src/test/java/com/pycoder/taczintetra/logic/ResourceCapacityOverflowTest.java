package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceCapacityOverflowTest {
    @Test
    void reserveCapacitySaturatesInsteadOfWrappingNegative() {
        assertEquals(Integer.MAX_VALUE,
                ResourceCapacityCalculator.reserveCapacity(Integer.MAX_VALUE, 1, 1));
        assertEquals(Integer.MAX_VALUE,
                ResourceCapacityCalculator.resourceMax(Integer.MAX_VALUE, 1, 1, 1));
    }
}
