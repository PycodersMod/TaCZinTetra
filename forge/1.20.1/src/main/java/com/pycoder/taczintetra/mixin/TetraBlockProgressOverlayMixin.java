package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.items.modular.impl.BlockProgressOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;

/** Suppresses Tetra's secondary block-progress bar while a modular firearm is held in either hand. */
@Mixin(value = BlockProgressOverlay.class, remap = false)
public abstract class TetraBlockProgressOverlayMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean overlayProbeLogged;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$hideModularGunProgressBar(ForgeGui gui, GuiGraphics graphics,
                                                        float partialTick, int screenWidth,
                                                        int screenHeight, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var mainHand = player == null ? net.minecraft.world.item.ItemStack.EMPTY : player.getMainHandItem();
        var offHand = player == null ? net.minecraft.world.item.ItemStack.EMPTY : player.getOffhandItem();
        boolean mainGun = mainHand.getItem() instanceof ModularGunItem;
        boolean offGun = offHand.getItem() instanceof ModularGunItem;
        boolean shouldCancel = player != null
                && (player.getMainHandItem().getItem() instanceof ModularGunItem
                || player.getOffhandItem().getItem() instanceof ModularGunItem);

        if ((mainGun || offGun) && Boolean.getBoolean("taczintetra.overlay_probe") && !overlayProbeLogged) {
            overlayProbeLogged = true;
            LOGGER.info("TETRA_BLOCK_PROGRESS_PROBE callback=render playerPresent={} mainHandId={} mainModularGun={} offHandId={} offModularGun={} cancel={}",
                    player != null, BuiltInRegistries.ITEM.getKey(mainHand.getItem()), mainGun,
                    BuiltInRegistries.ITEM.getKey(offHand.getItem()), offGun, shouldCancel);
        }

        if (shouldCancel) {
            callback.cancel();
        }
    }
}
