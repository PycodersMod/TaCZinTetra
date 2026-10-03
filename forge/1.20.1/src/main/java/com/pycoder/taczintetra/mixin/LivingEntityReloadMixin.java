package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.compat.ModularGunLifecycleAdapter;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.shooter.LivingEntityReload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityReload.class)
public abstract class LivingEntityReloadMixin {
    @Shadow(remap = false) @Final private LivingEntity shooter;
    @Shadow(remap = false) @Final private ShooterDataHolder data;

    @Inject(method = "reload", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$reload(CallbackInfo ci) {
        ItemStack stack = currentStack();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                com.mojang.logging.LogUtils.getLogger().info(
                        "TaCZinTetra dev modular reload entered: hand={}, ammo={}, tag={}, dataState={}",
                        shooter.getMainHandItem().equals(stack) ? "MAIN_HAND" : "OFF_HAND",
                        ((com.pycoder.taczintetra.item.ModularGunItem) stack.getItem()).getCurrentAmmoCount(stack),
                        stack.getTag(), data.reloadStateType);
            }
            ModularGunLifecycleAdapter.startReload(data, stack, shooter);
            ci.cancel();
        }
    }

    @Inject(method = "cancelReload", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$cancelReload(CallbackInfo ci) {
        ItemStack stack = currentStack();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            ModularGunLifecycleAdapter.interruptReload(data, stack, shooter);
            ci.cancel();
        }
    }

    @Inject(method = "tickReloadState", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$tickReload(CallbackInfoReturnable<ReloadState> cir) {
        ItemStack stack = currentStack();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            cir.setReturnValue(ModularGunLifecycleAdapter.tickReload(data, stack, shooter));
        }
    }

    private ItemStack currentStack() {
        return data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
    }
}
