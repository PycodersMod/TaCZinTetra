package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczNativeAimRoutePolicyTest {
    @Test
    void preservesNativeTaCZGunAimRouting() {
        assertTrue(TaczNativeAimRoutePolicy.allowMainHandAim(true, false, false, false));
    }

    @Test
    void enablesTaCZNativeAimForSingleTetraModularGun() {
        assertTrue(TaczNativeAimRoutePolicy.allowMainHandAim(false, true, false, false));
    }

    @Test
    void keepsRightMouseReservedForOffhandShotWhenBothHandsHoldOneHandedModularGuns() {
        assertFalse(TaczNativeAimRoutePolicy.allowMainHandAim(false, true, true, false));
    }

    @Test
    void allowsMainHandAimWhenTetraProfileMakesTheGunTwoHanded() {
        assertTrue(TaczNativeAimRoutePolicy.allowMainHandAim(false, true, true, true));
    }

    @Test
    void rejectsNonGunItems() {
        assertFalse(TaczNativeAimRoutePolicy.allowMainHandAim(false, false, false, false));
    }

    @Test
    void mixinHooksTaCZNativeAimEligibilityAndIsRegisteredAsClientOnly() throws Exception {
        String mixin = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/ModularGunNativeAimMixin.java"),
                StandardCharsets.UTF_8);
        String config = Files.readString(Path.of("src/main/resources/taczintetra.mixins.json"),
                StandardCharsets.UTF_8);

        assertTrue(mixin.contains("@Mixin(value = AimKey.class"));
        assertTrue(mixin.contains("@Redirect("));
        assertTrue(mixin.contains("IGun;mainHandHoldGun"));
        assertTrue(mixin.contains("TaczNativeAimRoutePolicy.allowMainHandAim"));
        assertTrue(config.contains("\"client\"") && config.contains("\"ModularGunNativeAimMixin\""));
    }
}
