package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadSettlementCapacityTest {
    @Test
    void settlementClampsPersistedAmmoToCurrentCapacity() {
        AmmoRecipeDefinition recipe = new AmmoRecipeDefinition("tacz:9mm", 1, java.util.Map.of());

        ReloadSettlementPolicy.Result result = ReloadSettlementPolicy.settle(
                true, 5, 12, recipe, ResourceAccount.empty(), true);

        assertEquals(5, result.loadedRounds());
        assertEquals(0, result.paidBatches());
    }
}
