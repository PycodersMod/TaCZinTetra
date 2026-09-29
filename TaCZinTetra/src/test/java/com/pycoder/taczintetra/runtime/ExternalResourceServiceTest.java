package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalResourceServiceTest {
    @Test
    void acceptsOnlyAnExactPositiveExtraction() {
        assertTrue(ExternalResourceService.isExactExtraction(2, 2));
        assertFalse(ExternalResourceService.isExactExtraction(2, 1));
        assertFalse(ExternalResourceService.isExactExtraction(2, 3));
        assertFalse(ExternalResourceService.isExactExtraction(0, 0));
        assertFalse(ExternalResourceService.isExactExtraction(-1, -1));
    }
}
