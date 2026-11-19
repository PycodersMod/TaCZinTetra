package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.api.GunSourceAccessor;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.logic.ConfiguredProjectilePolicy;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

/** 将 Tetra 枪械实例属性桥接到 TaCZ 原生弹丸与属性回调。 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin implements GunSourceAccessor {
    @Unique
    private ItemStack taczintetra$sourceGun = ItemStack.EMPTY;
    @Shadow private boolean explosion;
    @Shadow private float explosionRadius;
    @Shadow private float explosionDamage;
    @Shadow private int life;
    @Shadow private float gravity;
    @Shadow private float friction;
    @Shadow private boolean igniteEntity;
    @Shadow private boolean igniteBlock;

    @WrapOperation(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;modifyProperty(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object taczintetra$captureSourceGun(EntityKineticBullet instance, String propertyName, Class<?> type,
                                                Object defaultValue, Operation<Object> original,
                                                @Local(argsOnly = true) ItemStack gunStack) {
        taczintetra$sourceGun = gunStack == null ? ItemStack.EMPTY : gunStack.copy();
        return original.call(instance, propertyName, type, defaultValue);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At("TAIL"), remap = false)
    private void taczintetra$applyConfiguredDamage(EntityType<?> type, Level level, LivingEntity owner,
                                                    ItemStack gunStack, ResourceLocation gunId,
                                                    ResourceLocation gunDisplayId, ResourceLocation ammoId,
                                                    boolean tracer, GunData gunData, BulletData bulletData,
                                                    CallbackInfo ci) {
        if (gunStack == null || !(gunStack.getItem() instanceof ModularGunItem modular)) {
            return;
        }
        var profile = TetraItemStackProfileResolver.tryResolve(modular, gunStack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null || bulletData == null || !Float.isFinite(bulletData.getDamageAmount())
                || bulletData.getDamageAmount() <= 0) {
            return;
        }
        float baseDamage = bulletData.getDamageAmount();
        float configuredDamage = (float) profile.projectile().damage();
        if (Float.isFinite(configuredDamage) && configuredDamage > 0) {
            ((EntityKineticBullet) (Object) this).setShotDamageMultiplier(configuredDamage / baseDamage);
        }
        var modules = TetraItemStackProfileResolver.selectedModules(modular, gunStack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        String barrelId = modules == null ? null : modules.barrel().variantId();
        float configuredExplosionRadius = ConfiguredProjectilePolicy.explosionRadius(
                TaCZinTetra.MODULE_CONFIG, barrelId);
        var motion = ConfiguredProjectilePolicy.motionOverrides(TaCZinTetra.MODULE_CONFIG, barrelId);
        if (Float.isFinite(motion.lifeSeconds()) && motion.lifeSeconds() > 0) {
            double ticks = motion.lifeSeconds() * 20.0;
            life = (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(ticks)));
        }
        if (Float.isFinite(motion.gravity())) gravity = motion.gravity();
        if (Float.isFinite(motion.friction())) friction = motion.friction();
        if (motion.ignite()) {
            igniteEntity = true;
            igniteBlock = true;
        }
        explosion = configuredExplosionRadius > 0;
        if (explosion) {
            explosionRadius = configuredExplosionRadius;
            explosionDamage = configuredDamage > 0 ? configuredDamage : baseDamage;
        }
        if (Boolean.getBoolean("taczintetra.dev_automation")) {
            com.mojang.logging.LogUtils.getLogger().info(
                    "TaCZinTetra dev projectile profile bridge: specialInlays={}, tag={}, baseDamage={}, configuredDamage={}, multiplier={}",
                    TetraItemStackProfileResolver.specialTraits(modular, gunStack), gunStack.getTag(),
                    baseDamage, configuredDamage, configuredDamage / baseDamage);
        }
    }

    @Inject(method = "modifyProperty(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void taczintetra$dispatchNativeProperty(String propertyName, Class<T> type, T defaultValue,
                                                          CallbackInfoReturnable<T> cir) {
        ItemStack source = taczintetra$sourceGun;
        if (!(source.getItem() instanceof ModularGunItem gun)
                || !(((EntityKineticBullet) (Object) this).getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        var holder = IGunOperator.fromLivingEntity(shooter).getDataHolder();
        cir.setReturnValue(gun.modifyProperty(holder, source, shooter, propertyName, type, defaultValue));
    }

    @Override
    public ItemStack sourceGun() {
        return taczintetra$sourceGun.isEmpty() ? ItemStack.EMPTY : taczintetra$sourceGun.copy();
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), remap = false)
    private void taczintetra$observeEntityHit(TacHitResult hit, Vec3 start, Vec3 end, CallbackInfo ci) {
        if (Boolean.getBoolean("taczintetra.dev_automation") && hit.getEntity() != null) {
            com.mojang.logging.LogUtils.getLogger().info(
                    "TaCZinTetra dev bullet entity hit: entity={}, start={}, end={}, hit={}",
                    hit.getEntity(), start, end, hit.getLocation());
        }
    }

}
