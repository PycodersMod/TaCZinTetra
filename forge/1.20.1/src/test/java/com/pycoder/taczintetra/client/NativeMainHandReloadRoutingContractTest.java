package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeMainHandReloadRoutingContractTest {
    @Test
    void plainMainHandReloadIsLeftToTaczAndOnlySpecialRoutesAreIntercepted() throws Exception {
        String handler = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java"),
                StandardCharsets.UTF_8);
        String mixin = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/MainHandReloadInputMixin.java"),
                StandardCharsets.UTF_8);

        assertTrue(handler.contains("GunInputRouter.shouldRouteReloadToCustomHandler(decision)"),
                "ordinary main-hand R must continue through TaCZ's native ReloadKey lifecycle");
        assertTrue(mixin.contains("Screen.hasShiftDown()"),
                "only the custom Shift+R refill route may cancel TaCZ's native main-hand reload");
    }
}
