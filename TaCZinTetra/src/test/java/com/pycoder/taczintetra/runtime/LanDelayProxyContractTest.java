package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LanDelayProxyContractTest {
    @Test
    void delayRelayReadsAheadIntoABoundedQueueInsteadOfSleepingTheReader() throws Exception {
        String source = Files.readString(Path.of("../../tools/lan-delay-proxy.ps1"), StandardCharsets.UTF_8);
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

