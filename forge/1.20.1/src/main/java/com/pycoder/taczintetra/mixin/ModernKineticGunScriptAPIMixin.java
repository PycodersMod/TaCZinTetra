package com.pycoder.taczintetra.mixin;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.api.util.LuaNbtAccessor;
import net.minecraft.resources.ResourceLocation;
import com.pycoder.taczintetra.item.ModularGunItem;
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
    @Shadow(remap = false) private CommonGunIndex gunIndex;
    @Shadow(remap = false) private ResourceLocation gunId;
    @Shadow(remap = false) private ResourceLocation gunDisplayId;
    @Shadow(remap = false) private LuaNbtAccessor nbtUtil;

    @Inject(method = "setItemStack", at = @At("TAIL"), remap = false)
    private void taczintetra$useRegisteredDelegate(CallbackInfo ci) {
        if (itemStack == null || !(itemStack.getItem() instanceof ModularGunItem modularGun)) return;
        // TaCZ initGunItem() only accepts AbstractGunItem, so it clears its
        // index for Tetra-backed stacks. Reproduce its exact field binding
        // through TaCZ's own registry and NBT APIs for this one item type.
        abstractGunItem = ModItems.MODERN_KINETIC_GUN.get();
        gunId = modularGun.getGunId(itemStack);
        gunDisplayId = modularGun.getGunDisplayId(itemStack);
        gunIndex = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        nbtUtil = itemStack.hasTag() ? new LuaNbtAccessor(itemStack.getTag()) : null;
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
