package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.compat.ModularGunLifecycleAdapter;
import com.tacz.guns.entity.shooter.LivingEntityMelee;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityMelee.class)
public abstract class LivingEntityMeleeMixin {
    @Shadow(remap = false) @Final private LivingEntity shooter;
    @Shadow(remap = false) @Final private ShooterDataHolder data;

    @Inject(method = "melee", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$melee(CallbackInfo ci) {
        ItemStack stack = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            ModularGunLifecycleAdapter.melee(data, shooter, stack);
            ci.cancel();
        }
    }
}
