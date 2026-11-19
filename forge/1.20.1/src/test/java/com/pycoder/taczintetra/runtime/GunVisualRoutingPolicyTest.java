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

    @Test
    void clientMixinAllowsTaczNativeOffhandRenderForDualPistols() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/ThirdPersonOffhandRenderMixin.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("@Mixin(AnimateGeoItemRenderer.class)"),
                "the mixin must target the class that actually declares TaCZ's render hooks");
        assertTrue(source.contains("method = \"blockOffhandRender\""),
                "TaCZ's first-person handler blocks the offhand before calling its renderer");
        assertTrue(source.contains("GunInputRouter.offhandShootAllowed("),
                "only a valid dual-pistol loadout should release TaCZ's offhand block");
        assertTrue(source.contains("TetraItemStackProfileResolver.isTwoHanded("),
                "a two-handed modular gun must keep TaCZ's native offhand block");
        assertTrue(source.contains("callback.setReturnValue(false)"),
                "the mixin must allow the library to render the offhand natively");
        assertTrue(source.contains("FIRST_PERSON_LEFT_HAND"),
                "the unblocked offhand must route through TaCZ's native first-person renderer");
        assertTrue(source.contains("@Invoker(value = \"renderFirstPerson\", remap = false)"),
                "offhand rendering must invoke the library method skipped by its vanilla ItemStack renderer");
        String firstPersonBranch = source.substring(
                source.indexOf("if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)"),
                source.indexOf("if (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND)"));
        assertTrue(firstPersonBranch.contains("taczintetra$renderFirstPerson("),
                "the left-hand render route must delegate model, pose, and animations to TaCZ");
        assertTrue(firstPersonBranch.contains("callback.cancel();"),
                "cancel vanilla's empty first-person render after TaCZ renders the offhand");
        assertTrue(source.contains("native first-person off-hand renderer invoked"),
                "the opt-in runtime probe must confirm TaCZ invoked its native renderer");
    }
}
