package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.compat.ModularGunLifecycleAdapter;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.shooter.LivingEntityDrawGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(LivingEntityShoot.class)
public abstract class LivingEntityShootMixin {
    @Shadow(remap = false) @Final private LivingEntity shooter;
    @Shadow(remap = false) @Final private ShooterDataHolder data;
    @Shadow(remap = false) @Final private LivingEntityDrawGun draw;

    @Inject(method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp, float charge,
                                   boolean charged, CallbackInfoReturnable<ShootResult> cir) {
        ItemStack stack = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        if (ModularGunLifecycleAdapter.isModular(stack)) {
            if (!ModularGunLifecycleAdapter.acceptsNetworkTimestamp(data, shooter, timestamp)) {
                cir.setReturnValue(ShootResult.NETWORK_FAIL);
                return;
            }
            if (draw.getDrawCoolDown() != 0L) {
                cir.setReturnValue(ShootResult.IS_DRAWING);
                return;
            }
            if (((IModularItem) stack.getItem()).isBroken(stack)) {
                cir.setReturnValue(ShootResult.UNKNOWN_FAIL);
                return;
            }
            if (((ModularGunItem) stack.getItem()).isOverheatLocked(stack)) {
                cir.setReturnValue(ShootResult.OVERHEATED);
                return;
            }
            if (ModularGunLifecycleAdapter.shoot(data, stack, pitch, yaw, shooter, timestamp)) {
                cir.setReturnValue(ShootResult.SUCCESS);
            } else {
                cir.setReturnValue(ShootResult.FORGE_EVENT_CANCEL);
            }
        }
    }
}
