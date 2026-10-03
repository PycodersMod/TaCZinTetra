package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadRuntimeSettlementServiceTest {
    @Test
    void lifecycleAdapterClearsMarkerOnInterruptionAndSettlesOnlyAtTerminalState() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeSettlementService.java"),
                StandardCharsets.UTF_8);
        String adapter = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);

        assertTrue(service.contains("remove(FILL_MODE)"));
        assertTrue(service.indexOf("boolean fillMode") < service.indexOf("clearPending(stack)"));
        assertTrue(service.contains("resources, fillMode"));
        assertTrue(adapter.contains("getStateType().name().equals(\"NOT_RELOADING\")"));
        assertTrue(adapter.contains("clearRuntimeState(stack)"));
    }

    @Test
    void interruptedReloadClearsOriginalStackRuntimeStateAndNativeHolder() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeSettlementService.java"),
                StandardCharsets.UTF_8);
        String dual = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/DualHandRuntimeState.java"),
                StandardCharsets.UTF_8);
        String coordinator = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeCoordinator.java"),
                StandardCharsets.UTF_8);

        assertTrue(service.contains("clearRuntimeState"));
        assertTrue(service.contains("RELOAD_STARTED_AT"));
        assertTrue(service.contains("RELOAD_STATE"));
        assertTrue(dual.contains("clearRuntimeState(context.reloadStack())"));
        assertTrue(coordinator.contains("ReloadState.StateType.NOT_RELOADING"));
        assertTrue(coordinator.contains("reloadTimestamp = -1L"));
    }

    @Test
    void explicitReloadInterruptResetsNativeHolderAfterDelegate() throws Exception {
        String adapter = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        int interrupt = adapter.indexOf("public static void interruptReload");
        int delegate = adapter.indexOf("delegate().interruptReload", interrupt);
        int reset = adapter.indexOf("data.reloadStateType = ReloadState.StateType.NOT_RELOADING", delegate);
        assertTrue(interrupt >= 0 && delegate > interrupt && reset > delegate);
        assertTrue(adapter.indexOf("data.reloadTimestamp = -1L", reset) > reset);
    }
}
