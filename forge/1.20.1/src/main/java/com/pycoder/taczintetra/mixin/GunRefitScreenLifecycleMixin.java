package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.client.GunRefitScreenState;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores TaCZ's global refit preference when a refit screen is replaced or closed. */
@Mixin(Screen.class)
public abstract class GunRefitScreenLifecycleMixin {
    @Inject(method = {"removed", "m_7861_"}, at = @At("HEAD"), remap = false)
    private void taczintetra$restoreRefitPropertyVisibility(CallbackInfo ci) {
        if ((Object) this instanceof GunRefitScreenState refitScreen) {
            refitScreen.taczintetra$restorePropertyVisibility();
        }
    }
}
