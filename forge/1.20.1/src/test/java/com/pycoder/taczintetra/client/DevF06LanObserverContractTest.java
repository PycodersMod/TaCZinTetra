package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevF06LanObserverContractTest {
    @Test
    void guestObserverIsOptInAndReadsTheRemoteHostsAuthoritativeOffhandState() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String observer = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevF06LanObserver.java"),
                StandardCharsets.UTF_8);

        assertTrue(build.contains("titF06LanObserver"),
                "the LAN observer must be enabled only through an explicit Gradle property");
        assertTrue(build.contains("taczintetra.dev_f06_lan_observer"),
                "the Gradle property must reach only the opted-in client JVM");
        assertTrue(observer.contains("Boolean.getBoolean(\"taczintetra.dev_f06_lan_observer\")"),
                "ordinary client runs must leave this diagnostic observer disabled");
        assertTrue(observer.contains("candidate != minecraft.player"),
                "the observer must inspect the remote LAN host from the Guest client");
        assertTrue(observer.contains("getOffhandItem()"),
                "the observer must read the Host's replicated off-hand item stack");
        assertTrue(observer.contains("getCurrentAmmoCount"),
                "the observer must report authoritative replicated magazine ammo");
        assertTrue(observer.contains("taczintetra_reload_pending"),
                "the observer must report replicated reload lifecycle state");
        assertTrue(observer.contains("F06_LAN_HOST_OFFHAND_STATE"),
                "the observer must emit machine-checkable state transitions");
    }
}
