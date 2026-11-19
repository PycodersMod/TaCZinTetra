package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevF06ReloadInterruptAutomationContractTest {
    @Test
    void reloadInterruptionProbeIsExplicitlyOptInOnBothRuntimeSides() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevF06ReloadInterruptClientAutomation.java"),
                StandardCharsets.UTF_8);
        String devClient = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevF06ReloadInterruptServerAutomation.java"),
                StandardCharsets.UTF_8);
        String ammoSeed = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(build.contains("titF06ReloadInterruptProbe"));
        assertTrue(build.contains("taczintetra.dev_f06_reload_interrupt_probe"));
        assertTrue(client.contains("ReloadIntentMessage"));
        assertTrue(client.contains("StackIdentity.of"));
        assertTrue(client.contains("taczintetra_dev_f06_reload_probe_ready"),
                "the client must wait for authoritative one-round setup before sending the reload request");
        assertTrue(devClient.contains("if (F06_RELOAD_INTERRUPT_PROBE) return;"),
                "the dedicated reload probe must not race ordinary shooting automation");
        assertTrue(server.contains("ReloadRuntimeSettlementService.isPending"));
        assertTrue(server.contains("reloadStateType.isReloading()"));
        assertTrue(server.contains("ClientboundSetCarriedItemPacket"));
        assertTrue(server.contains("DevF06ReloadInterruptPolicy.passes"));
        assertTrue(server.contains("getInventory().getItem(gunSlot)"));
        assertTrue(server.contains("StackIdentity.of(slotStack)"));
        assertTrue(server.contains("isSameLogicalGunStack"));
        assertTrue(server.contains("pendingOnSlot"));
        assertTrue(ammoSeed.contains("F06_RELOAD_INTERRUPT_PROBE"));
        assertTrue(ammoSeed.contains("taczintetra_dev_f06_reload_probe_ready"));
        assertTrue(ammoSeed.contains("if (F06_RELOAD_INTERRUPT_PROBE || NATIVE_RELOAD_KEY_PROBE) return;"),
                "other server-side probes must not mutate either reload test gun");
        assertTrue(server.contains("F06_RELOAD_INTERRUPT_ASSERTION"));
        assertTrue(server.contains("ORIGINAL_GUNS"),
                "the server probe must retain the original reload-state source while validating the live inventory slot by stable identity");
        assertFalse(server.contains("getItem(gunSlot) == original"),
                "a Java object-reference comparison can reject the same serialized gun stack");
    }
}
