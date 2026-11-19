package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevQuickPlayTraceContractTest {
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/taczintetra.mixins.json");
    private static final Path QUICKPLAY_MIXIN = Path.of(
            "src/main/java/com/pycoder/taczintetra/mixin/QuickPlayTraceMixin.java");
    private static final Path MINECRAFT_MIXIN = Path.of(
            "src/main/java/com/pycoder/taczintetra/mixin/MinecraftQuickPlayTraceMixin.java");
    private static final Path SCREEN_TRACE = Path.of(
            "src/main/java/com/pycoder/taczintetra/client/DevQuickPlayScreenTrace.java");
    private static final Path TRACE_HELPER = Path.of(
            "src/main/java/com/pycoder/taczintetra/client/DevQuickPlayTrace.java");

    @Test
    void quickPlayTraceMixinsAreClientOnlyAndOptIn() throws Exception {
        assertTrue(Files.exists(QUICKPLAY_MIXIN), "QuickPlay boundary diagnostics are not implemented yet");
        assertTrue(Files.exists(MINECRAFT_MIXIN), "initial-screen boundary diagnostics are not implemented yet");
        assertTrue(Files.exists(SCREEN_TRACE), "screen transition diagnostics are not implemented yet");
        assertTrue(Files.exists(TRACE_HELPER), "QuickPlay trace gate is not implemented yet");
        String config = Files.readString(MIXIN_CONFIG, StandardCharsets.UTF_8);
        String quickPlay = Files.readString(QUICKPLAY_MIXIN, StandardCharsets.UTF_8);
        String minecraft = Files.readString(MINECRAFT_MIXIN, StandardCharsets.UTF_8);
        String screens = Files.readString(SCREEN_TRACE, StandardCharsets.UTF_8);

        assertTrue(config.contains("\"QuickPlayTraceMixin\""));
        assertTrue(config.contains("\"MinecraftQuickPlayTraceMixin\""));
        String trace = Files.readString(TRACE_HELPER, StandardCharsets.UTF_8);
        assertTrue(quickPlay.contains("DevQuickPlayTrace.isEnabled()"));
        assertTrue(minecraft.contains("DevQuickPlayTrace.isEnabled()"));
        assertTrue(screens.contains("DevQuickPlayTrace.isEnabled()"));
        assertTrue(trace.contains("taczintetra.dev_quickplay_trace"));
        assertTrue(quickPlay.contains("multiplayerTarget != null"),
                "singleplayer QuickPlay must not crash the opt-in trace");
        assertTrue(minecraft.contains("multiplayerTarget != null"),
                "initial-screen trace must tolerate absent multiplayer target");
    }

    @Test
    void quickPlayTraceCoversTheAsynchronousJoinBoundariesWithoutChangingThem() throws Exception {
        assertTrue(Files.exists(QUICKPLAY_MIXIN), "QuickPlay boundary diagnostics are not implemented yet");
        assertTrue(Files.exists(MINECRAFT_MIXIN), "initial-screen boundary diagnostics are not implemented yet");
        assertTrue(Files.exists(SCREEN_TRACE), "screen transition diagnostics are not implemented yet");
        String quickPlay = Files.readString(QUICKPLAY_MIXIN, StandardCharsets.UTF_8);
        String minecraft = Files.readString(MINECRAFT_MIXIN, StandardCharsets.UTF_8);
        String screens = Files.readString(SCREEN_TRACE, StandardCharsets.UTF_8);

        assertTrue(minecraft.contains("setInitialScreen"));
        assertTrue(quickPlay.contains("RESOURCE_RELOAD_COMPLETED"));
        assertTrue(quickPlay.contains("joinMultiplayerWorld"));
        assertTrue(screens.contains("ScreenEvent.Opening"));
        assertTrue(screens.contains("ConnectScreen"));
        assertFalse(quickPlay.contains("setScreen("), "diagnostics must not open or replace screens");
        assertFalse(quickPlay.contains("startConnecting("), "diagnostics must not initiate connections");
        assertFalse(minecraft.contains("setScreen("), "diagnostics must not open or replace screens");
    }
}
