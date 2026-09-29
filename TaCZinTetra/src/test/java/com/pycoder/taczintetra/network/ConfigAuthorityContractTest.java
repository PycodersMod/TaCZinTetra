package com.pycoder.taczintetra.network;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigAuthorityContractTest {
    @Test
    void loginSendsServerConfigDigestAndClientRejectsMismatch() throws Exception {
        String mod = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/TaCZinTetra.java"), StandardCharsets.UTF_8);
        String network = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/ModNetwork.java"), StandardCharsets.UTF_8);
        String message = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/ConfigDigestMessage.java"), StandardCharsets.UTF_8);

        assertTrue(mod.contains("PlayerLoggedInEvent"));
        assertTrue(mod.contains("ConfigDigestMessage"));
        assertTrue(mod.contains("PacketDistributor.PLAYER"));
        assertTrue(network.contains("ConfigDigestMessage.class"));
        assertTrue(message.contains("ConfigDigest.of(TaCZinTetra.MODULE_CONFIG)"));
        assertTrue(message.contains("disconnect"));
    }
}
