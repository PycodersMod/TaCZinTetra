package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GunVisualRoutingPolicyTest {
    @Test
    void createsAVisualModelOnlyForAHeldModularGun() {
        assertTrue(GunVisualRoutingPolicy.resolve(InteractionHand.MAIN_HAND, true, false).createModelInstance());
        assertFalse(GunVisualRoutingPolicy.resolve(InteractionHand.OFF_HAND, false, false).createModelInstance());
    }

    @Test
    void keepsAnimationChannelsSeparateByHand() {
        assertEquals("main_hand", GunVisualRoutingPolicy.resolve(InteractionHand.MAIN_HAND, true, false).animationChannel());
        assertEquals("off_hand", GunVisualRoutingPolicy.resolve(InteractionHand.OFF_HAND, true, false).animationChannel());
    }

    @Test
    void disablesAdsOnlyForDualPistolVisuals() {
        assertFalse(GunVisualRoutingPolicy.resolve(InteractionHand.MAIN_HAND, true, true).adsAllowed());
        assertTrue(GunVisualRoutingPolicy.resolve(InteractionHand.MAIN_HAND, true, false).adsAllowed());
    }

    @Test
    void clientInputUsesTheVisualPolicyForDualAds() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("GunVisualRoutingPolicy.resolve"));
        assertTrue(source.contains("adsAllowed()"));
    }

    @Test
    void clientMixinOwnsOnlyThirdPersonOffhandFallback() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/taczintetra.mixins.json"), StandardCharsets.UTF_8);
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/ThirdPersonOffhandRenderMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains("\"client\""));
        assertTrue(config.contains("ThirdPersonOffhandRenderMixin"));
        assertTrue(config.contains("ThirdPersonHumanoidOffhandProbeMixin"));
        assertTrue(source.contains("THIRD_PERSON_LEFT_HAND"));
        assertTrue(source.contains("instanceof ModularGunItem"));
        assertTrue(Files.readString(Path.of(
                "src/main/resources/assets/tacz/custom/taczintetra/assets/tacz/display/guns/modular_gun_display.json"),
                StandardCharsets.UTF_8).contains("\"offhand_show\""));
    }
}
