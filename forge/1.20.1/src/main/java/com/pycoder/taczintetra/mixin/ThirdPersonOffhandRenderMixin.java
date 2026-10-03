package com.pycoder.taczintetra.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;

/** 为渲染器跳过的第三人称左手重新进入 TaCZ 模型渲染流程。 */
@Mixin(GunItemRendererWrapper.class)
public abstract class ThirdPersonOffhandRenderMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loggedRender;

    /**
     * TaCZ 在 userdev 环境中已反混淆，但发布包中的渲染器使用
     * Minecraft 混淆方法名。这里同时保留两种名称，
     * 使客户端 mixin 在两种启动模式下都有效。
     */
    @Inject(method = {"renderByItem", "m_108829_"}, at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private void taczintetra$renderThirdPersonOffhand(ItemStack stack, ItemDisplayContext context,
                                                       PoseStack poseStack, MultiBufferSource buffers,
                                                       int light, int overlay, CallbackInfo callback) {
        if (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || !(stack.getItem() instanceof ModularGunItem)) return;

        boolean rendered = TimelessAPI.getGunDisplay(stack).map(display -> {
            BedrockGunModel model = display.getGunModel();
            if (model == null || display.getModelTexture() == null) return false;
            poseStack.pushPose();
            Vector3f scale = display.getTransform().getScale().getThirdPerson();
            if (scale != null) poseStack.scale(scale.x(), scale.y(), scale.z());
            model.render(poseStack, stack, context,
                    RenderType.entityCutout(display.getModelTexture()), light, overlay);
            poseStack.popPose();
            if (!loggedRender && Boolean.getBoolean("taczintetra.dev_automation")) {
                loggedRender = true;
                LOGGER.info("TaCZinTetra dev third-person off-hand render invoked: item={}, display={}",
                        stack.getItem(), display.getModelTexture());
            }
            return true;
        }).orElse(false);
        if (rendered) callback.cancel();
    }
}
