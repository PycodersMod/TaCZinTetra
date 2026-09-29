package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmmoBatchCalculatorTest {
    @Test
    void singleBatchLoadsOnlyOneBatch() {
        AmmoBatchCalculator.Result result = AmmoBatchCalculator.calculate(5, 2, 3, 1, false);
        assertEquals(1, result.paidBatches());
        assertEquals(3, result.loadedRounds());
    }

    @Test
    void fillModePaysOnlyAffordableCompleteBatches() {
        AmmoBatchCalculator.Result result = AmmoBatchCalculator.calculate(10, 1, 5, 2, true);
        assertEquals(2, result.paidBatches());
        assertEquals(9, result.loadedRounds());
    }

    @Test
    void fullMagazineAndInvalidValuesAreSafe() {
        assertEquals(0, AmmoBatchCalculator.calculate(0, 5, 3, 9, true).paidBatches());
        assertEquals(0, AmmoBatchCalculator.calculate(5, 5, 3, 9, true).paidBatches());
        assertEquals(0, AmmoBatchCalculator.calculate(5, 2, 0, 9, true).paidBatches());
    }

    @Test
    void lastPaidBatchMayHaveUnusedTheoreticalCapacity() {
        AmmoBatchCalculator.Result result = AmmoBatchCalculator.calculate(10, 7, 4, 1, true);
        assertEquals(1, result.paidBatches());
        assertEquals(3, result.loadedRounds());
    }
}
