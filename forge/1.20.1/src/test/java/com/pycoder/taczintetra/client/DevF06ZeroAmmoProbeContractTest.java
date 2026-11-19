package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevF06ZeroAmmoProbeContractTest {
    @Test
    void successfulShotDefersAttemptConsumptionUntilAmmoSynchronizationIsObserved() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        int shotCall = client.indexOf("ShootResult result = gunOperator.shoot();");
        int pendingSync = client.indexOf("f06AmmoSyncPending = true;", shotCall);
        int attemptConsumption = client.indexOf("shotsRemaining--;", shotCall);

        assertTrue(shotCall >= 0, "the automated real shot must remain in the probe flow");
        assertTrue(pendingSync > shotCall, "successful F06 shots must schedule authoritative ammo observation");
        assertTrue(attemptConsumption > pendingSync,
                "a successful shot must not consume another requested shot before sync observation resolves");
    }

    @Test
    void zeroAmmoProbeIsOptInAndFlowsFromGradleToBothSides() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(build.contains("titF06ZeroAmmoProbe"),
                "the F06 probe must be explicitly opt-in through the Gradle run configuration");
        assertTrue(build.contains("taczintetra.dev_f06_zero_ammo_probe"),
                "the Gradle property must reach the client and integrated server JVM");
        assertTrue(client.contains("taczintetra.dev_f06_zero_ammo_probe"),
                "client assertions must be disabled unless the explicit JVM property is set");
        assertTrue(client.contains("F06_ZERO_AMMO_ASSERTION"),
                "the client must emit a machine-searchable result for each shot assertion");
        assertTrue(server.contains("taczintetra.dev_f06_zero_ammo_probe"),
                "server-side probe setup must use the same explicit opt-in");
        assertTrue(server.contains("seededAmmo = F06_ZERO_AMMO_PROBE || F06_RELOAD_INTERRUPT_PROBE || NATIVE_RELOAD_KEY_PROBE ? 1 : 15"),
                "ordinary dev runs must keep 15 rounds while the explicit one-round probes seed one round");
    }
}
