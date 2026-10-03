package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.api.GunSourceAccessor;
import com.pycoder.taczintetra.api.GunRuntimeEvent;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.logic.ConfiguredProjectilePolicy;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 仅对配置为原生爆炸行为的 TaCZ 子弹触发附属模组钩子。 */
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
    @Shadow private float speed;
    @Shadow private float distanceAmount;
    @Shadow private float armorIgnore;
    @Shadow private int pierce;
    @Shadow private float knockback;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At("TAIL"), remap = false)
    private void taczintetra$applyConfiguredDamage(EntityType<?> type, Level level, LivingEntity owner,
                                                    ItemStack gunStack, ResourceLocation gunId,
                                                    ResourceLocation gunDisplayId, ResourceLocation ammoId,
                                                    boolean tracer, GunData gunData, BulletData bulletData,
                                                    CallbackInfo ci) {
        taczintetra$sourceGun = gunStack == null ? ItemStack.EMPTY : gunStack.copy();
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
        var projectile = profile.projectile();
        if (Float.isFinite((float) projectile.velocity()) && projectile.velocity() > 0) {
            speed *= (float) projectile.velocity();
        }
        if (Float.isFinite((float) projectile.range()) && projectile.range() > 0) {
            distanceAmount *= (float) projectile.range();
        }
        if (Float.isFinite((float) projectile.armorIgnore())) {
            armorIgnore = Math.max(0, Math.min(1, armorIgnore + (float) projectile.armorIgnore()));
        }
        if (projectile.pierce() > 0) {
            pierce = Math.max(1, projectile.pierce());
        }
        if (Float.isFinite((float) projectile.knockback()) && projectile.knockback() > 0) {
            knockback *= (float) projectile.knockback();
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
                    "TaCZinTetra dev projectile profile bridge: specialInlays={}, tag={}, baseDamage={}, configuredDamage={}, multiplier={}, speed={}, range={}, armorIgnore={}, pierce={}, knockback={}",
                    TetraItemStackProfileResolver.specialTraits(modular, gunStack), gunStack.getTag(),
                    baseDamage, configuredDamage, configuredDamage / baseDamage, speed, distanceAmount,
                    armorIgnore, pierce, knockback);
        }
    }

    /**
     * 将配置的弹丸伤害视为每次命中的伤害值。TaCZ
     * 原生返回值已包含距离伤害曲线；若再将其作为乘数，
     * JSON 伤害就会错误地依赖模板枪械。
     */
    @Inject(method = "getDamage(Lnet/minecraft/world/phys/Vec3;)F", at = @At("RETURN"),
            cancellable = true, remap = false)
    private void taczintetra$useConfiguredPerHitDamage(Vec3 hitPosition,
                                                        CallbackInfoReturnable<Float> ci) {
        ItemStack gunStack = sourceGun();
        if (!(gunStack.getItem() instanceof ModularGunItem modular)) return;
        var profile = TetraItemStackProfileResolver.tryResolve(modular, gunStack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null) return;
        float configuredDamage = (float) profile.projectile().damage();
        if (Float.isFinite(configuredDamage) && configuredDamage > 0) {
            ci.setReturnValue((float) profile.projectile().damage());
        }
    }

    @Override
    public ItemStack sourceGun() {
        return taczintetra$sourceGun.isEmpty() ? ItemStack.EMPTY : taczintetra$sourceGun.copy();
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void taczintetra$explosionHit(BlockHitResult hit, Vec3 start, Vec3 end, CallbackInfo ci) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        ItemStack sourceGun = sourceGun();
        if (Boolean.getBoolean("taczintetra.dev_automation")
                && owner instanceof LivingEntity actor
                && sourceGun.getItem() instanceof ModularGunItem) {
            com.mojang.logging.LogUtils.getLogger().info(
                    "TaCZinTetra dev modular bullet block hit: block={}, location={}, start={}, end={}, projectileId={}",
                    hit.getBlockPos(), hit.getLocation(), start, end, ((Entity) (Object) this).getId());
        }
        if (!explosion || !(owner instanceof LivingEntity actor)
                || !(sourceGun.getItem() instanceof ModularGunItem)) {
            return;
        }
        if (MinecraftForge.EVENT_BUS.post(new GunRuntimeEvent(
                GunRuntimeEvent.Type.EXPLOSION_HIT, actor, sourceGun))) {
            ci.cancel();
        }
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
