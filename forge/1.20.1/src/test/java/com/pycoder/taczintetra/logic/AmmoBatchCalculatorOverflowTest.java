package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmmoBatchCalculatorOverflowTest {
    @Test
    void largeBatchSizeDoesNotOverflowPlanningArithmetic() {
        AmmoBatchCalculator.Result result = AmmoBatchCalculator.calculate(
                Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 2, true);

        assertEquals(1, result.paidBatches());
        assertEquals(Integer.MAX_VALUE, result.loadedRounds());
    }

    @Test
    void largeAffordableBatchCountStillClampsToRemainingCapacity() {
        AmmoBatchCalculator.Result result = AmmoBatchCalculator.calculate(
                Integer.MAX_VALUE, 0, 1_500_000_000, Integer.MAX_VALUE, true);

        assertEquals(2, result.paidBatches());
        assertEquals(Integer.MAX_VALUE, result.loadedRounds());
    }
}
