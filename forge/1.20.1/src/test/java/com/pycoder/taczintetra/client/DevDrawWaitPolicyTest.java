package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevDrawWaitPolicyTest {
    @Test
    void drawProbeWaitIsBoundedAndReportsAtStartAndAtIntervals() {
        assertFalse(DevDrawWaitPolicy.timedOut(DevDrawWaitPolicy.MAX_WAIT_TICKS - 1));
        assertTrue(DevDrawWaitPolicy.timedOut(DevDrawWaitPolicy.MAX_WAIT_TICKS));
        assertTrue(DevDrawWaitPolicy.shouldLog(1));
        assertTrue(DevDrawWaitPolicy.shouldLog(DevDrawWaitPolicy.LOG_INTERVAL_TICKS));
        assertFalse(DevDrawWaitPolicy.shouldLog(DevDrawWaitPolicy.LOG_INTERVAL_TICKS + 1));
    }
}
