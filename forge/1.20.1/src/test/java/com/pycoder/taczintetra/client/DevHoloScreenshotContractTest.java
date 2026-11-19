package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevHoloScreenshotContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java");

    @Test
    void holdHoloModeLatchesTheSpecialSchematicAndUsesClientTickCaptureFallback() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private static boolean specialSchematicProbeActive;"),
                "the special-page hold must survive Screen-class transitions");

        int openCall = source.indexOf("HoloGui.getInstance().openSchematic(");
        int latch = source.indexOf("specialSchematicProbeActive = true;", openCall);
        assertTrue(openCall >= 0 && latch > openCall,
                "latch the special schematic once Tetra opens its internal page");

        int holdBranch = source.indexOf("if (HOLD_HOLO && specialSchematicProbeActive)");
        int nextAutomationBranch = source.indexOf("if (!workbenchScreenshotSent", holdBranch);

        assertTrue(holdBranch >= 0 && nextAutomationBranch > holdBranch,
                "hold-holo capture fallback must run before other screen automation");
        String holdBody = source.substring(holdBranch, nextAutomationBranch);
        assertTrue(holdBody.contains("holoScreenshotTicks"),
                "fallback must respect the existing stable-screen delay");
        assertTrue(holdBody.contains("minecraft.screen != null"),
                "fallback must wait for an actual visible screen before capturing");
        assertTrue(holdBody.contains("Screenshot.grab(minecraft.gameDirectory"),
                "hold-holo mode must capture from the client tick when ScreenEvent is absent");
        assertTrue(holdBody.contains("holoScreenshotSent = true"),
                "both capture paths must share the one-shot guard");
    }

    @Test
    void renderCaptureAcceptsTheLatchedCurrentSpecialScreen() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        int handler = source.indexOf("public static void onScreenRender");
        int nextHandler = source.indexOf("public static void onWorkbenchScreenRender", handler);
        assertTrue(handler >= 0 && nextHandler > handler);
        String renderBody = source.substring(handler, nextHandler);
        assertTrue(renderBody.contains("specialSchematicProbeActive"),
                "capture should not depend only on the outer HoloGui Screen class");
        assertTrue(renderBody.contains("minecraft.screen != event.getScreen()"),
                "only capture the currently active screen");
    }

    @Test
    void holdHoloModeSuppressesCompetingClientAndServerProbes() throws Exception {
        String client = Files.readString(SOURCE, StandardCharsets.UTF_8);
        int holdBranch = client.indexOf("if (HOLD_HOLO && specialSchematicProbeActive)");
        int backpackProbe = client.indexOf("if (!HOLD_HOLO && !sophisticatedBackpackUseSent");
        assertTrue(holdBranch >= 0 && backpackProbe > holdBranch,
                "the latched Holo page must stop the client state machine before optional GUI probes");

        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        int playerTick = server.indexOf("public static void onPlayerTick");
        int restorePending = server.indexOf("restorePendingBackpackProbe(serverPlayer)", playerTick);
        int holdStop = server.indexOf("if (HOLD_HOLO) return;", restorePending);
        int nextProbe = server.indexOf("observeDevConfigChange(player)", holdStop);
        assertTrue(playerTick >= 0 && restorePending > playerTick && holdStop > restorePending
                        && nextProbe > holdStop,
                "the Holo-only inspection mode must suspend server-side automation after restoring pending inventory state");
    }
}
