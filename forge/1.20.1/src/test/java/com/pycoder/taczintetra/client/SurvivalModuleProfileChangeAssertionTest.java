package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalModuleProfileChangeAssertionTest {
    @Test
    void pistolToRifleChangeMustBeVisibleInRuntimeStats() {
        var result = SurvivalModuleProfileChangeAssertion.evaluate(
                new SurvivalModuleProfileChangeAssertion.Snapshot(400, 5.0, 1.0, false),
                new SurvivalModuleProfileChangeAssertion.Snapshot(600, 5.25, 1.3, true));

        assertTrue(result.passed());
        assertEquals("runtime_profile_updated", result.reason());
    }

    @Test
    void unchangedStatsCannotPassJustBecauseModuleNbtChanged() {
        var before = new SurvivalModuleProfileChangeAssertion.Snapshot(400, 5.0, 1.0, false);
        var result = SurvivalModuleProfileChangeAssertion.evaluate(before,
                new SurvivalModuleProfileChangeAssertion.Snapshot(400, 5.0, 1.0, false));

        assertFalse(result.passed());
        assertEquals("runtime_profile_did_not_reflect_rifle", result.reason());
    }

    @Test
    void missingSnapshotsFailClosed() {
        var result = SurvivalModuleProfileChangeAssertion.evaluate(null,
                new SurvivalModuleProfileChangeAssertion.Snapshot(600, 5.25, 1.3, true));

        assertFalse(result.passed());
        assertEquals("profile_snapshot_missing", result.reason());
    }
}
