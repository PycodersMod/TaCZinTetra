package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.compat.ModularGunLifecycleAdapter;
import com.tacz.guns.entity.shooter.LivingEntityBolt;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityBolt.class)
public abstract class LivingEntityBoltMixin {
    @Shadow(remap = false) @Final private LivingEntity shooter;
    @Shadow(remap = false) @Final private ShooterDataHolder data;

    @Inject(method = "bolt", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$bolt(CallbackInfo ci) {
        ItemStack stack = currentStack();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            data.isBolting = ModularGunLifecycleAdapter.startBolt(data, stack, shooter);
            ci.cancel();
        }
    }

    @Inject(method = "tickBolt", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$tickBolt(CallbackInfo ci) {
        ItemStack stack = currentStack();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            data.isBolting = ModularGunLifecycleAdapter.tickBolt(data, stack, shooter);
            ci.cancel();
        }
    }

    private ItemStack currentStack() {
        return data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
    }
}
