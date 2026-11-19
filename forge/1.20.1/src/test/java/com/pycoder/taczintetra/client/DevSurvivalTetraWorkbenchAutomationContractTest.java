package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevSurvivalTetraWorkbenchAutomationContractTest {
    @Test
    void probeIsExplicitlyOptInAndUsesSurvivalGameMechanics() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalTetraWorkbenchAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("taczintetra.dev_survival_tetra_workbench_probe"));
        assertTrue(source.contains("GameType.SURVIVAL"));
        assertTrue(source.contains("gameMode.destroyBlock"));
        assertTrue(source.contains("oakHammerClicks(plankMenuSlot, stickMenuSlot)"));
        assertTrue(source.contains("gameMode.useItemOn"));
        assertTrue(source.contains("tetra:basic_workbench"));
    }

    @Test
    void acceptsOnlyTheVanillaTableUpgradeProducedByTheRealMalletInteraction() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalTetraWorkbenchAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("minecraft:crafting_table"));
        assertTrue(source.contains("BlockHitResult"));
        assertTrue(source.contains("TETRA_WORKBENCH_ACQUISITION_ASSERTION PASS"));
        assertTrue(source.contains("getMainHandItem()"));
        assertTrue(source.contains("rawMaterialsGranted=false"));
    }

    @Test
    void waitsForRealDropsAndRateLimitsIntegratedServerPickupTeleport() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalTetraWorkbenchAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("entity.tickCount >= 20"),
                "natural survival drops need their vanilla pickup-delay ticks before moving the player");
        assertTrue(source.contains("tick - lastPickupAssistTick < 25"),
                "the client tick must not enqueue the same server teleport every frame");
        assertTrue(source.contains("player.connection.teleport("),
                "the integrated-server teleport must synchronize the actual client player position");
        assertTrue(source.contains("TETRA_WORKBENCH_SURVIVAL harvest diagnostics:"),
                "the test report must preserve block-break and actual-drop state");
        assertTrue(source.contains("TETRA_WORKBENCH_SURVIVAL pickup diagnostics:"),
                "a failed pickup must preserve drop age, positions, and server-player distance");
        assertTrue(source.contains("TETRA_WORKBENCH_SURVIVAL pickup callback:"),
                "the async server callback must record whether its captured player and item remain valid");
        assertTrue(source.contains("craftSlots="),
                "a failed vanilla workbench craft must preserve the real 2x2 grid and result slot");
        assertTrue(source.contains("crafting-table recipe staged:"),
                "the vanilla recipe result must be recorded before the result click changes the grid");
        assertTrue(source.contains("SurvivalProbeRecoveryPolicy.shouldRecover"),
                "a stale death in a copied world must use the established integrated-server respawn path");
        assertTrue(source.contains("server.getPlayerList().respawn(serverPlayer, false)"));
        assertTrue(source.contains("connection.player = respawned"),
                "the actual connection must point to the server's replacement player after respawn");
    }
}
