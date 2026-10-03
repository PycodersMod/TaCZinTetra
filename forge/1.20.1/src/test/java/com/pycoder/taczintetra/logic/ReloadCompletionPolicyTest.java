package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadCompletionPolicyTest {
    @Test
    void interruptionDoesNotCommitPaymentOrAmmo() {
        ReloadCompletionPolicy.Result result = ReloadCompletionPolicy.commit(false, 7, 3, 2);
        assertEquals(7, result.resourceAmount());
        assertEquals(3, result.loadedAmmo());
        assertEquals(0, result.paidBatches());
    }

    @Test
    void completionCommitsBothChanges() {
        ReloadCompletionPolicy.Result result = ReloadCompletionPolicy.commit(true, 7, 3, 2);
        assertEquals(5, result.resourceAmount());
        assertEquals(5, result.loadedAmmo());
        assertEquals(2, result.paidBatches());
    }

    @Test
    void completionClampsNegativeResourcesAndSaturatesAmmo() {
        ReloadCompletionPolicy.Result negative = ReloadCompletionPolicy.commit(true,
                Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
        assertEquals(0, negative.resourceAmount());
        assertEquals(Integer.MAX_VALUE, negative.loadedAmmo());
        assertEquals(Integer.MAX_VALUE, negative.paidBatches());

        ReloadCompletionPolicy.Result overflow = ReloadCompletionPolicy.commit(true,
                Integer.MAX_VALUE, Integer.MAX_VALUE, 1);
        assertEquals(Integer.MAX_VALUE, overflow.loadedAmmo());
    }
}
