package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadSequenceTest {
    @Test
    void sequenceWrapsToPositiveOne() {
        assertEquals(1, ReloadSequence.next(Integer.MAX_VALUE));
    }

    @Test
    void ordinarySequenceIncrements() {
        assertEquals(8, ReloadSequence.next(7));
    }

    @Test
    void sharedSequenceKeepsManualAndAutomatedReloadRequestsMonotonic() {
        int first = ReloadSequence.next();
        int second = ReloadSequence.next();

        assertEquals(ReloadSequence.next(first), second);
    }
}
