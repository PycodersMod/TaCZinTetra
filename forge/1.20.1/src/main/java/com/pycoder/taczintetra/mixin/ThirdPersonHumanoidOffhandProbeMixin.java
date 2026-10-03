package com.pycoder.taczintetra.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;

/** 仅供开发使用的证据钩子，用于确认 TaCZ 的真实人形副手渲染器能识别模组枪械。 */
@Mixin(HumanoidOffhandRender.class)
public abstract class ThirdPersonHumanoidOffhandProbeMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean logged;

    @Inject(method = "renderGun", at = @At("HEAD"), remap = false)
    private static void taczintetra$probe(LivingEntity entity, PoseStack poseStack,
                                          MultiBufferSource buffers, int packedLight, CallbackInfo callback) {
        if (logged || !Boolean.getBoolean("taczintetra.dev_automation")
                || !(entity.getOffhandItem().getItem() instanceof ModularGunItem)) return;
        logged = true;
        LOGGER.info("TaCZinTetra dev HumanoidOffhandRender invoked for modular off-hand item={}",
                entity.getOffhandItem().getItem());
    }
}
