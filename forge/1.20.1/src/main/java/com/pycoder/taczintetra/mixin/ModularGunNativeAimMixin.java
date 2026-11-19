package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.TaczNativeAimRoutePolicy;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.AimKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets TaCZ's own AimKey handler accept Tetra modular guns while preserving TaCZ aim semantics. */
@Mixin(value = AimKey.class, remap = false)
public abstract class ModularGunNativeAimMixin {
    @Redirect(
            method = "onAimPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/api/item/IGun;mainHandHoldGun(Lnet/minecraft/world/entity/LivingEntity;)Z",
                    remap = false),
            remap = false)
    private static boolean taczintetra$allowTetraModularGunAim(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        boolean modularGun = mainHand.getItem() instanceof ModularGunItem gun;
        boolean offHandModularGun = entity.getOffhandItem().getItem() instanceof ModularGunItem;
        boolean mainHandTwoHanded = modularGun
                && TetraItemStackProfileResolver.isTwoHanded((ModularGunItem) mainHand.getItem(), mainHand);
        return TaczNativeAimRoutePolicy.allowMainHandAim(
                IGun.mainHandHoldGun(entity), modularGun, offHandModularGun, mainHandTwoHanded);
    }
}
