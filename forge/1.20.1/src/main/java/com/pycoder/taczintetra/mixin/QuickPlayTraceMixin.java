package com.pycoder.taczintetra.mixin;

import com.mojang.realmsclient.client.RealmsClient;
import com.pycoder.taczintetra.client.DevQuickPlayTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opt-in diagnostics for the asynchronous vanilla QuickPlay callback. */
@Mixin(QuickPlay.class)
public abstract class QuickPlayTraceMixin {
    @Inject(method = {"connect", "m_278613_"}, at = @At("HEAD"), remap = false)
    private static void taczintetra$traceQuickPlayConnect(Minecraft minecraft,
            GameConfig.QuickPlayData quickPlayData, ReloadInstance reloadInstance,
            RealmsClient realmsClient, CallbackInfo callback) {
        if (!DevQuickPlayTrace.isEnabled()) return;
        String multiplayerTarget = quickPlayData.multiplayer();
        DevQuickPlayTrace.record("QUICKPLAY_CONNECT_INVOKED",
                "enabled=" + quickPlayData.isEnabled()
                        + ", multiplayerTargetPresent=" + (multiplayerTarget != null && !multiplayerTarget.isBlank())
                        + ", reloadDone=" + reloadInstance.done().isDone());
        reloadInstance.done().whenComplete((ignored, failure) -> {
            if (failure == null) {
                DevQuickPlayTrace.record("RESOURCE_RELOAD_COMPLETED", "success=true");
            } else {
                DevQuickPlayTrace.record("RESOURCE_RELOAD_COMPLETED",
                        "success=false, failure=" + failure.getClass().getName());
            }
        });
    }

    @Inject(method = {"joinMultiplayerWorld", "m_278767_"}, at = @At("HEAD"), remap = false)
    private static void taczintetra$traceMultiplayerCallback(Minecraft minecraft,
            String address, CallbackInfo callback) {
        if (!DevQuickPlayTrace.isEnabled()) return;
        DevQuickPlayTrace.record("JOIN_MULTIPLAYER_CALLBACK", "targetPresent=" + !address.isBlank());
    }
}
