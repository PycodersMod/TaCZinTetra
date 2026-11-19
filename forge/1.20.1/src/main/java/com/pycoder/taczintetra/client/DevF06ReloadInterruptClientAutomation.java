package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.network.ModNetwork;
import com.pycoder.taczintetra.network.ReloadIntentMessage;
import com.pycoder.taczintetra.network.StackIdentity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Opt-in real-client driver for the F06 interrupted-reload integration probe. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevF06ReloadInterruptClientAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_f06_reload_interrupt_probe");
    private static int stage;
    private static int waitTicks;

    private DevF06ReloadInterruptClientAutomation() { }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        stage = 0;
        waitTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || stage != 0) return;
        var stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof ModularGunItem)
                || !stack.getOrCreateTag().getBoolean("taczintetra_dev_modules_ready")
                || !stack.getOrCreateTag().getBoolean("taczintetra_dev_f06_reload_probe_ready")
                || ((ModularGunItem) stack.getItem()).getCurrentAmmoCount(stack) != 1) {
            if (++waitTicks == 200) {
                LOGGER.error("F06_RELOAD_INTERRUPT_CLIENT_TIMEOUT waiting for server-seeded one-round modular gun");
                stage = 2;
            }
            return;
        }

        int identity = StackIdentity.of(stack);
        int sequence = com.pycoder.taczintetra.runtime.ReloadSequence.next();
        ModNetwork.CHANNEL.sendToServer(new ReloadIntentMessage(false, false, identity, sequence));
        stage = 1;
        LOGGER.info("F06_RELOAD_INTERRUPT_CLIENT_SENT identity={} sequence={} ammo={}",
                identity, sequence, ((ModularGunItem) stack.getItem()).getCurrentAmmoCount(stack));
    }
}
