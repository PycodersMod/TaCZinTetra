package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevSurvivalPartCraftAutomationContractTest {
    @Test
    void resolvesConfiguredOutputLazilyAfterForgeRegistryInitialization() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalPartCraftAutomation.java"),
                StandardCharsets.UTF_8);

        assertFalse(source.contains("static final Item OUTPUT = ModItems.configuredPart("),
                "RegistryObject.get must not run from this event subscriber's static initializer");
        assertTrue(source.contains("private static Item configuredOutput()"),
                "the configured item must be resolved lazily during the live client probe");
    }

    @Test
    void obtainsRecipeIngredientsThroughSurvivalWorldDropsAndVanillaInventoryCrafting() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalPartCraftAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("Blocks.OAK_LOG"), "the controlled world fixture must contain a real oak log block");
        assertTrue(source.contains("if (!minecraft.player.isAlive())"),
                "the probe must fail clearly instead of continuing its survival harvest after player death");
        assertTrue(source.contains("minecraft.screen instanceof DeathScreen"),
                "the probe must distinguish a real death screen from transient player initialization");
        assertTrue(source.contains("ensureSpawnSupport(minecraft)"),
                "a player loading over void must receive a floor before the survival probe can start");
        assertTrue(source.contains("onProbePlayerDeath(LivingDeathEvent event)"),
                "unexpected real-player death must retain its exact damage source in the probe log");
        assertTrue(source.contains("Blocks.STONE.defaultBlockState()"),
                "the isolated world fixture must provide a safe support block before survival harvesting");
        assertTrue(source.contains("oakLogPos = player.blockPosition().relative(player.getDirection(), 1)"),
                "the harvested log fixture must be one block forward at player height");
        assertTrue(source.contains("scale(0.2D)"),
                "the controlled survival player must stand within the vanilla pickup radius of the adjacent drop");
        assertTrue(source.contains("supportPos.getX() + 0.5D + pickupReachOffset.x"),
                "the pickup fixture must center the player on its support block to avoid corner-distance misses");
        assertTrue(source.contains("activeServerPlayer.gameMode.destroyBlock(oakLogPos)"),
                "the integrated-server survival player must harvest the real world block through game mode");
        assertTrue(source.contains("inventoryCraftingMenuSlot("), "intermediate planks and sticks must use the vanilla 2x2 menu");
        assertFalse(source.contains("give @p minecraft:oak_planks"), "raw planks must not be granted by commands");
        assertFalse(source.contains("give @p minecraft:stick"), "raw sticks must not be granted by commands");
        assertTrue(source.contains("SURVIVAL_PART_CRAFT setup state:"),
                "failed setup diagnostics must capture mode and both controlled block states");
        assertTrue(source.contains("serverTable="),
                "setup diagnostics must distinguish server block state from the client chunk view");
        assertTrue(source.contains("nearbyDrops="),
                "a failed harvest must distinguish missing drops from inventory pickup failure");
        assertFalse(source.contains("sendCommand(\"setblock "),
                "the isolated fixture must not depend on world cheats to place blocks");
        assertFalse(source.contains("sendCommand(\"clear @p"),
                "the isolated fixture must clear the test inventory through its local server API");
        assertFalse(source.contains("sendCommand(\"gamemode survival"),
                "the isolated fixture must select survival through the local server API");
        assertFalse(source.contains("sendCommand(\"save-all"),
                "the isolated probe must not require cheats to save; graceful world exit performs normal saving");
        assertTrue(source.contains("currentServerPlayer.getInventory().clearContent()"),
                "the isolated fixture must clear inventory without granting recipe ingredients");
        assertTrue(source.contains("currentServerPlayer.setGameMode(GameType.SURVIVAL)"),
                "the isolated fixture must set survival mode without requiring operator permission");
        assertTrue(source.contains("plankGrid="),
                "the plank-result timeout must report exact player crafting-grid slot state");
        assertTrue(source.contains("cursor="),
                "the plank-result timeout must report the carried cursor stack");
        assertTrue(source.contains("case WAIT_LOG_CURSOR -> waitForLogCursor(minecraft)"),
                "the harvested log must be confirmed on the cursor before the next menu click");
        assertTrue(source.contains("case WAIT_LOG_IN_CRAFT_GRID -> waitForLogInCraftGrid(minecraft)"),
                "the crafting input must be confirmed after a separate server round trip");
        assertTrue(source.contains("menu.clicked(slotId, button, clickType, serverPlayer)"),
                "the development fixture must apply survival inventory interactions to the authoritative server menu");
        assertTrue(source.contains("menu.broadcastChanges()"),
                "authoritative menu updates must be synchronized back to the live client");
        assertTrue(source.contains("serverOakLogs < 1"),
                "menu transfer must wait for server-confirmed item pickup, not client prediction alone");
        assertTrue(source.contains("positioned the development player beside the real harvested drop"),
                "the isolated probe may recover pickup reach only after a real survival drop ages naturally");
        String stickCrafting = source.substring(source.indexOf("private static void waitForPlanksSettled"),
                source.indexOf("private static void waitForStickResult"));
        int firstInput = stickCrafting.indexOf("inventoryCraftingMenuSlot(0, 0)");
        int secondInput = stickCrafting.indexOf("inventoryCraftingMenuSlot(1, 0)");
        assertTrue(firstInput >= 0 && secondInput > firstInput,
                "two sticks inputs must be placed vertically in the 2x2 player crafting grid");
        assertFalse(stickCrafting.substring(firstInput, secondInput).contains("sourceSlot"),
                "after splitting the plank stack, do not click the source between placing the two carried planks");
        assertTrue(source.contains("SURVIVAL_PART_CRAFT configured recipe state:"),
                "the final configured-recipe timeout must report the grid, cursor, and remaining ingredients");
        assertTrue(source.contains("private static void clickIntegratedServerMenuSequence"),
                "multi-slot survival recipe inputs must be applied as an ordered authoritative server sequence");
        assertTrue(source.contains("craftingTablePlayerInventoryMenuSlot(plankInventoryIndex)"),
                "crafting-table inputs must use the slot map that accounts for its extra result slot");
        assertTrue(source.contains("place planks and stick in vanilla shaped recipe"),
                "the configured recipe inputs must be staged through the actual server-side crafting menu");
        assertTrue(source.contains("WAIT_RECIPE_CURSOR_SETTLED -> waitForRecipeCursorSettled(minecraft)"),
                "remaining recipe ingredients on the cursor must be returned before taking the real result");
    }

    @Test
    void recoversOnlyTheExplicitDevelopmentProbeFromAStaleDeadSingleplayerSave() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalPartCraftAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("recoverDeadProbePlayer(minecraft, serverPlayer)"),
                "the isolated survival probe must recover a stale dead player instead of timing out");
        assertTrue(source.contains("server.getPlayerList().respawn(serverPlayer, false)"),
                "recovery must use the integrated server's real player respawn path");
        assertTrue(source.contains("connection.player = respawned"),
                "recovery must update the packet listener to the replacement player just like vanilla respawn handling");
        assertTrue(source.contains("setRespawnPosition(serverLevel.dimension(), safeSpawn, player.getYRot(), true, false)"),
                "the probe must set a controlled safe respawn before replacing the dead player");
        assertTrue(source.contains("SURVIVAL_PART_CRAFT stale-player recovery"),
                "the report must retain the pre/post recovery state for diagnosis");
    }
}
