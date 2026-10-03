package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.client.event.RenderCrosshairEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 让模块化枪械使用 TaCZ 的普通圆点准星纹理，而非线条/标尺纹理。 */
@Mixin(value = RenderCrosshairEvent.class, remap = false)
public abstract class RenderCrosshairMixin {
    @Redirect(method = "renderCrosshair", remap = false, at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/renderer/crosshair/CrosshairType;getTextureLocation(Lcom/tacz/guns/client/renderer/crosshair/CrosshairType;)Lnet/minecraft/resources/ResourceLocation;", remap = false))
    private static net.minecraft.resources.ResourceLocation taczintetra$useDotForModularGun(CrosshairType selected) {
        if (TaczInTetraForgeConfig.FORCE_DOT_CROSSHAIR.get()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null
                    && (minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem
                    || minecraft.player.getOffhandItem().getItem() instanceof ModularGunItem)) {
                return CrosshairType.getTextureLocation(CrosshairType.DOT_1);
            }
        }
        return CrosshairType.getTextureLocation(selected);
    }
}
