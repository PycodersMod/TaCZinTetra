package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LanDelayProxyContractTest {
    private static Path findWorkspaceTool() {
        Path directory = Path.of("").toAbsolutePath().normalize();
        while (directory != null) {
            Path candidate = directory.resolve("tools").resolve("lan-delay-proxy.ps1");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        return null;
    }

    @Test
    void delayRelayReadsAheadIntoABoundedQueueInsteadOfSleepingTheReader() throws Exception {
        Path tool = findWorkspaceTool();
        Assumptions.assumeTrue(tool != null, "Workspace utility is unavailable in a standalone checkout.");
        String source = Files.readString(tool, StandardCharsets.UTF_8);
        assertTrue(source.contains("Channel.CreateBounded"));
        assertTrue(source.contains("ReadAllAsync"));
        assertTrue(source.contains("queue.Writer.WriteAsync"));
        assertTrue(source.contains("queue.Writer.TryComplete"));
        assertTrue(source.contains("AcceptTcpClientAsync(token)"));
        assertTrue(source.contains("class DelayedPacket"));
        assertTrue(source.contains("packet.DueAt - DateTime.UtcNow"));
        assertTrue(source.contains("requires PowerShell 7+"));
    }
}

