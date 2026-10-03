package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceAccountOverflowTest {
    @Test
    void consumingLargeBatchCountDoesNotWrapResourceAmount() {
        ResourceAccount account = ResourceAccount.empty().insert("ammo", Integer.MAX_VALUE, Integer.MAX_VALUE).account();

        ResourceAccount result = account.consume(Map.of("ammo", 1), Integer.MAX_VALUE);

        assertEquals(0, result.amount("ammo"));
    }
}
