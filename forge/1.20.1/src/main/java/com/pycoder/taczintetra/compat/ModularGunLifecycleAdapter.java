package com.pycoder.taczintetra.compat;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.api.GunRuntimeEvent;
import net.minecraftforge.common.MinecraftForge;
import com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService;
import com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator;
import com.pycoder.taczintetra.runtime.GunCacheSynchronizer;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.logic.ConfiguredAmmoPolicy;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.logic.ConfiguredProjectilePolicy;
import com.pycoder.taczintetra.logic.ReloadTimingPolicy;
import com.pycoder.taczintetra.logic.ReloadStartPolicy;
import com.mojang.logging.LogUtils;
import net.minecraft.world.InteractionHand;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.pojo.data.gun.GunReloadData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import se.mickelus.tetra.items.modular.IModularItem;
import com.pycoder.taczintetra.logic.DurabilityCostPolicy;
import com.pycoder.taczintetra.logic.FireCooldownPolicy;
import com.pycoder.taczintetra.logic.ShotSettlementPolicy;
import com.pycoder.taczintetra.logic.AimInputPolicy;
import com.pycoder.taczintetra.logic.ShootStatePolicy;
import com.pycoder.taczintetra.logic.FireModePolicy;
import com.pycoder.taczintetra.logic.NetworkTimestampPolicy;
import com.pycoder.taczintetra.logic.SpreadPolicy;

import java.util.function.Supplier;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.slf4j.Logger;

