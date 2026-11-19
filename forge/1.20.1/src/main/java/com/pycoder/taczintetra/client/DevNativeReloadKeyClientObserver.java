package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.ReloadKey;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.util.InputExtraCheck;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/** Read-only client observer for a real TaCZ reload-key input. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevNativeReloadKeyClientObserver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_native_reload_key_probe");
    private static final boolean INPUT_OBSERVER_ENABLED = Boolean.getBoolean(
            "taczintetra.dev_reload_key_input_observer");
    private static final boolean ADS_MOUSE_INPUT_OBSERVER_ENABLED = Boolean.getBoolean(
            "taczintetra.dev_ads_mouse_input_observer");
    private static String previousSnapshot = "";
    private static boolean readyLogged;

    private DevNativeReloadKeyClientObserver() { }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        previousSnapshot = "";
        readyLogged = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onReloadKeyInput(InputEvent.Key event) {
        boolean reloadKeyMatched = ReloadKey.RELOAD_KEY.matches(event.getKey(), event.getScanCode());
        if (!NativeReloadInputObservationPolicy.shouldObserve(
                ENABLED || INPUT_OBSERVER_ENABLED, event.getAction(), reloadKeyMatched)) return;

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        boolean playerPresent = player != null;
        boolean spectator = playerPresent && player.isSpectator();
        ItemStack main = playerPresent ? player.getMainHandItem() : ItemStack.EMPTY;
        IGun gun = IGun.getIGunOrNull(main);
        boolean usesInventoryAmmo = gun != null && gun.useInventoryAmmo(main);
        boolean inputExtraCheckInGame = InputExtraCheck.isInGame();
        boolean nativeGateOpen = NativeReloadInputObservationPolicy.nativeGateOpen(
                inputExtraCheckInGame, playerPresent, spectator, gun != null, usesInventoryAmmo);
        boolean eventCanceled = event.isCancelable() && event.isCanceled();
        boolean controlDown = Screen.hasControlDown();
        boolean shiftDown = Screen.hasShiftDown();
        String screen = minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
        String item = main.isEmpty() ? "empty" : main.getItem().getDescriptionId();
        int identity = main.isEmpty() ? 0 : StackIdentity.of(main);

        LOGGER.info("NATIVE_RELOAD_KEY_INPUT side=client key={} scanCode={} action={} keyMatched={} ctrlDown={} shiftDown={} eventCanceled={} inputExtraCheckInGame={} windowActive={} mouseGrabbed={} screen={} playerPresent={} spectator={} mainHandItem={} isTaczGun={} useInventoryAmmo={} nativeGateOpen={} modularGun={} stackIdentity={}",
                event.getKey(), event.getScanCode(), event.getAction(), reloadKeyMatched, controlDown, shiftDown,
                eventCanceled,
                inputExtraCheckInGame, minecraft.isWindowActive(), minecraft.mouseHandler.isMouseGrabbed(), screen,
                playerPresent, spectator, item, gun != null, usesInventoryAmmo, nativeGateOpen,
                main.getItem() instanceof ModularGunItem, identity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onNativeAdsMouseInput(InputEvent.MouseButton.Post event) {
        if (!ADS_MOUSE_INPUT_OBSERVER_ENABLED || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        ItemStack main = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(main);
        float aimingZoom = gun == null ? 1.0f : gun.getAimingZoom(main);
        boolean aimState = player instanceof IClientPlayerGunOperator operator && operator.isAim();
        float aimProgress = player instanceof IClientPlayerGunOperator operator
                ? operator.getClientAimingProgress(1.0f) : 0.0f;
        String screen = minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
        LOGGER.info("NATIVE_ADS_MOUSE_INPUT button={} action={} inputExtraCheckInGame={} windowActive={} mouseGrabbed={} screen={} playerPresent={} mainHandItem={} isTaczGun={} aimState={} aimProgress={} aimingZoom={}",
                event.getButton(), event.getAction(), InputExtraCheck.isInGame(), minecraft.isWindowActive(),
                minecraft.mouseHandler.isMouseGrabbed(), screen, player != null,
                main.isEmpty() ? "empty" : main.getItem().getDescriptionId(), gun != null,
                aimState, aimProgress, aimingZoom);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof ModularGunItem gun)) return;
        boolean modulesReady = stack.getOrCreateTag().getBoolean("taczintetra_dev_modules_ready");
        boolean playerAlive = minecraft.player.isAlive();

        int rawAmmoBeforeGet = stack.getOrCreateTag().getInt("GunCurrentAmmoCount");
        int maxAmmo = gun.getMaxDummyAmmoAmount(stack);
        int ammo = gun.getCurrentAmmoCount(stack);
        int rawAmmoAfterGet = stack.getOrCreateTag().getInt("GunCurrentAmmoCount");
        int reserve = ResourceNbtAdapter.read(stack.getOrCreateTag(),
                ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm");
        boolean hasBulletInBarrel = gun.hasBulletInBarrel(stack);
        int identity = StackIdentity.of(stack);
        String uuid = StackIdentity.hasUniqueId(stack)
                ? stack.getOrCreateTag().getUUID(StackIdentity.UNIQUE_ID).toString() : "missing";
        var reloadState = IGunOperator.fromLivingEntity(minecraft.player).getDataHolder().reloadStateType;
        String state = reloadState == null ? "null" : reloadState.name();
        String screen = minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
        String snapshot = modulesReady + "|" + playerAlive + "|" + identity + "|" + uuid + "|" + rawAmmoBeforeGet + "|" + maxAmmo
                + "|" + ammo + "|" + rawAmmoAfterGet + "|" + reserve + "|"
                + hasBulletInBarrel + "|" + state + "|" + screen;

        if (minecraft.player.isAlive() && !readyLogged
                && NativeReloadReadinessPolicy.isReady(ammo, reserve, hasBulletInBarrel)) {
            readyLogged = true;
            LOGGER.info("NATIVE_RELOAD_KEY_READY side=client player={} playerAlive={} modulesReady={} stackIdentity={} stackUuid={} ammo={} reserve9mm={} hasBulletInBarrel={} reloadState={}",
                    minecraft.player.getGameProfile().getName(), playerAlive, modulesReady, identity, uuid, ammo, reserve,
                    hasBulletInBarrel, state);
        }
        if (!snapshot.equals(previousSnapshot)) {
            previousSnapshot = snapshot;
            LOGGER.info("NATIVE_RELOAD_KEY_STATE side=client player={} playerAlive={} modulesReady={} stackIdentity={} stackUuid={} rawAmmoBeforeGet={} maxAmmo={} ammo={} rawAmmoAfterGet={} reserve9mm={} hasBulletInBarrel={} reloadState={} screen={}",
                    minecraft.player.getGameProfile().getName(), playerAlive, modulesReady, identity, uuid, rawAmmoBeforeGet, maxAmmo,
                    ammo, rawAmmoAfterGet, reserve, hasBulletInBarrel, state, screen);
        }
    }
}
