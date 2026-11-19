package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkbenchGuiCraftAssertionTest {
    @Test
    void guiProbeUsesTetraContainerClicksForInputsInsteadOfWritingWorkbenchHandler() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        int start = source.indexOf("private static boolean prepareWorkbenchGuiProbe");
        String preparation = source.substring(start,
                source.indexOf("private static void craftBodyThroughWorkbenchForRuntimeVerification", start));

        assertTrue(preparation.contains("clearWorkbenchSlotsThroughMenu(player, workbench)"),
                "stale saved workbench inputs must be returned through the container before the probe starts");
        assertTrue(preparation.contains("player.containerMenu.clicked("),
                "workbench inputs must enter through the actual Tetra container menu");
        assertTrue(preparation.contains("TetraWorkbenchSlotLayout.playerInventoryMenuSlot("),
                "player input slots must use the verified Tetra menu mapping");
        assertFalse(preparation.contains("handler.setStackInSlot("),
                "the GUI path must not bypass the player's inventory with direct handler writes");
    }

    @Test
    void staleWorkbenchInputsAreReturnedToPlayerInventoryWithoutBeingDiscarded() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        int start = source.indexOf("private static boolean clearWorkbenchSlotsThroughMenu");
        int end = source.indexOf("private static boolean moveInventoryStackToWorkbench", start);
        String cleanup = source.substring(start, end);

        assertTrue(cleanup.contains("TetraWorkbenchSlotLayout.playerInventoryMenuSlot("),
                "stale workbench contents must return to a mapped player inventory slot");
        assertTrue(cleanup.contains("player.containerMenu.clicked("),
                "stale workbench contents must be removed by normal container pickup clicks");
        assertFalse(cleanup.contains("setStackInSlot("),
                "the probe must not delete saved workbench items through direct handler writes");
    }

    @Test
    void guiSettlementAssertionRunsBeforeServerSideRegressionCraft() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        int observation = source.indexOf("observeWorkbenchGuiSettlement(serverPlayer, stack)");
        int runtimeRegression = source.indexOf("craftBodyThroughWorkbenchForRuntimeVerification(serverPlayer, stack)");

        assertTrue(observation >= 0 && runtimeRegression > observation,
                "capture and assert the GUI result before the later server-side crafting regression can change it");
    }

    @Test
    void failedGuiPreparationDoesNotArmSettlementAssertionOrServerRegression() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        int start = source.indexOf("private static void openWorkbenchForRuntimeVerification");
        int end = source.indexOf("private static boolean clearWorkbenchSlotsThroughMenu", start);
        String opening = source.substring(start, end);

        int gate = opening.indexOf("if (guiProbePrepared)");
        int arm = opening.indexOf("putInt(WORKBENCH_GUI_PENDING, 80)");
        int skipped = opening.indexOf("remove(WORKBENCH_GUI_PENDING)");
        assertTrue(gate >= 0 && arm > gate && skipped > arm,
                "only a prepared GUI transaction may start observation and server-side regression timing");
        assertFalse(opening.substring(0, gate).contains("putInt(WORKBENCH_GUI_PENDING, 80)"),
                "the pending timer must not be armed before preparation succeeds");
    }

    @Test
    void passesOnlyWhenGuiChangesTheExpectedModuleAndConsumesMaterial() {
        var result = WorkbenchGuiCraftAssertion.evaluate(
                "taczintetra/body/pistol", "taczintetra/body/rifle", "taczintetra/body/rifle", 1, 0);

        assertTrue(result.passed());
    }

    @Test
    void rejectsExpectedModuleAlreadyPresentBeforeGuiInteraction() {
        var result = WorkbenchGuiCraftAssertion.evaluate(
                "taczintetra/body/rifle", "taczintetra/body/rifle", "taczintetra/body/rifle", 1, 0);

        assertFalse(result.passed(), "a preconfigured target cannot prove a GUI-triggered change");
    }

    @Test
    void rejectsModuleChangeWithoutMaterialConsumption() {
        var result = WorkbenchGuiCraftAssertion.evaluate(
                "taczintetra/body/pistol", "taczintetra/body/rifle", "taczintetra/body/rifle", 1, 1);

        assertFalse(result.passed(), "module state alone does not prove the material-limited transaction");
    }

    @Test
    void rejectsMaterialConsumptionWithoutExpectedModule() {
        var result = WorkbenchGuiCraftAssertion.evaluate(
                "taczintetra/body/pistol", "taczintetra/body/other", "taczintetra/body/rifle", 1, 0);

        assertFalse(result.passed());
    }

    @Test
    void rejectsMissingOrInvalidSnapshots() {
        assertFalse(WorkbenchGuiCraftAssertion.evaluate(null, "taczintetra/body/rifle",
                "taczintetra/body/rifle", 1, 0).passed());
        assertFalse(WorkbenchGuiCraftAssertion.evaluate("taczintetra/body/pistol",
                "taczintetra/body/rifle", "taczintetra/body/rifle", 0, 0).passed());
        assertFalse(WorkbenchGuiCraftAssertion.evaluate("taczintetra/body/pistol",
                "taczintetra/body/rifle", "taczintetra/body/rifle", 1, 2).passed());
    }
}
