package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevBodyVisualProbeAutomationContractTest {
    @Test
    void bodyVisualProbeSkipsDamageTargetAndWorkbenchAutomationAfterModuleSync() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);

        int modulesReadyBranch = source.indexOf("getBoolean(MODULES_READY)");
        int readyProbeGate = source.indexOf("if (BODY_VISUAL_PROBE)", modulesReadyBranch);
        int damageTarget = source.indexOf("ensureDamageProbeTarget(player)", modulesReadyBranch);
        int initialModulesReadyWrite = source.indexOf("putBoolean(MODULES_READY, true)");
        int initialProbeGate = source.indexOf("if (BODY_VISUAL_PROBE)", initialModulesReadyWrite);
        int workbenchAutomation = source.indexOf("openWorkbenchForRuntimeVerification(player)", initialModulesReadyWrite);

        assertTrue(source.contains("taczintetra.dev_body_visual_probe"),
                "probe-only server behavior must have an explicit opt-in property");
        assertTrue(modulesReadyBranch >= 0 && readyProbeGate > modulesReadyBranch
                        && readyProbeGate < damageTarget,
                "the already-assembled probe branch must exit before creating a damage target");
        assertTrue(initialModulesReadyWrite >= 0 && initialProbeGate > initialModulesReadyWrite
                        && initialProbeGate < workbenchAutomation,
                "the initial assembly branch must exit before opening the Tetra workbench");
        assertTrue(source.contains("inventoryMenu.broadcastChanges()"),
                "the isolated probe must still synchronize the assembled gun to the client");
    }
}
