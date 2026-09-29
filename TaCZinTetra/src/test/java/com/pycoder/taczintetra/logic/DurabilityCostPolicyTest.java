package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurabilityCostPolicyTest {
    @Test
    void pelletCountDoesNotMultiplySingleShotCost() {
        assertEquals(1, DurabilityCostPolicy.cost(1, 12, 1.0));
        assertEquals(3, DurabilityCostPolicy.cost(3, 12, 1.0));
    }

    @Test
    void appliesConfiguredMultiplierAndMinimum() {
        assertEquals(2, DurabilityCostPolicy.cost(1, 1, 1.5));
        assertEquals(0, DurabilityCostPolicy.cost(0, 0, 0));
    }
}
