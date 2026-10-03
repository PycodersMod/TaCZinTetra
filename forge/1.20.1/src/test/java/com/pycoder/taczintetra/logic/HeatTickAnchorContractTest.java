package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatTickAnchorContractTest {
    @Test
    void coolingAdvancesThePersistedTimeAnchorAfterEachLazyUpdate() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        int cooldown = source.indexOf("public void tickHeat(");
        int nextMethod = source.indexOf("private void updateOverheat(", cooldown);
        String method = source.substring(cooldown, nextMethod);
        assertTrue(method.contains("RUNTIME_HEAT_TICK.getOrDefault(stack"),
                "lazy cooling must use a runtime anchor when available");
        assertTrue(method.contains("RUNTIME_HEAT_TICK.put(stack, gameTime);"),
                "lazy cooling must move its runtime anchor to the newly calculated time");
    }
}
