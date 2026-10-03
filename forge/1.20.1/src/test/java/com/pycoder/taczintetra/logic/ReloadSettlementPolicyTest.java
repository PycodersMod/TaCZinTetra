package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadSettlementPolicyTest {
    @Test
    void interruptionDoesNotConsumeAnyResource() {
        AmmoRecipeDefinition recipe = new AmmoRecipeDefinition("tacz:9mm", "magazine", 4,
                Map.of("shell", 2, "powder", 1));
        ResourceAccount resources = ResourceAccount.empty()
                .insert("shell", 8, 8).account()
                .insert("powder", 4, 4).account();

        ReloadSettlementPolicy.Result result = ReloadSettlementPolicy.settle(
                false, 12, 0, recipe, resources, true);

        assertEquals(0, result.loadedRounds());
        assertEquals(8, result.resources().amount("shell"));
        assertEquals(4, result.resources().amount("powder"));
        assertEquals(0, result.paidBatches());
    }

    @Test
    void completedFillConsumesOnlyAffordableCompleteBatches() {
        AmmoRecipeDefinition recipe = new AmmoRecipeDefinition("tacz:9mm", "magazine", 4,
                Map.of("shell", 2, "powder", 1));
        ResourceAccount resources = ResourceAccount.empty()
                .insert("shell", 4, 4).account()
                .insert("powder", 4, 4).account();

        ReloadSettlementPolicy.Result result = ReloadSettlementPolicy.settle(
                true, 20, 2, recipe, resources, true);

        assertEquals(10, result.loadedRounds());
        assertEquals(2, result.paidBatches());
        assertEquals(0, result.resources().amount("shell"));
        assertEquals(2, result.resources().amount("powder"));
    }

    @Test
    void completedSingleBatchLoadsAtMostTheRemainingCapacity() {
        AmmoRecipeDefinition recipe = new AmmoRecipeDefinition("tacz:9mm", "magazine", 8,
                Map.of("shell", 1));
        ResourceAccount resources = ResourceAccount.empty().insert("shell", 1, 1).account();

        ReloadSettlementPolicy.Result result = ReloadSettlementPolicy.settle(
                true, 10, 9, recipe, resources, false);

        assertEquals(10, result.loadedRounds());
        assertEquals(1, result.paidBatches());
        assertEquals(0, result.resources().amount("shell"));
    }

}
