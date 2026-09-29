package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalResourceTransactionPolicyTest {
    @Test
    void rejectsMultipleExternalDebitsWhenRollbackIsUnavailable() {
        assertFalse(ExternalResourceTransactionPolicy.canBegin(2, false));
        assertTrue(ExternalResourceTransactionPolicy.canBegin(2, true));
        assertTrue(ExternalResourceTransactionPolicy.canBegin(1, false));
    }
}
