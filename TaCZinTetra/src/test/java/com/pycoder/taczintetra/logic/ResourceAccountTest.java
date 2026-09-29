package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceAccountTest {
    @Test
    void resourcesHaveIndependentCapacity() {
        ResourceAccount account = ResourceAccount.empty();
        ResourceAccount.Insertion first = account.insert("tacz:9mm", 20, 12);
        ResourceAccount.Insertion second = first.account().insert("tacz:45", 5, 6);
        assertEquals(12, first.inserted());
        assertEquals(8, first.remainder());
        assertEquals(5, second.inserted());
        assertEquals(12, second.account().amount("tacz:9mm"));
        assertEquals(5, second.account().amount("tacz:45"));
    }

    @Test
    void invalidInputNeverCreatesNegativeState() {
        ResourceAccount.Insertion result = ResourceAccount.empty().insert("tacz:9mm", -2, 12);
        assertEquals(0, result.inserted());
        assertEquals(0, result.remainder());
        assertEquals(0, result.account().amount("tacz:9mm"));
    }

    @Test
    void batchAffordabilityAndConsumptionAreAtomic() {
        ResourceAccount account = ResourceAccount.empty()
                .insert("a", 10, 10).account()
                .insert("b", 5, 5).account();
        Map<String, Integer> cost = Map.of("a", 2, "b", 1);
        assertEquals(5, account.affordableBatches(cost));
        ResourceAccount remaining = account.consume(cost, 3);
        assertEquals(4, remaining.amount("a"));
        assertEquals(2, remaining.amount("b"));
        assertEquals(10, account.amount("a"));
        assertEquals(10, account.consume(cost, 99).amount("a"));
    }

    @Test
    void nullCostIsAStableNoOp() {
        ResourceAccount account = ResourceAccount.empty().insert("round", 8, 20).account();
        org.junit.jupiter.api.Assertions.assertSame(account, account.consume(null, 1));
    }
}
