package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurabilityStateRevalidatorTest {
    @Test
    void clampsDamageToDurabilityBounds() {
        assertEquals(new DurabilityStateRevalidator.State(100, 100),
                DurabilityStateRevalidator.revalidate(100, 140));
        assertEquals(new DurabilityStateRevalidator.State(100, 0),
                DurabilityStateRevalidator.revalidate(100, -2));
    }

    @Test
    void negativeMaximumBecomesSafeZero() {
        assertEquals(new DurabilityStateRevalidator.State(0, 0),
                DurabilityStateRevalidator.revalidate(-1, 3));
    }
}
