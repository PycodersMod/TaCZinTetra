package com.pycoder.taczintetra.mixin;

import com.mojang.realmsclient.client.RealmsClient;
import com.pycoder.taczintetra.client.DevQuickPlayTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records whether Minecraft selected QuickPlay as its initial-screen path. */
@Mixin(Minecraft.class)
public abstract class MinecraftQuickPlayTraceMixin {
    @Inject(method = {"setInitialScreen", "m_278684_"}, at = @At("HEAD"), remap = false)
    private void taczintetra$traceInitialScreen(RealmsClient realmsClient,
            ReloadInstance reloadInstance, GameConfig.QuickPlayData quickPlayData,
            CallbackInfo callback) {
        if (!DevQuickPlayTrace.isEnabled()) return;
        String multiplayerTarget = quickPlayData.multiplayer();
        DevQuickPlayTrace.record("INITIAL_SCREEN_SELECTED",
                "quickPlayEnabled=" + quickPlayData.isEnabled()
                        + ", multiplayerTargetPresent=" + (multiplayerTarget != null && !multiplayerTarget.isBlank())
                        + ", reloadDone=" + reloadInstance.done().isDone());
    }
}
