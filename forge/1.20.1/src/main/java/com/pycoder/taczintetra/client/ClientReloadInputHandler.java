package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.network.ModNetwork;
import com.pycoder.taczintetra.network.ReloadIntentMessage;
import com.pycoder.taczintetra.network.FireModeIntentMessage;
import com.pycoder.taczintetra.network.OffhandShootIntentMessage;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.runtime.GunInputRouter;
import com.pycoder.taczintetra.runtime.ReloadIntentFactory;
import com.pycoder.taczintetra.runtime.ReloadSequence;
import com.pycoder.taczintetra.runtime.GunVisualRoutingPolicy;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.tacz.guns.client.input.ReloadKey;
import com.tacz.guns.client.input.FireSelectKey;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/** Client-only input adapter; ordinary TaCZ guns are left to TaCZ itself. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class ClientReloadInputHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int sequence;

    private ClientReloadInputHandler() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_RELEASE)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack offhand = minecraft.player.getOffhandItem();
        boolean mainTwoHanded = main.getItem() instanceof ModularGunItem gun
                && TetraItemStackProfileResolver.isTwoHanded(gun, main);
        boolean offhandGun = offhand.getItem() instanceof ModularGunItem;
        boolean dualPistol = !mainTwoHanded && main.getItem() instanceof ModularGunItem && offhandGun;
        if (GunInputRouter.offhandShootAllowed(main.getItem() instanceof ModularGunItem,
                offhandGun, mainTwoHanded)) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                sequence = ReloadSequence.next(sequence);
                ModNetwork.CHANNEL.sendToServer(new OffhandShootIntentMessage(
                        StackIdentity.of(offhand), sequence, System.currentTimeMillis()));
            }
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev dual-hand RMB intercepted: action={}, offhandShot={}",
                        event.getAction(), event.getAction() == GLFW.GLFW_PRESS);
            }
            // Only the Pre phase is cancellable. Keep native TaCZ from seeing
            // the same offhand click after the custom route accepted it.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack offHand = minecraft.player.getOffhandItem();
        boolean mainGun = main.getItem() instanceof ModularGunItem;
        boolean offHandGun = offHand.getItem() instanceof ModularGunItem;
        boolean mainTwoHanded = mainGun
                && TetraItemStackProfileResolver.isTwoHanded((ModularGunItem) main.getItem(), main);
        boolean dualPistol = mainGun && offHandGun && !mainTwoHanded;
        boolean adsAllowed = GunVisualRoutingPolicy.resolve(
                net.minecraft.world.InteractionHand.MAIN_HAND, mainGun, dualPistol).adsAllowed();
        if (AimKey.AIM_KEY.matches(event.getKey(), event.getScanCode()) && !adsAllowed && dualPistol) {
            IClientPlayerGunOperator.fromLocalPlayer(minecraft.player).aim(false);
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev dual-hand ADS input intercepted: action={}, key={}, scanCode={}",
                        event.getAction(), event.getKey(), event.getScanCode());
            }
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (FireSelectKey.FIRE_SELECT_KEY.matches(event.getKey(), event.getScanCode())
                && Screen.hasControlDown()
                && dualPistol) {
            sequence = ReloadSequence.next(sequence);
            ModNetwork.CHANNEL.sendToServer(new FireModeIntentMessage(true,
                    StackIdentity.of(offHand), sequence));
            return;
        }
        if (!ReloadKey.RELOAD_KEY.matches(event.getKey(), event.getScanCode())) return;

        GunInputRouter.Decision decision = GunInputRouter.resolve(
                mainGun,
                offHandGun,
                mainTwoHanded,
                Screen.hasControlDown(),
                Screen.hasShiftDown());
        if (decision.state() == com.pycoder.taczintetra.runtime.GunInputState.NO_GUN) {
            return;
        }

        // Let TaCZ own main-hand reload. Sending a custom packet as well would
        // create two competing reload paths for one physical key press.
        if (!decision.routeReloadToOffHand()) return;

        ItemStack active = decision.routeReloadToOffHand() ? offHand : main;
        sequence = ReloadSequence.next(sequence);
        ReloadIntentMessage intent = ReloadIntentFactory.create(
                decision, StackIdentity.of(active), sequence);
        ModNetwork.CHANNEL.sendToServer(intent);
    }
}