/** 将 TaCZ 严格的 AbstractGunItem 生命周期委托给由 Tetra 支持的物品栈。 */
public final class ModularGunLifecycleAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RELOAD_STARTED_AT = "taczintetra_reload_started_at";
    private static final String RELOAD_STATE = "taczintetra_reload_state";
    private static final Map<LivingEntity, EnumMap<InteractionHand, Long>> LAST_SHOTS = new WeakHashMap<>();
    private ModularGunLifecycleAdapter() {
    }

    /** 使用 TaCZ 已注册的物品实例；注册表冻结后再构造 Item 会导致服务端崩溃。 */
    private static ModernKineticGunItem delegate() {
        return ModItems.MODERN_KINETIC_GUN.get();
    }

    public static boolean isModular(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ModularGunItem;
    }

    /** 为每个模组枪械入口复现 TaCZ 服务端的时间戳容差规则。 */
    public static boolean acceptsNetworkTimestamp(ShooterDataHolder data, LivingEntity shooter,
                                                  long timestamp) {
        if (data == null || shooter == null) return false;
        double tickDurationMillis = 50.0D;
        var server = shooter.getServer();
        if (server != null && server.tickTimes != null && server.tickTimes.length > 0) {
            int index = Math.floorMod(server.getTickCount(), server.tickTimes.length);
            tickDurationMillis = Math.max(50.0D, server.tickTimes[index] * 1.0E-6D);
        }
        return NetworkTimestampPolicy.accepts(System.currentTimeMillis(), data.baseTimestamp,
                timestamp, tickDurationMillis);
    }

    public static boolean shoot(ShooterDataHolder data, ItemStack stack,
                             Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter) {
        return shoot(data, stack, pitch, yaw, shooter, System.currentTimeMillis());
    }

    public static boolean shoot(ShooterDataHolder data, ItemStack stack,
                             Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter,
                             long timestamp) {
        ensureGunId(stack);
        int independentShots = 1;
        if (isModular(stack)) {
            if (!acceptsNetworkTimestamp(data, shooter, timestamp)) return false;
            final float checkedPitch;
            final float checkedYaw;
            try {
                checkedPitch = pitch == null ? Float.NaN : pitch.get();
                checkedYaw = yaw == null ? Float.NaN : yaw.get();
            } catch (RuntimeException invalidAim) {
                return false;
            }
            if (!AimInputPolicy.valid(checkedPitch, checkedYaw)) return false;
            pitch = () -> checkedPitch;
            yaw = () -> checkedYaw;
            boolean nativeReloading = data != null && data.reloadStateType != null
                    && data.reloadStateType.isReloading();
            if (!ShootStatePolicy.allowed(nativeReloading,
                    data != null && data.isBolting,
                    data == null ? Float.NaN : data.sprintTimeS)) return false;
            ModularGunItem gun = (ModularGunItem) stack.getItem();
            var gunIndex = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
            if (gunIndex == null || gunIndex.getGunData() == null
                    || !FireModePolicy.isAllowed(gun.getFireMode(stack),
                    gunIndex.getGunData().getFireModeSet())) return false;
            // 即使未来 TaCZ 入口绕过 LivingEntityShootMixin 直接调用适配器，
            // 适配器也必须保持安全。
            if (gun.isBroken(stack) || gun.isOverheatLocked(stack)) return false;
            InteractionHand hand = ReloadRuntimeCoordinator.handForStack(shooter, stack);
            if (hand != null && (ReloadRuntimeCoordinator.handReloading(shooter, hand)
                    || ReloadRuntimeCoordinator.otherHandReloading(shooter, hand))) {
                return false;
            }
            if (gun.getCurrentAmmoCount(stack) <= 0) return false;
            independentShots = Math.min(gun.independentShots(stack), gun.getCurrentAmmoCount(stack));
            if (independentShots <= 0) return false;
            if (!shooter.level().isClientSide) {
                InteractionHand effectiveHand = hand == null ? InteractionHand.MAIN_HAND : hand;
                long now = System.nanoTime();
                if (!FireCooldownPolicy.ready(now, gun.getRPM(stack), lastShot(shooter, effectiveHand))) {
                    return false;
                }
            }
            GunCacheSynchronizer.ensure(shooter, stack);
            syncNativeAmmo(gun, stack);
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                var index = com.tacz.guns.api.TimelessAPI.getCommonGunIndex(
                        ((ModularGunItem) stack.getItem()).getGunId(stack));
                LOGGER.info("TaCZinTetra dev shoot bridge: cachePresent={}, gunIndexPresent={}, scriptPresent={}, ammo={}, nativeAmmo={}, stackEqualsMainHand={}, mainHandModular={}",
                        com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(shooter).getCacheProperty() != null,
                        index.isPresent(),
                        index.map(ModularGunLifecycleAdapter::hasScript).orElse(false),
                        gun.getCurrentAmmoCount(stack),
                        stack.getOrCreateTag().getInt("AmmoCount"),
                        shooter.getMainHandItem().equals(stack),
                        shooter.getMainHandItem().getItem() instanceof ModularGunItem);
            }
        }
        if (MinecraftForge.EVENT_BUS.post(new GunRuntimeEvent(GunRuntimeEvent.Type.BULLET_FIRE, shooter, stack))) {
            return false;
        }
        if (isModular(stack)) {
            if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(shooter, stack, LogicalSide.SERVER))) {
                return false;
            }
            NetworkHandler.sendToTrackingEntity(
                    new ServerMessageGunShoot(shooter.getId(), stack), shooter);
        }
        int createdProjectiles = -1;
        int requestedProjectiles = -1;
        if (isModular(stack)) {
            ModularGunItem gun = (ModularGunItem) stack.getItem();
            requestedProjectiles = gun.projectileCount(stack, independentShots);
            createdProjectiles = spawnNativeProjectile(data, gun, stack,
                    pitch, yaw, shooter, independentShots);
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev modular projectile batch: count={}, independentShots={}, pelletsPerRoundApprox={}",
                        createdProjectiles, independentShots,
                        Math.max(1, requestedProjectiles / independentShots));
            }
        } else {
            shootThroughNativeScriptApi(data, stack, pitch, yaw, shooter, true);
        }
        if (isModular(stack) && createdProjectiles <= 0) return false;
        int settledShots = isModular(stack)
                ? ShotSettlementPolicy.consumedRounds(independentShots, requestedProjectiles, createdProjectiles)
                : independentShots;
        if (isModular(stack) && settledShots <= 0) return false;
        if (!shooter.level().isClientSide && isModular(stack)) {
            InteractionHand effectiveHand = ReloadRuntimeCoordinator.handForStack(shooter, stack);
            rememberShot(shooter, effectiveHand == null ? InteractionHand.MAIN_HAND : effectiveHand, System.nanoTime());
        }
        if (isModular(stack)) {
            ModularGunItem gun = (ModularGunItem) stack.getItem();
            gun.reduceCurrentAmmoCount(stack, settledShots);
            syncNativeAmmo(gun, stack);
        }
        data.lastShootTimestamp = data.shootTimestamp;
        data.shootTimestamp = timestamp;
        data.shootCount = Math.max(0, data.shootCount) + 1;
        if (!shooter.level().isClientSide && stack.getItem() instanceof IModularItem modular) {
            modular.applyDamage(DurabilityCostPolicy.cost(settledShots, 1, 1), stack, shooter);
        }
        if (!shooter.level().isClientSide && stack.getItem() instanceof ModularGunItem gun) {
            gun.addShotHeat(stack, shooter.level().getGameTime(), settledShots);
        }
        return true;
    }

    private static synchronized long lastShot(LivingEntity entity, InteractionHand hand) {
        EnumMap<InteractionHand, Long> byHand = LAST_SHOTS.get(entity);
        return byHand == null ? -1L : byHand.getOrDefault(hand, -1L);
    }

    private static synchronized void rememberShot(LivingEntity entity, InteractionHand hand, long now) {
        LAST_SHOTS.computeIfAbsent(entity, ignored -> new EnumMap<>(InteractionHand.class)).put(hand, now);
    }

    public static boolean startBolt(ShooterDataHolder data, ItemStack stack, LivingEntity shooter) {
        ensureGunId(stack);
        return delegate().startBolt(data, stack, shooter);
    }

    public static boolean tickBolt(ShooterDataHolder data, ItemStack stack, LivingEntity shooter) {
        ensureGunId(stack);
        return delegate().tickBolt(data, stack, shooter);
    }

    public static boolean startReload(ShooterDataHolder data, ItemStack stack, LivingEntity shooter) {
        ensureGunId(stack);
        boolean eventCancelled = isModular(stack) && MinecraftForge.EVENT_BUS.post(
                new GunRuntimeEvent(GunRuntimeEvent.Type.RELOAD, shooter, stack));
        if (eventCancelled) return false;
        InteractionHand hand = ReloadRuntimeCoordinator.handForStack(shooter, stack);
        if (isModular(stack)) {
            ModularGunItem gun = (ModularGunItem) stack.getItem();
            boolean handReloading = hand != null && ReloadRuntimeCoordinator.handReloading(shooter, hand);
            boolean otherHandReloading = hand != null && ReloadRuntimeCoordinator.otherHandReloading(shooter, hand);
            boolean full = gun.getCurrentAmmoCount(stack) >= gun.getMaxDummyAmmoAmount(stack);
            if (!ReloadStartPolicy.allowed(true, gun.isBroken(stack), gun.isOverheatLocked(stack),
                    handReloading, otherHandReloading, full, gun.hasBulletInBarrel(stack))) {
                return false;
            }
        }
        boolean ownershipStarted = !isModular(stack) || hand == null || ReloadRuntimeCoordinator.begin(shooter, hand, stack);
        if (!ownershipStarted) {
            if (Boolean.getBoolean("taczintetra.dev_automation"))
                LOGGER.info("TaCZinTetra dev reload start rejected: eventCancelled={}, hand={}, ownershipStarted={}",
                        eventCancelled, hand, ownershipStarted);
            return false;
        }
        if (isModular(stack)) {
            ModularGunItem gun = (ModularGunItem) stack.getItem();
            boolean tactical = gun.getCurrentAmmoCount(stack) > 0 || gun.hasBulletInBarrel(stack);
            // LivingEntityReload 通常会在调用
            // AbstractGunItem.startReload 前初始化这些字段。Mixin 会拦截该方法
            // 以处理 Tetra 物品栈，因此适配器必须保留相同的原生
            // 计时状态，避免在下一个 tick 过早结算。
            data.reloadStateType = tactical
                    ? ReloadState.StateType.TACTICAL_RELOAD_FEEDING
                    : ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            data.reloadTimestamp = System.currentTimeMillis();
            // 模组弹匣由 TiT 管理。在 TaCZ 状态机读取物品栈之前，
            // 先同步 TaCZ 的旧版影子字段。
            syncNativeAmmo(gun, stack);
        }
        boolean accepted = delegate().startReload(data, stack, shooter);
        if (!accepted && isModular(stack)) {
            // 生成的枪械包刻意不包含 Lua 换弹钩子。某些 TaCZ 构建仍会在
            // 进入原生状态机之前返回 false；此处应初始化
            // 原生状态，但不要结算弹药。
            ModularGunItem gun = (ModularGunItem) stack.getItem();
            boolean tactical = gun.getCurrentAmmoCount(stack) > 0 || gun.hasBulletInBarrel(stack);
            data.reloadStateType = tactical
                    ? ReloadState.StateType.TACTICAL_RELOAD_FEEDING
                    : ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            data.reloadTimestamp = System.currentTimeMillis();
            accepted = true;
            if (Boolean.getBoolean("taczintetra.dev_automation"))
                LOGGER.info("TaCZinTetra dev reload delegate fallback: native tactical state seeded");
        }
        if (Boolean.getBoolean("taczintetra.dev_automation") && !accepted)
            LOGGER.info("TaCZinTetra dev reload delegate rejected: eventCancelled={}, hand={}, ownershipStarted={}, gunId={}, currentItem={}",
                    eventCancelled, hand, ownershipStarted, stack.getOrCreateTag().getString("GunId"),
                    data.currentGunItem == null ? "null" : data.currentGunItem.get().getItem());
        if (!accepted && isModular(stack)) {
            data.reloadStateType = ReloadState.StateType.NOT_RELOADING;
            data.reloadTimestamp = -1L;
            if (hand != null) ReloadRuntimeCoordinator.interrupt(shooter, hand);
        }
        if (accepted && isModular(stack)) {
            ReloadRuntimeSettlementService.markPending(stack,
                    stack.getOrCreateTag().getBoolean("taczintetra_reload_fill"));
            stack.getOrCreateTag().putLong(RELOAD_STARTED_AT, System.currentTimeMillis());
            stack.getOrCreateTag().putString(RELOAD_STATE, data.reloadStateType.name());
        }
        return accepted;
    }

    public static ReloadState tickReload(ShooterDataHolder data, ItemStack stack, LivingEntity shooter) {
        ensureGunId(stack);
        boolean pending = isModular(stack) && ReloadRuntimeSettlementService.isPending(stack);
        ModularGunItem gun = pending ? (ModularGunItem) stack.getItem() : null;
        int ammoBeforeNative = pending ? gun.getCurrentAmmoCount(stack) : -1;
        if (pending) syncNativeAmmo(gun, stack);
        ReloadState.StateType activeReloadType = pending
                ? persistedReloadState(stack, data.reloadStateType) : data.reloadStateType;
        long reloadStartedAt = pending && stack.getOrCreateTag().contains(RELOAD_STARTED_AT)
                ? stack.getOrCreateTag().getLong(RELOAD_STARTED_AT) : data.reloadTimestamp;
        if (pending && Boolean.getBoolean("taczintetra.dev_automation"))
            LOGGER.info("TaCZinTetra dev reload tick before delegate: state={}, elapsed={}ms",
                    data.reloadStateType, data.reloadTimestamp < 0 ? -1L : System.currentTimeMillis() - data.reloadTimestamp);
        ReloadState state = delegate().tickReload(data, stack, shooter);
        if (pending) syncNativeAmmo(gun, stack);
        if (pending && Boolean.getBoolean("taczintetra.dev_automation"))
            LOGGER.info("TaCZinTetra dev reload tick after delegate: state={}, elapsed={}ms, result={}",
                    data.reloadStateType, data.reloadTimestamp < 0 ? -1L : System.currentTimeMillis() - data.reloadTimestamp,
                    state == null ? "null" : state.getStateType());
        if (pending && state != null && state.getStateType() == ReloadState.StateType.NOT_RELOADING) {
            long elapsed = reloadStartedAt < 0 ? Long.MAX_VALUE
                    : Math.max(0L, System.currentTimeMillis() - reloadStartedAt);
            long duration = nativeReloadDurationMillis(stack, activeReloadType);
            if (elapsed < duration) {
                ReloadState guarded = new ReloadState();
                // TaCZ 委托逻辑可能会暂时将 holder 重置为
                // NOT_RELOADING，早于无 Lua 模组换弹时长结束的时间。
                // 必须恢复状态和时间戳，否则下一个 tick
                // 会看到 elapsed=-1 并立即结算换弹。
                data.reloadStateType = activeReloadType;
                data.reloadTimestamp = reloadStartedAt;
                guarded.setStateType(activeReloadType);
                guarded.setCountDown(duration - elapsed);
                if (Boolean.getBoolean("taczintetra.dev_automation"))
                    LOGGER.info("TaCZinTetra dev reload tick guard: native result was NOT_RELOADING before {}ms", duration);
                return guarded;
            }
        }
        if (pending && state != null && state.getStateType().name().equals("NOT_RELOADING")) {
            // TaCZ 默认结束器会在控制权交给本适配器之前写入原生弹匣。
            // 恢复 TiT 管理的值，确保只有一个权威来源
            // 计算弹药批次并且只扣除一次资源。
            gun.setCurrentAmmoCount(stack, ammoBeforeNative);
            boolean settled = ReloadRuntimeSettlementService.settleIfComplete(gun, stack, shooter);
            syncNativeAmmo(gun, stack);
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev reload completed: settled={}, ammo={}, resource9mm={}", settled,
                        gun.getCurrentAmmoCount(stack),
                        com.pycoder.taczintetra.logic.ResourceNbtAdapter.read(stack.getOrCreateTag(),
                                com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm"));
            }
            InteractionHand hand = ReloadRuntimeCoordinator.handForStack(shooter, stack);
            if (hand != null) ReloadRuntimeCoordinator.complete(shooter, hand);
            ReloadRuntimeSettlementService.clearRuntimeState(stack);
            restoreDefaultGunSupplier(data, shooter);
        }
        return state;
    }

    private static long nativeReloadDurationMillis(ItemStack stack, ReloadState.StateType stateType) {
        long fallback = stateType != ReloadState.StateType.EMPTY_RELOAD_FEEDING ? 1500L : 1880L;
        if (!(stack.getItem() instanceof ModularGunItem gun)) return fallback;
        long nativeDuration = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).map(index -> {
            GunReloadData reload = index.getGunData().getReloadData();
            boolean empty = stateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            float feed = empty ? reload.getFeed().getEmptyTime() : reload.getFeed().getTacticalTime();
            float cooldown = empty ? reload.getCooldown().getEmptyTime() : reload.getCooldown().getTacticalTime();
            return Math.max(1L, (long) ((feed + cooldown) * 1000.0F));
        }).orElse(fallback);
        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                com.pycoder.taczintetra.item.GunModuleSlots.BODY,
                com.pycoder.taczintetra.item.GunModuleSlots.BARREL,
                com.pycoder.taczintetra.item.GunModuleSlots.MAGAZINE);
        double multiplier = profile == null ? 1.0 : profile.handling().reloadTimeMultiplier();
        return ReloadTimingPolicy.durationMillis(nativeDuration, multiplier);
    }

    public static void interruptReload(ShooterDataHolder data, ItemStack stack, LivingEntity shooter) {
        ensureGunId(stack);
        ReloadRuntimeSettlementService.clearRuntimeState(stack);
        InteractionHand hand = ReloadRuntimeCoordinator.handForStack(shooter, stack);
        if (hand != null) ReloadRuntimeCoordinator.interrupt(shooter, hand);
        delegate().interruptReload(data, stack, shooter);
        data.reloadStateType = ReloadState.StateType.NOT_RELOADING;
        data.reloadTimestamp = -1L;
        restoreDefaultGunSupplier(data, shooter);
    }

    /** 手部专属换弹流程结束后，恢复共享的 TaCZ holder。 */
    private static void restoreDefaultGunSupplier(ShooterDataHolder data, LivingEntity shooter) {
        data.currentGunItem = shooter::getMainHandItem;
    }

    private static ReloadState.StateType persistedReloadState(ItemStack stack, ReloadState.StateType fallback) {
        String value = stack.getOrCreateTag().getString(RELOAD_STATE);
        if (!value.isBlank()) {
            try {
                return ReloadState.StateType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                // 旧物品栈格式异常时，回退使用 TaCZ holder 状态。
            }
        }
        return fallback;
    }

    public static void fireSelect(ShooterDataHolder data, ItemStack stack) {
        ensureGunId(stack);
        delegate().fireSelect(data, stack);
    }

    public static void melee(ShooterDataHolder data, LivingEntity shooter, ItemStack stack) {
        ensureGunId(stack);
        delegate().melee(data, shooter, stack);
    }

    private static void ensureGunId(ItemStack stack) {
        if (isModular(stack)) {
            stack.getOrCreateTag().putString("GunId", "taczintetra:modular_gun");
        }
    }

    /** TaCZ 已注册委托读取此原版 TaCZ 键，公开物品则使用自己的命名空间键。 */
    private static void syncNativeAmmo(ModularGunItem gun, ItemStack stack) {
        stack.getOrCreateTag().putInt("AmmoCount", Math.max(0, gun.getCurrentAmmoCount(stack)));
    }

    /** 直接调用 TaCZ 弹丸实现；物品包装器的可选脚本路径会跳过 Tetra 物品栈的 shootOnce。 */
    private static void shootThroughNativeScriptApi(ShooterDataHolder data, ItemStack stack,
                                                     Supplier<Float> pitch, Supplier<Float> yaw,
                                                     LivingEntity shooter, boolean consumeNativeAmmo) {
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(stack);
        api.setShooter(shooter);
        api.setDataHolder(data);
        api.setPitchSupplier(pitch);
        api.setYawSupplier(yaw);
        api.shootOnce(consumeNativeAmmo);
    }

    /** 使用 TaCZ 的公开弹丸实现，同时以模块弹药数据为准。 */
    private static int spawnNativeProjectile(ShooterDataHolder data, ModularGunItem gun,
                                               ItemStack stack, Supplier<Float> pitch,
                                               Supplier<Float> yaw, LivingEntity shooter,
                                               int independentShots) {
        var gunIndex = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        if (gunIndex == null || gunIndex.getGunData() == null || gunIndex.getBulletData() == null) return 0;
        var gunData = gunIndex.getGunData();
        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                com.pycoder.taczintetra.item.GunModuleSlots.BODY,
                com.pycoder.taczintetra.item.GunModuleSlots.BARREL,
                com.pycoder.taczintetra.item.GunModuleSlots.MAGAZINE);
        float velocity = profile == null || !Float.isFinite((float) profile.projectile().velocity())
                || profile.projectile().velocity() <= 0 ? 1.0f
                : (float) profile.projectile().velocity();
        float nativeSpread = gunData.getInaccuracy(InaccuracyType.getInaccuracyType(shooter), 0.0f);
        float spread = SpreadPolicy.total(nativeSpread, gun.lerpInaccuracy(stack));
        int projectileCount = gun.projectileCount(stack, independentShots);
        if (projectileCount <= 0) return 0;
        var modules = TetraItemStackProfileResolver.selectedModules(gun, stack,
                com.pycoder.taczintetra.item.GunModuleSlots.BODY,
                com.pycoder.taczintetra.item.GunModuleSlots.BARREL,
                com.pycoder.taczintetra.item.GunModuleSlots.MAGAZINE);
        String barrelId = modules == null ? null : modules.barrel().variantId();
        var bulletData = ConfiguredProjectilePolicy.nativeBulletData(
                TaCZinTetra.MODULE_CONFIG, barrelId, gunIndex.getBulletData());
        String configuredAmmoText = ConfiguredAmmoRecipeResolver.projectileId(
                TaCZinTetra.MODULE_CONFIG, modules);
        ResourceLocation ammoId = ConfiguredAmmoPolicy.resolve(configuredAmmoText, gunData.getAmmoId());
        if (configuredAmmoText != null && !configuredAmmoText.isBlank() && ammoId == null) return 0;
        if (ammoId == null) return 0;
        int createdProjectiles = 0;
        for (int index = 0; index < projectileCount; index++) {
            var bullet = new EntityKineticBullet(shooter.level(), shooter, stack,
                    ammoId, gun.getGunId(stack), gun.getGunDisplayId(stack),
                    false, gunData, bulletData);
            // TaCZ 1.1.8-hotfix 仅在 Lua 散布回调中设置轨迹。
            // 模组枪械刻意不提供脚本，因此该回调不执行操作，
            // 导致弹丸方向变化量为零。
            // 对无脚本的回退路径使用 TaCZ 公共旋转 API；
            // 原生脚本枪械仍保留原有的散布流程。
            if (!hasScript(gunIndex)) {
                bullet.shootFromRotation(shooter, pitch.get(), yaw.get(), 0.0f,
                        velocity, spread);
            } else {
                delegate().doBulletSpread(data, stack, shooter, bullet, index,
                        velocity, spread, pitch.get(), yaw.get());
            }
            if (shooter.level().addFreshEntity(bullet)) createdProjectiles++;
        }
        return createdProjectiles;
    }

    private static boolean hasScript(Object index) {
        try {
            return index.getClass().getMethod("getScript").invoke(index) != null;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
