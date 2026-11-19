package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.client.input.ReloadKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps only the custom Shift+R refill route from duplicating TaCZ's native reload key. */
@Mixin(value = ReloadKey.class, remap = false)
public abstract class MainHandReloadInputMixin {
    @Inject(method = "onReloadPress", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczintetra$routeMainHandReload(InputEvent.Key event, CallbackInfo callback) {
        if (event.getAction() != GLFW.GLFW_PRESS
                || !ReloadKey.RELOAD_KEY.matches(event.getKey(), event.getScanCode())
                || Screen.hasControlDown()
                || !Screen.hasShiftDown()) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof ModularGunItem) {
            callback.cancel();
        }
    }
}
