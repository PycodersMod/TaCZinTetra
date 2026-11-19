package com.pycoder.taczintetra.client;

import com.pycoder.taczintetra.TaCZinTetra;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observes screen transitions only while the QuickPlay trace property is enabled. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevQuickPlayScreenTrace {
    private DevQuickPlayScreenTrace() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!DevQuickPlayTrace.isEnabled()) return;
        if (event.getScreen() instanceof ConnectScreen) {
            DevQuickPlayTrace.record("CONNECT_SCREEN_OPENING", "screen=" + event.getScreen().getClass().getName());
            return;
        }
        DevQuickPlayTrace.record("SCREEN_OPENING", "screen=" + event.getScreen().getClass().getName());
    }
}
