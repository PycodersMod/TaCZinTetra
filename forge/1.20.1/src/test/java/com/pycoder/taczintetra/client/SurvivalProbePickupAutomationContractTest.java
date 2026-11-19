package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalProbePickupAutomationContractTest {
    @Test
    void pickupAssistCreatesStableGroundAndSynchronizesThePlayerTeleport() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalPartCraftAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("BlockPos pickupFloor = BlockPos.containing(dropPosition).below()"),
                "pickup assist must provide a stable floor under a drop that rolled away from the spawn support");
        assertTrue(source.contains("serverLevel.setBlock(pickupFloor, Blocks.STONE.defaultBlockState(), 3)"),
                "the isolated probe must prevent the survival player falling away from the harvested drop");
        assertTrue(source.contains("activeServerPlayer.connection.teleport(dropPosition.x, dropPosition.y, dropPosition.z"),
                "pickup positioning must send the authoritative server teleport packet to the client");
        assertTrue(!source.contains("activeServerPlayer.teleportTo(serverLevel, dropPosition.x, dropPosition.y, dropPosition.z"),
                "the level-aware entity teleport overload did not move the server player in the real probe");
        assertTrue(source.contains("pickupPlayerPos={}"),
                "pickup telemetry must include the server player's post-teleport position");
        assertTrue(source.contains("connectionPlayer == activeServerPlayer"),
                "pickup telemetry must verify the packet listener is bound to the authoritative player object");
        assertTrue(source.contains("connectionPlayerPos={}"),
                "pickup telemetry must compare the listener player and server player positions");
    }
}
