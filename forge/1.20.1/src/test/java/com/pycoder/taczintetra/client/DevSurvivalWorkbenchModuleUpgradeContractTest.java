package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevSurvivalWorkbenchModuleUpgradeContractTest {
    @Test
    void moduleUpgradeProbeIsOptInAndBuildsInputsThroughSurvivalRecipes() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        assertTrue(Files.exists(sourcePath), "the isolated real-client module-upgrade probe has not been implemented");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("taczintetra.dev_survival_workbench_module_upgrade_probe"));
        assertTrue(source.contains("GameType.SURVIVAL"));
        assertTrue(source.contains("stagedLogBlocks=6"));
        assertTrue(source.contains("Blocks.CRAFTING_TABLE"));
        assertTrue(source.contains("ModItems.STARTER_PISTOL.get()"));
        assertTrue(source.contains("woodBody()"));
        assertTrue(source.contains("CraftingMenuRecipePlacement.plan"));
        assertTrue(source.contains("rawMaterialsGranted=false"));
    }

    @Test
    void workbenchInputsUseTheContainerAndTheVisibleCraftButton() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        assertTrue(Files.exists(sourcePath), "the isolated real-client module-upgrade probe has not been implemented");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("containerMenu.clicked("));
        assertTrue(source.contains("DevClientAutomation.clickWorkbenchCraftButton(screen)"));
        assertTrue(source.contains("WorkbenchScreen"));
        assertTrue(source.contains("TETRA_SURVIVAL_MODULE_UPGRADE_ASSERTION PASS"));
        assertTrue(source.contains("materialBefore={}, materialAfter={}"));
        assertFalse(source.contains("handler.setStackInSlot("),
                "the player test must not bypass real inventory/menu transactions");
        assertFalse(source.contains("setModuleInSlot("),
                "the resulting module must come from the actual Tetra workbench interaction");
    }

    @Test
    void pickupRateLimitUsesTheMonotonicallyIncreasingPhaseTick() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("int tick = phaseTicks;")
                        && source.contains("SurvivalProbePickupCadencePolicy.shouldAssist(tick, lastPickupTick, 25)"),
                "pickup retries must use the increasing phase tick, not a countdown-derived inverse");
    }

    @Test
    void registryBackedItemsAreResolvedAfterModRegistriesAreReady() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertFalse(source.contains("private static final Item STARTER_PISTOL = BuiltInRegistries.ITEM.get("),
                "the event subscriber can initialize before deferred item registration completes");
        assertFalse(source.contains("private static final Item WOOD_BODY = BuiltInRegistries.ITEM.get("));
        assertFalse(source.contains("private static final Item MODULAR_DOUBLE = BuiltInRegistries.ITEM.get("));
        assertTrue(source.contains("ModItems.STARTER_PISTOL.get()"));
        assertTrue(source.contains("private static Item modularDouble()"));
        assertTrue(source.contains("ResourceLocation.parse(\"tetra:modular_double\")"));
    }

    @Test
    void survivalCraftedBodyAdvancesToHammerIngredientsBeforeHammerUse() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("WAIT_HAMMER_INGREDIENTS"));
        assertTrue(source.contains("case WAIT_HAMMER_INGREDIENTS -> waitForHammerIngredients(minecraft, player)"));
        assertTrue(source.contains("Phase.WAIT_HAMMER_INGREDIENTS"));
        assertTrue(source.contains("case WAIT_HAMMER -> waitForHammer(minecraft, player)"));
    }

    @Test
    void workbenchCraftButtonGetsBoundedGuiReadinessRetries() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("CRAFT_BUTTON_MAX_ATTEMPTS = 120"));
        assertTrue(source.contains("craftButtonAttempts++"));
        assertTrue(source.contains("if (craftButtonAttempts >= CRAFT_BUTTON_MAX_ATTEMPTS)"));
        assertFalse(source.contains("if (!handled) fail(\"the visible Tetra CraftButton"),
                "an initial GUI-not-ready frame must not abort the real session");
    }

    @Test
    void realWorkbenchProbeRequiresObservableRuntimeProfileChange() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("SurvivalModuleProfileChangeAssertion.snapshot(gun, target)"));
        assertTrue(source.contains("SurvivalModuleProfileChangeAssertion.snapshot(gun, result)"));
        assertTrue(source.contains("profileAssertion.passed()"));
        assertTrue(source.contains("rpm={}->{}"));
        assertTrue(source.contains("damage={}->{}"));
        assertTrue(source.contains("recoil={}->{}"));
        assertTrue(source.contains("twoHanded={}->{}"));
    }

    @Test
    void profileObservationWaitsForAcceptedGuiClickAndSettledWorkbenchTransaction() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevSurvivalWorkbenchModuleUpgradeAutomation.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        int verifyStart = source.indexOf("private static void verifyWorkbenchTransaction(Minecraft minecraft, ServerPlayer player)");
        int verifyEnd = source.indexOf("private static void verifyWorkbenchTransactionOnServer", verifyStart);
        String scheduling = source.substring(verifyStart, verifyEnd);
        int settledTransactionCheck = source.indexOf("if (!assertion.passed())", verifyEnd);
        int profileCheck = source.indexOf("if (!profileAssertion.passed())", verifyEnd);

        assertTrue(scheduling.contains("if (!craftButtonClicked || observationRequested) return;"),
                "server observation must not race ahead of the first visible CraftButton click");
        assertTrue(settledTransactionCheck >= 0 && profileCheck > settledTransactionCheck,
                "profile deltas are meaningful only after the module and material transaction settles");
    }
}
