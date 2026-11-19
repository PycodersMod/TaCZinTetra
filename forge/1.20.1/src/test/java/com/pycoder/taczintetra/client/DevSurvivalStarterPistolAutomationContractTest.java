package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevSurvivalStarterPistolAutomationContractTest {
    @Test
    void isOptInAndUsesActualSurvivalHarvestAndVanillaMenuCrafting() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalStarterPistolAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("taczintetra.dev_survival_starter_pistol_recipe_craft"));
        assertTrue(source.contains("active.gameMode.destroyBlock(pos)"));
        assertTrue(source.contains("setGameMode(GameType.SURVIVAL)"));
        assertTrue(source.contains("ClickType.QUICK_MOVE"));
        assertTrue(source.contains("StarterPistolSurvivalRecipePlan.menuClicks("));
        assertTrue(source.contains("restoreTableApproachPosition(minecraft)"),
                "drop-pickup teleports must not leave the player inside the crafting-table interaction position");
        assertTrue(source.contains("tableInteractionAttempts"),
                "the probe must log and boundedly retry table interaction if no CraftingScreen opens");
        assertTrue(source.contains("minecraft.execute(minecraft::stop)"),
                "a passing probe must gracefully stop the isolated game before unrelated world events alter state");
        assertTrue(source.contains("ProcessHandle.current().pid()"),
                "runtime PASS evidence must include the actual Minecraft JVM PID");
        assertTrue(source.contains("server.getPlayerList().respawn(serverPlayer, false)"));
        assertTrue(source.contains("connection.player = respawned"),
                "recovering a stale dead player must rebind the integrated-server packet connection");
        int firstWorldMutation = source.indexOf("serverLevel.setBlock(support,");
        assertTrue(firstWorldMutation >= 0
                        && source.lastIndexOf("server.execute(() ->", firstWorldMutation) >= 0,
                "fixture blocks must be changed on the integrated-server thread so the client receives updates");
        assertFalse(source.contains("give @p"), "raw materials and crafted items must not be granted by commands");
    }

    @Test
    void assertsTheActualPistolAndExactRemainingSurvivalInventory() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalStarterPistolAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("inventory.countItem(Items.OAK_LOG) == 0"));
        assertTrue(source.contains("inventory.countItem(Items.OAK_PLANKS) == 3"));
        assertTrue(source.contains("inventory.countItem(Items.STICK) == 3"));
        assertTrue(source.contains("inventory.countItem(Items.OAK_SLAB) == 5"));
        assertTrue(source.contains("inventory.countItem(ModItems.STARTER_PISTOL.get()) == 1"));
        assertTrue(source.contains("SURVIVAL_STARTER_PISTOL_ASSERTION PASS"));
    }
}
