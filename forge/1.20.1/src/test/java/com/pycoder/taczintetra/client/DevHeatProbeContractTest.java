package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevHeatProbeContractTest {
    @Test
    void developmentProbeCoversHeatLockCoolingAndUnlock() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("dev heat probe"));
        assertTrue(source.contains("isOverheatLocked"));
        assertTrue(source.contains("heat cooled and unlocked"));
    }

    @Test
    void holoSphereProbeInitializesTheSameDualHandSetupAsWorkbenchProbe() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        String marker = "if (event.getScreen() instanceof HoloGui)";
        assertTrue(source.contains(marker));
        String holoBranch = source.substring(source.indexOf(marker),
                source.indexOf("if (!(event.getScreen() instanceof WorkbenchScreen))"));
        assertTrue(holoBranch.contains("sendDualSetup"),
                "HoloSphere opening must initialize the same dev dual-hand setup");
    }

    @Test
    void holoSphereScreenshotWaitsForWorldAndScreenRender() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("if (!holoProbeLogged && minecraft.level != null"),
                "the HoloSphere probe must wait until a world is attached");
        assertTrue(source.contains("ScreenEvent.Render.Post"),
                "the screenshot must be captured after the HoloGui has rendered");
        assertTrue(source.contains("event.getScreen() instanceof HoloGui"),
                "the screenshot must reject loading and non-Holo screens");
    }
}
