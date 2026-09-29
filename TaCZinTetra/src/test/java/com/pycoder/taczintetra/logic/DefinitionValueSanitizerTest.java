package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionValueSanitizerTest {
    @Test void integerUsesHalfUpAndMinimum() {
        assertEquals(0, DefinitionValueSanitizer.integer(-4.5, 0));
        assertEquals(1, DefinitionValueSanitizer.integer(0.5, 1));
        assertEquals(3, DefinitionValueSanitizer.integer(2.5, 0));
    }

    @Test void continuousClampsAndRoundsToTwoDecimals() {
        assertEquals(0.01, DefinitionValueSanitizer.continuous(0, 0.01));
        assertEquals(1.24, DefinitionValueSanitizer.continuous(1.235, 0));
        assertEquals(0, DefinitionValueSanitizer.continuous(Double.NaN, 0));
    }
}
