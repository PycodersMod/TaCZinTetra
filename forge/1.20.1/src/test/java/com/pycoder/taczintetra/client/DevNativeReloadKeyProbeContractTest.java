package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevNativeReloadKeyProbeContractTest {
    @Test
    void automatedDrawWaitIsBoundedAndCapturesNativeTaczState() throws Exception {
        String automation = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(automation.contains("DevDrawWaitPolicy.timedOut(drawWaitTicks)"),
                "a stuck TaCZ draw state must stop the dev probe instead of polling forever");
        assertTrue(automation.contains("getSynDrawCoolDown()"),
                "the timeout record must include TaCZ's native draw cooldown");
        assertTrue(automation.contains("clientReadyToDraw={}"),
                "the timeout record must include TaCZ's client readiness state");
        assertTrue(automation.contains("DEV_DRAW_PROBE_TIMEOUT"),
                "the stopped probe must leave a stable searchable reason in the game log");
    }

    @Test
    void nativeReloadInputPolicyOnlyObservesEnabledMatchingPressAndReflectsNativeGate() {
        assertTrue(NativeReloadInputObservationPolicy.shouldObserve(true, 1, true));
        assertFalse(NativeReloadInputObservationPolicy.shouldObserve(false, 1, true));
        assertFalse(NativeReloadInputObservationPolicy.shouldObserve(true, 0, true));
        assertFalse(NativeReloadInputObservationPolicy.shouldObserve(true, 1, false));

        assertTrue(NativeReloadInputObservationPolicy.nativeGateOpen(true, true, false, true, false));
        assertFalse(NativeReloadInputObservationPolicy.nativeGateOpen(false, true, false, true, false));
        assertFalse(NativeReloadInputObservationPolicy.nativeGateOpen(true, false, false, true, false));
        assertFalse(NativeReloadInputObservationPolicy.nativeGateOpen(true, true, true, true, false));
        assertFalse(NativeReloadInputObservationPolicy.nativeGateOpen(true, true, false, false, false));
        assertFalse(NativeReloadInputObservationPolicy.nativeGateOpen(true, true, false, true, true));
    }

    @Test
    void dedicatedNativeReloadProbeIsOptInAndDoesNotSendReloadMessages() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String automation = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);
        String serverAutomation = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                StandardCharsets.UTF_8);
        Path observerPath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevNativeReloadKeyClientObserver.java");
        Path serverObserverPath = Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevNativeReloadKeyServerObserver.java");

        assertTrue(build.contains("titNativeReloadKeyProbe"),
                "the native-key probe must be explicitly enabled from the isolated Gradle run");
        assertTrue(build.contains("taczintetra.dev_native_reload_key_probe"),
                "the Gradle property must reach the client and integrated server JVM");
        assertTrue(automation.contains("NATIVE_RELOAD_KEY_PROBE"),
                "the general development automation must stop before opening screens or sending actions");
        assertTrue(serverAutomation.contains("NATIVE_RELOAD_KEY_PROBE"),
                "server-side setup must stop after preparing the one-round gun");
        assertTrue(serverAutomation.contains("F06_ZERO_AMMO_PROBE || F06_RELOAD_INTERRUPT_PROBE || NATIVE_RELOAD_KEY_PROBE"),
                "the native-key mode must seed one loaded round while keeping reserve ammo available");
        assertTrue(serverAutomation.contains("gun.setCurrentAmmoCount(stack, seededAmmo)"),
                "the diagnostic seed must use TaCZ's native accessor so the setter trace covers the initial state");
        assertFalse(serverAutomation.contains("tag.putInt(\"GunCurrentAmmoCount\", seededAmmo)"),
                "the diagnostic seed must not bypass TaCZ's native ammo accessor");
        int seedMethod = serverAutomation.indexOf("private static void seedProbeAmmo(");
        int uniqueId = serverAutomation.indexOf("if (NATIVE_RELOAD_KEY_PROBE) StackIdentity.ensureUniqueId(stack);", seedMethod);
        int nativeAmmoSetter = serverAutomation.indexOf("gun.setCurrentAmmoCount(stack, seededAmmo)", seedMethod);
        assertTrue(seedMethod >= 0 && uniqueId > seedMethod && nativeAmmoSetter > uniqueId,
                "the native reload probe must assign its stack identity before tracing the native ammo seed");

        assertTrue(Files.exists(observerPath), "a dedicated native-key observer must exist");
        String observer = Files.readString(observerPath, StandardCharsets.UTF_8);
        String serverObserver = Files.readString(serverObserverPath, StandardCharsets.UTF_8);
        assertTrue(observer.contains("NATIVE_RELOAD_KEY_READY"));
        assertTrue(observer.contains("NATIVE_RELOAD_KEY_STATE"));
        int snapshotStart = observer.indexOf("String snapshot =");
        int readinessCheck = observer.indexOf("if (minecraft.player.isAlive()", snapshotStart);
        assertTrue(snapshotStart >= 0 && readinessCheck > snapshotStart
                        && observer.substring(snapshotStart, readinessCheck).contains("screen"),
                "client state changes must include the active screen so the probe can wait until the world is interactive");
        assertTrue(observer.contains("NativeReloadReadinessPolicy.isReady"),
                "a chambered round with an empty magazine is still a valid TaCZ reload-ready state");
        assertTrue(serverObserver.contains("NativeReloadReadinessPolicy.isReady"),
                "the integrated server must apply the same reload-readiness rule");
        assertTrue(observer.contains("hasBulletInBarrel"),
                "client evidence must record TaCZ's native chamber state");
        assertTrue(serverObserver.contains("hasBulletInBarrel"),
                "server evidence must record TaCZ's native chamber state");
        assertTrue(observer.contains("reloadStateType"));
        assertTrue(observer.contains("ResourceNbtAdapter"));
        assertTrue(observer.contains("StackIdentity"));
        assertTrue(observer.contains("rawAmmoBeforeGet"),
                "the observer must distinguish raw stored NBT from the TaCZ API read");
        assertTrue(observer.contains("maxAmmo"),
                "the observer must record the capacity used while validating the loaded count");
        assertTrue(observer.contains("rawAmmoAfterGet"),
                "the observer must reveal whether reading through the API mutates the stored count");
        assertTrue(observer.contains("modulesReady"),
                "client snapshots must report the readiness marker even when it is absent");
        assertTrue(serverObserver.contains("modulesReady"),
                "server snapshots must report the readiness marker even when it is absent");
        assertTrue(observer.contains("playerAlive"),
                "client readiness evidence must expose the actual player life state");
        assertTrue(serverObserver.contains("playerAlive"),
                "server readiness evidence must expose the actual player life state");
        assertTrue(observer.contains("minecraft.player.isAlive()"),
                "a dead client must never produce a native-reload READY event");
        assertTrue(serverObserver.contains("player.isAlive()"),
                "a dead server player must never produce a native-reload READY event");
        assertFalse(observer.contains("|| !stack.getOrCreateTag().getBoolean(\"taczintetra_dev_modules_ready\")"),
                "the client observer must not discard the exact state needed to diagnose a missing marker");
        assertFalse(serverObserver.contains("|| !stack.getOrCreateTag().getBoolean(\"taczintetra_dev_modules_ready\")"),
                "the server observer must not discard the exact state needed to diagnose a missing marker");
        assertFalse(observer.contains("ReloadIntentMessage"),
                "the observer must never synthesize the Mod reload packet");
        assertFalse(observer.contains("sendToServer"),
                "the observer must only record state; VK_R will come from the external input harness");
    }

    @Test
    void dualHandInputObserverIsIndependentAndRecordsModifierKeys() throws Exception {
        String observer = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevNativeReloadKeyClientObserver.java"),
                StandardCharsets.UTF_8);

        assertTrue(observer.contains("taczintetra.dev_reload_key_input_observer"),
                "manual dual-hand input observation must have an independent opt-in switch");
        assertTrue(observer.contains("ENABLED || INPUT_OBSERVER_ENABLED"),
                "the key observer must work without enabling the single-hand server probe");
        assertTrue(observer.contains("Screen.hasControlDown()"),
                "the event record must distinguish Ctrl+R from the main-hand R action");
        assertTrue(observer.contains("Screen.hasShiftDown()"),
                "the event record must preserve the Shift+R fill-reload modifier");
        assertTrue(observer.contains("ctrlDown={}"));
        assertTrue(observer.contains("shiftDown={}"));
    }

    @Test
    void dualHandSetupOnlyModePreparesTheOffhandWithoutAutoSendingReloadOrFire() throws Exception {
        String automation = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"),
                StandardCharsets.UTF_8);

        assertTrue(automation.contains("taczintetra.dev_dual_hand_setup_only"),
                "the manual Ctrl+R fixture must be able to seed both hands without synthetic intents");
        int setupOnlyGate = automation.indexOf("if (RUN_DUAL_HAND_SETUP_ONLY)");
        int automaticProbe = automation.indexOf("if (RUN_DUAL_HAND_PROBE && !dualModeProbeSent)");
        assertTrue(setupOnlyGate >= 0 && automaticProbe > setupOnlyGate,
                "setup-only mode must return before the dual-hand auto-fire/reload sequence");
        assertTrue(automation.contains("DUAL_HAND_SETUP_ONLY_READY"),
                "the manual test must know when both independent gun stacks are available");
        int setupOnlyGatePosition = automation.indexOf("if (RUN_DUAL_HAND_SETUP_ONLY)");
        int holoProbe = automation.indexOf("if (!holoProbeLogged && minecraft.level != null");
        int jeiProbe = automation.indexOf("if (active && jeiScreenProbeStage == 0");
        assertTrue(holoProbe > setupOnlyGatePosition && jeiProbe > setupOnlyGatePosition,
                "setup-only mode must bypass GUI probes that steal the game screen before manual input observation");
    }

    @Test
    void nativeAdsMouseObserverIsOptInAndRecordsTaCZStateWithoutChangingIt() throws Exception {
        String observer = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevNativeReloadKeyClientObserver.java"),
                StandardCharsets.UTF_8);

        assertTrue(observer.contains("taczintetra.dev_ads_mouse_input_observer"));
        assertTrue(observer.contains("InputEvent.MouseButton.Post"),
                "observe the real post-dispatch RMB event after TaCZ AimKey handles it");
        assertTrue(observer.contains("NATIVE_ADS_MOUSE_INPUT"));
        assertTrue(observer.contains("aimState={}"));
        assertTrue(observer.contains("aimingZoom={}"));
        assertFalse(observer.contains("IClientPlayerGunOperator.fromLocalPlayer(minecraft.player).aim("),
                "the diagnostic observer must never synthesize the native aim state");
    }
}
