package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevDualHandFireModeProbeContractTest {
    @Test
    void setupOnlyDualHandProbeCapturesFirstPersonEvidenceAfterBothGunStacksSync() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        int setupOnly = client.indexOf("if (RUN_DUAL_HAND_SETUP_ONLY)");
        int bothHandsReady = client.indexOf("if (mainGunReady && offhandGunReady", setupOnly);
        int capture = client.indexOf("taczintetra-dual-hand-first-person-offhand-probe", bothHandsReady);

        assertTrue(setupOnly >= 0 && bothHandsReady > setupOnly,
                "the isolated setup probe must wait for both authoritative gun stacks");
        assertTrue(capture > bothHandsReady,
                "the screenshot must be captured only after both hands contain modular guns");
        assertTrue(client.contains("dualHandSetupScreenshotSent"),
                "the isolated screenshot must be one-shot, not captured every render tick");
        assertTrue(client.contains("minecraft.screen == null"),
                "the evidence must capture the unobstructed first-person game view");
    }

    @Test
    void ordinaryDevAutomationDoesNotSilentlyTurnASingleHandGunIntoDualWield() throws Exception {
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        int syncBlock = server.indexOf("int syncTicks = OFFHAND_SYNC_TICKS.getOrDefault(player, 0);");
        int offhandCopy = server.indexOf("ItemStack offhand = stack.copy();", syncBlock);
        int explicitDualGate = server.lastIndexOf("if (DUAL_HAND_PROBE) {", syncBlock);
        assertTrue(explicitDualGate >= 0 && syncBlock > explicitDualGate && offhandCopy > syncBlock,
                "the recurring off-hand synchronization block must be enclosed by the explicit dual-hand probe");
        assertFalse(server.contains("offhandSyncLimit") || server.contains("DUAL_HAND_PROBE ? 1 : 100"),
                "ordinary development automation must never seed the off-hand as a fallback");
    }

    @Test
    void lanHostPublishesBeforeWaitingForGuestAndDoesNotGateSetupScreens() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        int tick = client.indexOf("public static void onClientTick(TickEvent.ClientTickEvent event)");
        int publish = client.indexOf("sendCommand(\"publish true survival 25565\")", tick);
        int guestGate = client.indexOf("if (!tryPassDualHandGuestGate(minecraft)) return;", tick);
        assertTrue(tick >= 0 && publish > tick && guestGate > publish,
                "the Host must publish LAN before it waits for a Guest, or the Guest cannot connect");

        int screenOpen = client.indexOf("public static void onScreenOpening(ScreenEvent.Opening event)");
        int screenClose = client.indexOf("public static void onScreenClosing(ScreenEvent.Closing event)", screenOpen);
        assertTrue(screenOpen >= 0 && screenClose > screenOpen);
        assertFalse(client.substring(screenOpen, screenClose).contains("tryPassDualHandGuestGate"),
                "guest synchronization must not block setup triggered by the Host's opening screen");
    }

    @Test
    void dualHandFireModeProbeIsExplicitlyOptInAndUsesTheExistingNetworkIntent() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(build.contains("titDualHandProbe"),
                "the dual-hand runtime probe must be enabled through an explicit Gradle property");
        assertTrue(build.contains("titDualHandProbeWaitForGuest"),
                "the optional LAN synchronization probe gate must be explicitly configurable");
        assertTrue(build.contains("taczintetra.dev_dual_hand_probe"),
                "the Gradle property must reach the client JVM");
        assertTrue(client.contains("Boolean.getBoolean(\"taczintetra.dev_dual_hand_probe\")"),
                "ordinary runs must leave the dual-hand probe disabled");
        assertTrue(client.contains("DevF06GuestJoinGatePolicy.evaluate("),
                "the synchronization probe must use the bounded guest-join policy");
        assertTrue(client.contains("getOnlinePlayers().size()"),
                "the LAN probe must check the actual client-visible player list before triggering the state transition");
        assertTrue(client.contains("new FireModeIntentMessage(true"),
                "the probe must exercise the existing off-hand fire-mode network intent");
        assertTrue(client.contains("NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis()"),
                "the off-hand probe must send TaCZ's client-base-relative timestamp");
        int waitForInventory = client.indexOf("if (ticksSinceDualSetup >= 20)");
        int sendOffhandShot = client.indexOf("new OffhandShootIntentMessage(", waitForInventory);
        assertTrue(client.contains("dualGunOperator.draw(minecraft.player.getMainHandItem())"),
                "the dual-hand probe must request TaCZ's draw before firing");
        assertTrue(client.contains("dualGunServerOperator.getSynDrawCoolDown()"),
                "the dual-hand probe must wait for TaCZ's server-synchronized draw cooldown");
        int requestDraw = client.indexOf("dualGunOperator.draw(minecraft.player.getMainHandItem())");
        int cooldownRead = client.indexOf("long drawCooldown = dualGunServerOperator.getSynDrawCoolDown()", requestDraw);
        int policyGate = client.indexOf("DevF06DrawCooldownPolicy.evaluate(", cooldownRead);
        assertTrue(requestDraw >= 0 && cooldownRead > requestDraw && policyGate > cooldownRead
                        && sendOffhandShot > policyGate,
                "the off-hand shoot packet must follow observation of the bounded server draw transition");
        assertTrue(client.contains("DevF06DrawCooldownPolicy.evaluate("),
                "the runtime probe must use the bounded cooldown transition policy");
        assertTrue(client.contains("dualHandDrawCooldownObservedPositive"),
                "the runtime probe must remember a positive server cooldown before accepting zero as ready");
        assertTrue(client.contains("dualHandDrawCooldownObservedPositive = drawResult.observedPositiveCooldown()"),
                "the runtime probe must preserve the cooldown transition state across ticks");
        assertTrue(client.contains("Outcome.READY") && client.contains("Outcome.TIMEOUT"),
                "the probe must distinguish an acknowledged cooldown transition from a bounded timeout");
        assertTrue(client.contains(".getDataHolder().clientBaseTimestamp"),
                "the off-hand probe must use TaCZ's synchronized client base timestamp");
        assertFalse(client.contains("identity, 1002, 0L"),
                "a zero client timestamp is rejected by the server network timestamp gate");
        String input = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java"),
                StandardCharsets.UTF_8);
        assertTrue(input.contains("NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis()"),
                "the real off-hand input handler must use the same timestamp contract as TaCZ");
        int offhandClearGuard = client.indexOf("if (!RUN_DUAL_HAND_PROBE) {", client.indexOf("private static void sendDualSetup"));
        int offhandClearCommand = client.indexOf("item replace entity @p weapon.offhand with air", offhandClearGuard);
        assertTrue(offhandClearGuard >= 0 && offhandClearCommand > offhandClearGuard,
                "dual-hand setup must not clear the off-hand after the server seeds the test gun");
        assertTrue(client.contains("TaCZinTetra dev dual-hand automation sent off-hand fire-mode intent"),
                "the client must log when the real intent is sent");
        assertTrue(server.contains("Boolean.getBoolean(\"taczintetra.dev_dual_hand_probe\")"),
                "the integrated server must isolate the dual-hand probe from other dev automation");
        assertFalse(server.contains("DUAL_HAND_PROBE ? 1 : 100"),
                "off-hand synchronization must not have an ordinary-run fallback");
        int offhandCopy = server.indexOf("ItemStack offhand = stack.copy();");
        int ensureOffhandIdentity = server.indexOf("StackIdentity.ensureUniqueId(offhand);", offhandCopy);
        int publishOffhand = server.indexOf("player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offhand);",
                offhandCopy);
        assertTrue(offhandCopy >= 0 && ensureOffhandIdentity > offhandCopy && publishOffhand > ensureOffhandIdentity,
                "the server must assign the off-hand stack identity before publishing it to the client");
        assertTrue(server.contains("if (DUAL_HAND_PROBE) return;"),
                "the probe must skip unrelated workbench and inventory automation");
    }
}
