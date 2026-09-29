package com.pycoder.taczintetra.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadIntentValidatorTest {
    @Test
    void acceptsPositiveSequenceAndNonNegativeIdentity() {
        assertTrue(ReloadIntentValidator.isValid(new ReloadIntentMessage(false, true, 0, 1)));
    }

    @Test
    void rejectsMalformedTransportValues() {
        assertFalse(ReloadIntentValidator.isValid(null));
        assertFalse(ReloadIntentValidator.isValid(new ReloadIntentMessage(false, false, -1, 1)));
        assertFalse(ReloadIntentValidator.isValid(new ReloadIntentMessage(false, false, 1, 0)));
    }
}
