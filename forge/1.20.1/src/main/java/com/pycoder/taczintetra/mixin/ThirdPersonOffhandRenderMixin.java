package com.pycoder.taczintetra.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.runtime.GunInputRouter;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.slf4j.Logger;

/** TaCZ 动画渲染器的第一人称/副手适配；第一人称几何渲染仍由 TaCZ 原生处理。 */
@Mixin(AnimateGeoItemRenderer.class)
public abstract class ThirdPersonOffhandRenderMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loggedRender;
    private static boolean loggedFirstPersonRender;
    private static boolean loggedOffhandBlockDecision;
    private static boolean loggedFirstPersonOffhandEntry;

    @Invoker(value = "renderFirstPerson", remap = false)
    protected abstract void taczintetra$renderFirstPerson(LocalPlayer player, ItemStack stack,
                                                           ItemDisplayContext context, PoseStack poseStack,
                                                           MultiBufferSource buffers, int light,
                                                           float partialTick);

    @Inject(method = "blockOffhandRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$allowDualPistolOffhandRender(CallbackInfoReturnable<Boolean> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        boolean allowOffhand = taczintetra$isDualPistolLoadout(player);
        if (!loggedOffhandBlockDecision && Boolean.getBoolean("taczintetra.dev_automation")) {
            loggedOffhandBlockDecision = true;
            LOGGER.info("TaCZinTetra dev off-hand render gate: main={}, offhand={}, allowOffhand={}",
                    player.getMainHandItem().getItem(), player.getOffhandItem().getItem(), allowOffhand);
        }
        if (allowOffhand) {
            // TaCZ 的 FirstPersonRenderHandler 会在此返回 false 后继续调用其
            // vanilla offhand ItemStackRenderer。AnimateGeoItemRenderer 的该路径会
            // 跳过第一人称，因此下方 renderByItem 注入再转回 TaCZ 原生 API。
            callback.setReturnValue(false);
        }
    }

    private static boolean taczintetra$isDualPistolLoadout(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        boolean mainGun = main.getItem() instanceof ModularGunItem;
        boolean offhandGun = offhand.getItem() instanceof ModularGunItem;
        boolean mainTwoHanded = mainGun
                && TetraItemStackProfileResolver.isTwoHanded((ModularGunItem) main.getItem(), main);
        return GunInputRouter.offhandShootAllowed(mainGun, offhandGun, mainTwoHanded);
    }

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
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && !loggedFirstPersonOffhandEntry
                && Boolean.getBoolean("taczintetra.dev_automation")) {
            loggedFirstPersonOffhandEntry = true;
            LOGGER.info("TaCZinTetra dev off-hand renderByItem entry: item={}", stack.getItem());
        }
        if (!(stack.getItem() instanceof ModularGunItem)) return;

        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || !taczintetra$isDualPistolLoadout(player)) return;

            taczintetra$renderFirstPerson(
                    player, stack, context, poseStack, buffers, light, minecraft.getFrameTime());
            if (!loggedFirstPersonRender && Boolean.getBoolean("taczintetra.dev_automation")) {
                loggedFirstPersonRender = true;
                LOGGER.info("TaCZinTetra dev native first-person off-hand renderer invoked: item={}, context={}",
                        stack.getItem(), context);
            }
            callback.cancel();
            return;
        }

        if (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return;

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
