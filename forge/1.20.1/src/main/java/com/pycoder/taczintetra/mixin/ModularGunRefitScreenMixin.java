package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.client.GunRefitScreenState;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TaCZinTetra 的配件由 Tetra 管理，因此隐藏 TaCZ 改装页中的原生配件槽按钮。 */
@Mixin(GunRefitScreen.class)
public abstract class ModularGunRefitScreenMixin implements GunRefitScreenState {
    @Unique
    private static final Logger TACZINTETRA_LOGGER = LogUtils.getLogger();
    @Shadow(remap = false)
    private static boolean HIDE_GUN_PROPERTY_DIAGRAMS;

    @Unique
    private boolean taczintetra$previewProbeCaptured;

    @Unique
    private boolean taczintetra$propertyVisibilityCaptured;

    @Unique
    private boolean taczintetra$originalPropertyDiagramsHidden;

    // TaCZ's packaged 1.20.1 screen overrides the mapped render method as m_88315_.
    // Injecting init fails because GunRefitScreen does not declare that inherited method.
    @Inject(method = "m_88315_", at = @At("HEAD"), remap = false)
    private void taczintetra$showNativePropertyDiagramsForModularGun(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem) {
            if (!taczintetra$propertyVisibilityCaptured) {
                taczintetra$originalPropertyDiagramsHidden = HIDE_GUN_PROPERTY_DIAGRAMS;
                taczintetra$propertyVisibilityCaptured = true;
            }
            // TaCZ 的原生 render 在该开关为 false 时绘制属性图。
            HIDE_GUN_PROPERTY_DIAGRAMS = false;
        }
    }

    @Inject(method = "m_88315_", at = @At("TAIL"), remap = false)
    private void taczintetra$renderNativeGunPreview(GuiGraphics graphics, int mouseX, int mouseY,
                                                     float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem)) {
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        // GuiGraphics delegates to Minecraft's ItemRenderer. TaCZ's registered gun renderer
        // therefore resolves the active body display/model/texture from this exact ItemStack.
        int previewCenterX = Math.max(24, graphics.guiWidth() - 97);
        int previewCenterY = Math.max(24, graphics.guiHeight() / 2 - 20);
        graphics.pose().pushPose();
        graphics.pose().translate(previewCenterX, previewCenterY, 200.0F);
        graphics.pose().scale(48.0F, 48.0F, 1.0F);
        var nativeRenderer = IClientItemExtensions.of(stack).getCustomRenderer();
        if (nativeRenderer == null) {
            graphics.pose().popPose();
            return;
        }
        nativeRenderer.renderByItem(stack, ItemDisplayContext.FIXED, graphics.pose(),
                graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
        graphics.pose().popPose();
        if (!taczintetra$previewProbeCaptured
                && Boolean.getBoolean("taczintetra.refit_screen_screenshot_probe")) {
            taczintetra$previewProbeCaptured = true;
            var display = TimelessAPI.getGunDisplay(stack).orElse(null);
            var gunData = (GunItemDataAccessor) stack.getItem();
            TACZINTETRA_LOGGER.info("TaCZinTetra refit preview evidence item={} renderer={} gunId={} displayId={} displayResolved={} modelResolved={} texture={}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()), nativeRenderer.getClass().getName(),
                    gunData.getGunId(stack), gunData.getGunDisplayId(stack), display != null,
                    display != null && display.getGunModel() != null,
                    display == null ? null : display.getModelTexture());
            Screenshot.grab(minecraft.gameDirectory, "taczintetra-refit-native-render-probe",
                    minecraft.getMainRenderTarget(), ignored -> { });
        }
    }

    @Override
    public void taczintetra$restorePropertyVisibility() {
        if (taczintetra$propertyVisibilityCaptured) {
            HIDE_GUN_PROPERTY_DIAGRAMS = taczintetra$originalPropertyDiagramsHidden;
            taczintetra$propertyVisibilityCaptured = false;
        }
    }

    @Inject(method = "addAttachmentTypeButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$hideAttachmentSlotsForTetraGun(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem) {
            ci.cancel();
        }
    }

    @Inject(method = "addInventoryAttachmentButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$hideInventoryAttachmentSlotsForTetraGun(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem) {
            ci.cancel();
        }
    }
}
