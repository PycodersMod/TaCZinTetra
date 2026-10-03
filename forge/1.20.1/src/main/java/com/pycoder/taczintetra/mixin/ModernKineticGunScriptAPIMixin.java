package com.pycoder.taczintetra.mixin;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.init.ModItems;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** 物品栈由 Tetra 驱动时，提供 TaCZ 原生实现的委托。 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow(remap = false) private AbstractGunItem abstractGunItem;
    @Shadow(remap = false) private net.minecraft.world.entity.LivingEntity shooter;
    @Shadow(remap = false) private net.minecraft.world.item.ItemStack itemStack;

    @Inject(method = "setItemStack", at = @At("TAIL"), remap = false)
    private void taczintetra$useRegisteredDelegate(CallbackInfo ci) {
        if (abstractGunItem == null) {
            abstractGunItem = ModItems.MODERN_KINETIC_GUN.get();
        }
    }

    @Inject(method = "shootOnce", at = @At("HEAD"), remap = false)
    private void taczintetra$observeShootOnce(boolean consumeAmmo, CallbackInfo ci) {
        if (Boolean.getBoolean("taczintetra.dev_automation")) {
            LOGGER.info("TaCZinTetra dev shootOnce entered: consumeAmmo={}, stackEqualsMainHand={}, cachePresent={}, abstractGunPresent={}",
                    consumeAmmo, shooter != null && shooter.getMainHandItem().equals(itemStack),
                    shooter != null && com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(shooter).getCacheProperty() != null,
                    abstractGunItem != null);
        }
    }

}
