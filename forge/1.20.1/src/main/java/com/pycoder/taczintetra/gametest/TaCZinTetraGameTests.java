package com.pycoder.taczintetra.gametest;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.api.GunSourceAccessor;
import com.pycoder.taczintetra.registry.ModItems;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.config.ConfiguredPartIds;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.pycoder.taczintetra.compat.ModularGunLifecycleAdapter;
import com.pycoder.taczintetra.logic.NetworkTimestampPolicy;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.network.OffhandShootIntentMessage;
import com.pycoder.taczintetra.recipe.AmmoResourceRecipe;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import se.mickelus.tetra.items.modular.IModularItem;

/** 需要真实注册表与 TaCZ 运行时环境的 Forge 端检查。 */
@GameTestHolder(TaCZinTetra.MOD_ID)
public final class TaCZinTetraGameTests {
    private TaCZinTetraGameTests() { }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void workbenchMaterialGameTestHolderIsScannedAndRegistered(GameTestHelper helper) {
        String expectedClass = "com.pycoder.taczintetra.gametest.TetraWorkbenchMaterialGameTests";
        var scannedHolders = ModList.get().getAllScanData().stream()
                .flatMap(scanData -> scanData.getAnnotations().stream())
                .filter(annotation -> "net.minecraftforge.gametest.GameTestHolder"
                        .equals(annotation.annotationType().getClassName()))
                .map(annotation -> annotation.clazz().getClassName())
                .toList();
        boolean holderWasScanned = scannedHolders.contains(expectedClass);
        boolean holderWasRegistered = net.minecraft.gametest.framework.GameTestRegistry
                .getAllTestClassNames().contains("TetraWorkbenchMaterialGameTests");

        helper.assertTrue(holderWasScanned && holderWasRegistered,
                "workbench holder scan=" + holderWasScanned + ", registered=" + holderWasRegistered
                        + ", scannedHolders=" + scannedHolders + ", registeredClasses="
                        + net.minecraft.gametest.framework.GameTestRegistry.getAllTestClassNames());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void starterGunIsRecognizedByTacz(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        helper.assertTrue(IGun.getIGunOrNull(stack) == ModItems.STARTER_PISTOL.get(),
                "starter_pistol must be visible through TaCZ IGun.getIGunOrNull");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void modularProjectileCapturesTetraGunForNativeTaczProperties(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        var shooter = helper.spawn(EntityType.PIG, BlockPos.ZERO);
        var index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        helper.assertTrue(index != null && index.getGunData() != null && index.getBulletData() != null,
                "the registered modular gun must resolve its native TaCZ projectile data");
        if (index == null || index.getGunData() == null || index.getBulletData() == null) return;
        AttachmentCacheProperty attachmentCache = new AttachmentCacheProperty();
        attachmentCache.eval(stack, index.getGunData());
        IGunOperator.fromLivingEntity(shooter).updateCacheProperty(attachmentCache);

        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(profile != null, "the real Tetra modules must resolve before bullet construction");
        if (profile == null) return;

        EntityKineticBullet bullet = new EntityKineticBullet(helper.getLevel(), shooter, stack,
                gun.getGunId(stack), gun.getGunDisplayId(stack), index.getGunData().getAmmoId(),
                false, index.getGunData(), index.getBulletData());
        helper.assertTrue(bullet instanceof GunSourceAccessor,
                "the TaCZ projectile mixin must expose the captured source gun stack; class="
                        + bullet.getClass().getName() + ", interfaces="
                        + java.util.Arrays.toString(bullet.getClass().getInterfaces()) + ", accessorLoader="
                        + GunSourceAccessor.class.getClassLoader());
        ItemStack captured = ((GunSourceAccessor) (Object) bullet).sourceGun();
        helper.assertTrue(captured.getItem() == gun,
                "the constructor's actual Tetra gun stack must be captured before native property calls");

        try {
            var propertyMethod = EntityKineticBullet.class.getDeclaredMethod(
                    "modifyProperty", String.class, Class.class, Object.class);
            propertyMethod.setAccessible(true);
            float baseSpeed = index.getBulletData().getSpeed();
            float actualSpeed = (float) propertyMethod.invoke(
                    bullet, GunProperties.AMMO_SPEED.name(), Float.class, baseSpeed);
            float expectedSpeed = baseSpeed * (float) profile.projectile().velocity();
            helper.assertTrue(Math.abs(actualSpeed - expectedSpeed) < 0.001f,
                    "TaCZ native ammo_speed callback must include the resolved Tetra barrel multiplier; expected "
                            + expectedSpeed + " but got " + actualSpeed);
            float baseDamage = index.getBulletData().getDamageAmount();
            float actualDamage = (float) propertyMethod.invoke(
                    bullet, GunProperties.DAMAGE.name(), Float.class, baseDamage);
            helper.assertTrue(Math.abs(actualDamage - baseDamage) < 0.001f,
                    "TaCZ native DAMAGE callback must preserve its distance-tier input; expected "
                            + baseDamage + " but got " + actualDamage);
        } catch (ReflectiveOperationException e) {
            helper.fail("the locked TaCZ projectile speed field could not be inspected: " + e.getMessage());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void tetraDamageMultiplierIsNotAppliedTwiceToNativeProjectile(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/rifle", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        var shooter = helper.spawn(EntityType.PIG, BlockPos.ZERO);
        var index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        helper.assertTrue(index != null && index.getGunData() != null && index.getBulletData() != null,
                "the configured modular rifle must resolve native TaCZ projectile data");
        if (index == null || index.getGunData() == null || index.getBulletData() == null) return;

        AttachmentCacheProperty attachmentCache = new AttachmentCacheProperty();
        attachmentCache.eval(stack, index.getGunData());
        IGunOperator.fromLivingEntity(shooter).updateCacheProperty(attachmentCache);

        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(profile != null, "the rifle Tetra profile must resolve");
        if (profile == null) return;

        EntityKineticBullet bullet = new EntityKineticBullet(helper.getLevel(), shooter, stack,
                gun.getGunId(stack), gun.getGunDisplayId(stack), index.getGunData().getAmmoId(),
                false, index.getGunData(), index.getBulletData());
        var damageAdjust = index.getBulletData().getExtraDamage().getDamageAdjust();
        helper.assertTrue(damageAdjust != null && !damageAdjust.isEmpty(),
                "TaCZ must provide its native distance damage tiers");
        if (damageAdjust == null || damageAdjust.isEmpty()) return;
        float nativeNearDamage = damageAdjust.getFirst().getDamage();
        float damageScale = (float) profile.projectile().damage() / index.getBulletData().getDamageAmount();
        float expectedDamage = nativeNearDamage * damageScale;
        float actualDamage = bullet.getDamage(bullet.position());
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 0.001f,
                "TaCZ distance damage and the resolved Tetra multiplier must apply once; expected "
                        + expectedDamage + " but got " + actualDamage);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nativeTaczProjectileActuallyDamagesLivingTarget(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/rifle", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        var shooter = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        var target = helper.spawn(EntityType.PIG, new BlockPos(6, 1, 2));
        var index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        helper.assertTrue(index != null && index.getGunData() != null && index.getBulletData() != null,
                "the configured rifle must resolve its native TaCZ projectile data");
        if (index == null || index.getGunData() == null || index.getBulletData() == null) return;

        AttachmentCacheProperty attachmentCache = new AttachmentCacheProperty();
        attachmentCache.eval(stack, index.getGunData());
        IGunOperator.fromLivingEntity(shooter).updateCacheProperty(attachmentCache);
        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(profile != null, "rifle Tetra profile must resolve before firing");
        if (profile == null) return;

        EntityKineticBullet bullet = new EntityKineticBullet(helper.getLevel(), shooter, stack,
                gun.getGunId(stack), gun.getGunDisplayId(stack), index.getGunData().getAmmoId(),
                false, index.getGunData(), index.getBulletData());
        bullet.setPos(target.getX() - 2.5, target.getY() + target.getBbHeight() * 0.5, target.getZ());
        bullet.setNoGravity(true);
        bullet.shoot(0, -90, 1.5f, new org.joml.Vector2d(0, 0));
        float healthBefore = target.getHealth();
        helper.assertTrue(helper.getLevel().addFreshEntity(bullet), "native TaCZ bullet must enter the test world");

        helper.runAtTickTime(5, () -> {
            float healthAfter = target.getHealth();
            helper.assertTrue(healthAfter < healthBefore,
                    "real TaCZ projectile tick must damage the target; before=" + healthBefore
                            + ", after=" + healthAfter + ", bulletPos=" + bullet.position()
                            + ", bulletMotion=" + bullet.getDeltaMovement() + ", targetBounds=" + target.getBoundingBox()
                            + ", profileDamage=" + profile.projectile().damage());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void zeroAmmoShotIsRejectedWithoutProjectileOrNegativeAmmo(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/rifle", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        gun.setCurrentAmmoCount(stack, 0);
        var shooter = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var shooterData = IGunOperator.fromLivingEntity(shooter).getDataHolder();
        int bulletsBefore = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32)).size();
        int durabilityBefore = stack.getDamageValue();

        long clientTimestamp = NetworkTimestampPolicy.clientTimestamp(
                System.currentTimeMillis(), shooterData.baseTimestamp);
        boolean accepted = ModularGunLifecycleAdapter.shoot(shooterData, stack,
                () -> 0.0f, () -> 0.0f, shooter, clientTimestamp);

        int bulletsAfter = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32)).size();
        helper.assertTrue(!accepted, "the lifecycle adapter must reject a modular gun shot with zero ammo");
        helper.assertTrue(bulletsAfter == bulletsBefore,
                "a rejected zero-ammo shot must not create a TaCZ projectile");
        helper.assertTrue(gun.getCurrentAmmoCount(stack) == 0,
                "a rejected zero-ammo shot must keep ammo at zero, never below zero");
        helper.assertTrue(stack.getDamageValue() == durabilityBefore,
                "a rejected zero-ammo shot must not consume Tetra durability");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void productionShootAdapterConsumesAmmoAndAddsHeatForMainHandShot(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        gun.setCurrentAmmoCount(stack, 1);
        StackIdentity.ensureUniqueId(stack);

        var shooter = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var holder = IGunOperator.fromLivingEntity(shooter).getDataHolder();
        long timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(), holder.baseTimestamp);
        var bulletsOwnedBefore = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32), bullet -> bullet.getOwner() == shooter);
        java.util.Set<java.util.UUID> bulletIdsBefore = bulletsOwnedBefore.stream()
                .map(EntityKineticBullet::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        float heatBefore = gun.getHeatAmount(stack);
        int durabilityBefore = stack.getDamageValue();

        boolean accepted = ModularGunLifecycleAdapter.shoot(holder, stack,
                () -> 0.0f, () -> 0.0f, shooter, timestamp);

        var bulletsOwnedAfter = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32), bullet -> bullet.getOwner() == shooter);
        var bulletsCreatedByThisShot = bulletsOwnedAfter.stream()
                .filter(bullet -> !bulletIdsBefore.contains(bullet.getUUID()))
                .toList();
        EntityKineticBullet bullet = bulletsCreatedByThisShot.size() == 1
                ? bulletsCreatedByThisShot.get(0) : null;
        ItemStack capturedSource = bullet instanceof GunSourceAccessor
                ? ((GunSourceAccessor) (Object) bullet).sourceGun() : ItemStack.EMPTY;
        helper.assertTrue(accepted, "the production lifecycle adapter must accept a ready main-hand modular shot");
        helper.assertTrue(bulletsCreatedByThisShot.size() == 1,
                "one accepted main-hand round must create exactly one projectile owned by this shooter; before="
                        + bulletsOwnedBefore.size() + ", after=" + bulletsOwnedAfter.size()
                        + ", newProjectileCount=" + bulletsCreatedByThisShot.size());
        helper.assertTrue(gun.getCurrentAmmoCount(stack) == 0,
                "the production adapter must consume exactly the loaded round");
        helper.assertTrue(gun.getHeatAmount(stack) > heatBefore,
                "a successful production shot must add configured heat; before=" + heatBefore
                        + ", after=" + gun.getHeatAmount(stack));
        helper.assertTrue(stack.getDamageValue() == durabilityBefore + 1,
                "one successful independent shot must consume exactly one Tetra durability point; before="
                        + durabilityBefore + ", after=" + stack.getDamageValue());
        helper.assertTrue(StackIdentity.hasUniqueId(capturedSource)
                        && StackIdentity.of(capturedSource) == StackIdentity.of(stack),
                "the native TaCZ projectile must retain the firing stack identity");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void shotgunPelletsConsumeOneAmmoAndOneTetraDurabilityPoint(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/shotgun", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/12g", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        gun.setCurrentAmmoCount(stack, 1);
        StackIdentity.ensureUniqueId(stack);

        var shooter = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var holder = IGunOperator.fromLivingEntity(shooter).getDataHolder();
        long timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(), holder.baseTimestamp);
        var bulletsBefore = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32), bullet -> bullet.getOwner() == shooter);
        java.util.Set<java.util.UUID> bulletIdsBefore = bulletsBefore.stream()
                .map(EntityKineticBullet::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        int durabilityBefore = stack.getDamageValue();

        boolean accepted = ModularGunLifecycleAdapter.shoot(holder, stack,
                () -> 0.0f, () -> 0.0f, shooter, timestamp);

        var bulletsAfter = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32), bullet -> bullet.getOwner() == shooter);
        long newProjectileCount = bulletsAfter.stream()
                .filter(bullet -> !bulletIdsBefore.contains(bullet.getUUID()))
                .count();
        helper.assertTrue(accepted, "the configured 12g shotgun shot must be accepted");
        helper.assertTrue(newProjectileCount == 8,
                "one configured 12g round must create eight pellets; actual=" + newProjectileCount);
        helper.assertTrue(gun.getCurrentAmmoCount(stack) == 0,
                "eight pellets from one shot must consume one ammunition round");
        helper.assertTrue(stack.getDamageValue() == durabilityBefore + 1,
                "eight pellets from one independent shot must consume one Tetra durability point");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void serverPlayerTickCoolsOverheatedModularGunAndUnlocksIt(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "HeatTickGameTest"));
        player.setItemInHand(InteractionHand.OFF_HAND, stack);

        gun.setHeatAmount(stack, 98.0f);
        long shotTime = helper.getLevel().getGameTime();
        gun.addShotHeat(stack, shotTime, 1);
        helper.assertTrue(gun.isOverheatLocked(stack),
                "the configured 9mm barrel must lock after its shot heat reaches the threshold");

        helper.runAfterDelay(120, () -> {
            MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(gun.getHeatAmount(stack) < 1.0f && gun.getHeatAmount(stack) >= 0.0f,
                    "the real server player-tick subscriber must cool heat into the valid non-negative range; heat="
                            + gun.getHeatAmount(stack));
            helper.assertTrue(!gun.isOverheatLocked(stack),
                    "the server tick cooling path must release the overheat lock near ambient");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nativeTaczOperatorShootsModularGunFromOffhandStack(GameTestHelper helper) {
        ItemStack mainHand = new ItemStack(ModItems.STARTER_PISTOL.get());
        ItemStack offHand = new ItemStack(ModItems.STARTER_PISTOL.get());
        for (ItemStack stack : new ItemStack[] { mainHand, offHand }) {
            IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
            IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
            IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        }
        ModularGunItem gun = (ModularGunItem) offHand.getItem();
        gun.setCurrentAmmoCount(mainHand, 0);
        gun.setCurrentAmmoCount(offHand, 1);
        StackIdentity.ensureUniqueId(offHand);
        var shooter = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        shooter.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        shooter.setItemInHand(InteractionHand.OFF_HAND, offHand);

        IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
        var data = operator.getDataHolder();
        var previousGunSupplier = data.currentGunItem;
        int bulletsBefore = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32)).size();
        long timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(), data.baseTimestamp);
        data.currentGunItem = () -> shooter.getItemInHand(InteractionHand.OFF_HAND);
        com.tacz.guns.api.entity.ShootResult result;
        try {
            result = operator.shoot(() -> 0.0f, () -> 0.0f, timestamp);
        } finally {
            data.currentGunItem = previousGunSupplier;
        }

        int bulletsAfter = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32)).size();
        helper.assertTrue(result == com.tacz.guns.api.entity.ShootResult.SUCCESS,
                "TaCZ native operator must accept the selected offhand modular gun; result=" + result);
        helper.assertTrue(gun.getCurrentAmmoCount(offHand) == 0 && gun.getCurrentAmmoCount(mainHand) == 0,
                "the one offhand round must be consumed while the empty main-hand stack stays unchanged; main="
                        + gun.getCurrentAmmoCount(mainHand) + ", off=" + gun.getCurrentAmmoCount(offHand));
        helper.assertTrue(bulletsAfter == bulletsBefore + 1,
                "one successful native offhand shot must create exactly one TaCZ projectile; before="
                        + bulletsBefore + ", after=" + bulletsAfter);
        EntityKineticBullet bullet = helper.getLevel().getEntitiesOfClass(EntityKineticBullet.class,
                shooter.getBoundingBox().inflate(32)).stream().reduce((first, second) -> second).orElse(null);
        ItemStack capturedSource = bullet instanceof GunSourceAccessor
                ? ((GunSourceAccessor) (Object) bullet).sourceGun() : ItemStack.EMPTY;
        helper.assertTrue(!capturedSource.isEmpty() && StackIdentity.hasUniqueId(capturedSource)
                        && StackIdentity.of(capturedSource) == StackIdentity.of(offHand),
                "the TaCZ projectile must capture a copy with the offhand stack identity; captured="
                        + capturedSource + ", offhand=" + offHand);
        helper.assertTrue(data.currentGunItem == previousGunSupplier,
                "the TaCZ current-gun supplier must be restored after the native shot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void offhandShootIntentValidatesIdentityAndTwoHandedMainBeforeShooting(GameTestHelper helper) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "OffhandShootGameTest"));
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        player.teleportTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
        player.setXRot(0.0F);
        player.setYRot(0.0F);

        ItemStack mainHand = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(mainHand);
        ItemStack offHand = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(offHand);
        ModularGunItem gun = (ModularGunItem) offHand.getItem();
        gun.setCurrentAmmoCount(offHand, 1);
        StackIdentity.ensureUniqueId(offHand);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);

        var level = helper.getLevel();
        int bulletsBefore = level.getEntitiesOfClass(EntityKineticBullet.class,
                player.getBoundingBox().inflate(64), bullet -> bullet.getOwner() == player).size();
        int stackIdentity = StackIdentity.of(offHand);
        long timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(),
                IGunOperator.fromLivingEntity(player).getDataHolder().baseTimestamp);

        invokeOffhandShootIntent(player,
                new OffhandShootIntentMessage(stackIdentity ^ 1, 1, timestamp));
        helper.assertTrue(gun.getCurrentAmmoCount(offHand) == 1,
                "an offhand shoot intent with a mismatched stack identity must not consume ammo");
        helper.assertTrue(level.getEntitiesOfClass(EntityKineticBullet.class,
                        player.getBoundingBox().inflate(64), bullet -> bullet.getOwner() == player).size()
                        == bulletsBefore,
                "a mismatched offhand identity must not create a TaCZ projectile");

        IModularItem.putModuleInSlot(mainHand, GunModuleSlots.BODY,
                "taczintetra/body/rifle", "taczintetra/iron/");
        helper.assertTrue(TetraItemStackProfileResolver.isTwoHanded(
                        (ModularGunItem) mainHand.getItem(), mainHand),
                "the main-hand fixture must resolve to a two-handed Tetra rifle");
        invokeOffhandShootIntent(player, new OffhandShootIntentMessage(stackIdentity, 1, timestamp));
        helper.assertTrue(gun.getCurrentAmmoCount(offHand) == 1,
                "a two-handed main-hand rifle must block the offhand shoot intent");
        helper.assertTrue(level.getEntitiesOfClass(EntityKineticBullet.class,
                        player.getBoundingBox().inflate(64), bullet -> bullet.getOwner() == player).size()
                        == bulletsBefore,
                "a two-handed main-hand rifle must not emit an offhand projectile");

        IModularItem.putModuleInSlot(mainHand, GunModuleSlots.BODY,
                "taczintetra/body/pistol", "taczintetra/iron/");
        timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(),
                IGunOperator.fromLivingEntity(player).getDataHolder().baseTimestamp);
        invokeOffhandShootIntent(player, new OffhandShootIntentMessage(stackIdentity, 1, timestamp));
        int bulletsAfter = level.getEntitiesOfClass(EntityKineticBullet.class,
                player.getBoundingBox().inflate(64), bullet -> bullet.getOwner() == player).size();
        helper.assertTrue(gun.getCurrentAmmoCount(offHand) == 0 && bulletsAfter == bulletsBefore + 1,
                "the same sequence must remain available after rejected intents and fire exactly once; ammo="
                        + gun.getCurrentAmmoCount(offHand) + ", projectiles=" + bulletsAfter);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void reloadIntentRejectsMismatchedIdentityAndStartsAcceptedReload(GameTestHelper helper) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ReloadIntentGameTest"));
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        player.teleportTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);

        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(stack);
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        gun.setCurrentAmmoCount(stack, 0);
        ResourceNbtAdapter.write(stack.getOrCreateTag(),
                com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm", 8);
        StackIdentity.ensureUniqueId(stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        int identity = StackIdentity.of(stack);

        invokeReloadIntent(player, new com.pycoder.taczintetra.network.ReloadIntentMessage(
                false, true, identity ^ 1, 1));
        helper.assertTrue(!com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(stack),
                "a reload intent for another stack identity must not start or reserve a reload");

        invokeReloadIntent(player, new com.pycoder.taczintetra.network.ReloadIntentMessage(
                false, true, identity, 1));
        helper.assertTrue(com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(stack),
                "the matching held stack intent must start TaCZ's reload lifecycle and mark settlement pending");
        helper.assertTrue(com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.handReloading(
                        player, InteractionHand.MAIN_HAND),
                "the accepted reload must be owned by the player's main-hand context");
        com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.reconcile(player);
        helper.assertTrue(com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.handReloading(
                        player, InteractionHand.MAIN_HAND)
                        && com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(stack),
                "TaCZ reload metadata must not make server reconciliation interrupt the accepted reload");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void otherHandReloadIntentIsRejectedWhileMainHandReloadOwnsPlayer(GameTestHelper helper) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ReloadArbitrationGameTest"));
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        player.teleportTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);

        ItemStack mainHand = new ItemStack(ModItems.STARTER_PISTOL.get());
        ItemStack offHand = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(mainHand);
        installMinimumMajorModules(offHand);
        ModularGunItem gun = (ModularGunItem) mainHand.getItem();
        gun.setCurrentAmmoCount(mainHand, 0);
        gun.setCurrentAmmoCount(offHand, 0);
        ResourceNbtAdapter.write(mainHand.getOrCreateTag(),
                com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm", 8);
        ResourceNbtAdapter.write(offHand.getOrCreateTag(),
                com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm", 8);
        StackIdentity.ensureUniqueId(mainHand);
        StackIdentity.ensureUniqueId(offHand);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        int mainIdentity = StackIdentity.of(mainHand);
        int offIdentity = StackIdentity.of(offHand);

        invokeReloadIntent(player, new com.pycoder.taczintetra.network.ReloadIntentMessage(
                false, true, mainIdentity, 1));
        helper.assertTrue(com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(mainHand)
                        && com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.handReloading(
                        player, InteractionHand.MAIN_HAND),
                "the valid main-hand reload must acquire the player's reload ownership");
        if (!com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(mainHand)) return;

        invokeReloadIntent(player, new com.pycoder.taczintetra.network.ReloadIntentMessage(
                true, true, offIdentity, 2));

        helper.assertTrue(!com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(offHand)
                        && !com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.handReloading(
                        player, InteractionHand.OFF_HAND),
                "an offhand reload must be rejected while the main hand owns the single native reload slot");
        helper.assertTrue(com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(mainHand)
                        && com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.handReloading(
                        player, InteractionHand.MAIN_HAND),
                "rejecting the competing intent must preserve the accepted main-hand transaction");
        helper.assertTrue(gun.getCurrentAmmoCount(mainHand) == 0
                        && gun.getCurrentAmmoCount(offHand) == 0
                        && ResourceNbtAdapter.read(mainHand.getOrCreateTag(),
                        com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm") == 8
                        && ResourceNbtAdapter.read(offHand.getOrCreateTag(),
                        com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm") == 8,
                "neither reload may settle ammunition or resources before the native reload completes");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void acceptedReloadCompletesThroughNativeTickAndSettlesOneConfiguredBatch(GameTestHelper helper) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ReloadSettlementGameTest"));
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        player.teleportTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);

        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(stack);
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        gun.setCurrentAmmoCount(stack, 0);
        ResourceNbtAdapter.write(stack.getOrCreateTag(),
                com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm", 8);
        StackIdentity.ensureUniqueId(stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        invokeReloadIntent(player, new com.pycoder.taczintetra.network.ReloadIntentMessage(
                false, true, StackIdentity.of(stack), 1));
        var holder = IGunOperator.fromLivingEntity(player).getDataHolder();
        helper.assertTrue(com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(stack),
                "the accepted TaCZ reload must remain pending until the native reload state completes");
        if (!com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(stack)) return;

        long expiredNativeStart = System.currentTimeMillis() - 60_000L;
        holder.reloadTimestamp = expiredNativeStart;
        stack.getOrCreateTag().putLong("taczintetra_reload_started_at", expiredNativeStart);
        var state = ModularGunLifecycleAdapter.tickReload(holder, stack, player);

        helper.assertTrue(state != null
                        && state.getStateType() == com.tacz.guns.api.entity.ReloadState.StateType.NOT_RELOADING,
                "the completed TaCZ native reload tick must report NOT_RELOADING; state="
                        + (state == null ? "null" : state.getStateType()));
        helper.assertTrue(gun.getCurrentAmmoCount(stack) == 1,
                "one configured reload batch must load exactly one virtual round; ammo="
                        + gun.getCurrentAmmoCount(stack));
        helper.assertTrue(ResourceNbtAdapter.read(stack.getOrCreateTag(),
                        com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm") == 7,
                "the completed single-batch reload must debit exactly one 9mm resource; remaining="
                        + ResourceNbtAdapter.read(stack.getOrCreateTag(),
                        com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm"));
        helper.assertTrue(!com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService.isPending(stack),
                "the completed reload must clear its pending transaction marker");
        helper.assertTrue(!com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator.handReloading(
                        player, InteractionHand.MAIN_HAND),
                "the completed reload must release main-hand reload ownership");
        helper.succeed();
    }

    private static void invokeReloadIntent(net.minecraft.server.level.ServerPlayer player,
                                            com.pycoder.taczintetra.network.ReloadIntentMessage message) {
        try {
            var handler = com.pycoder.taczintetra.network.ReloadIntentMessage.class
                    .getDeclaredMethod("validateCurrentHand",
                            net.minecraft.server.level.ServerPlayer.class,
                            com.pycoder.taczintetra.network.ReloadIntentMessage.class);
            handler.setAccessible(true);
            handler.invoke(null, player, message);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not execute the real reload server intent handler", exception);
        }
    }

    private static void invokeOffhandShootIntent(net.minecraft.server.level.ServerPlayer player,
                                                  OffhandShootIntentMessage message) {
        try {
            var shoot = OffhandShootIntentMessage.class.getDeclaredMethod("shoot",
                    net.minecraft.server.level.ServerPlayer.class, OffhandShootIntentMessage.class);
            shoot.setAccessible(true);
            shoot.invoke(null, player, message);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not execute the real offhand server intent handler", exception);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void modularGunScalesTaczNativeRecoilCacheFromTetraProfile(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        var shooter = helper.spawn(EntityType.PIG, BlockPos.ZERO);
        var index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        helper.assertTrue(index != null && index.getGunData() != null,
                "the modular gun must resolve TaCZ gun data for its native recoil curve");
        if (index == null || index.getGunData() == null) return;
        var baseCache = new AttachmentCacheProperty();
        baseCache.eval(stack, index.getGunData());
        var nativeBase = (com.tacz.guns.api.modifier.ParameterizedCachePair<Float, Float>)
                baseCache.getCache(GunProperties.RECOIL);
        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(nativeBase != null && profile != null,
                "TaCZ recoil data and the real Tetra handling profile must both resolve");
        if (nativeBase == null || profile == null) return;

        com.pycoder.taczintetra.runtime.GunCacheSynchronizer.ensure(shooter, stack);
        var actual = IGunOperator.fromLivingEntity(shooter).getCacheProperty()
                .<com.tacz.guns.api.modifier.ParameterizedCachePair<Float, Float>>getCache(GunProperties.RECOIL);
        float expectedPitch = nativeBase.left().getDefaultValue() * (float) profile.handling().recoilAdd();
        float expectedYaw = nativeBase.right().getDefaultValue() * (float) profile.handling().recoilAdd();
        helper.assertTrue(Math.abs(actual.left().getDefaultValue() - expectedPitch) < 0.0001f,
                "TaCZ pitch recoil cache must be scaled by Tetra profile; expected " + expectedPitch
                        + " but got " + actual.left().getDefaultValue());
        helper.assertTrue(Math.abs(actual.right().getDefaultValue() - expectedYaw) < 0.0001f,
                "TaCZ yaw recoil cache must be scaled by Tetra profile; expected " + expectedYaw
                        + " but got " + actual.right().getDefaultValue());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void realTetraModuleNbtResolvesConfiguredRecipe(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        TetraItemStackProfileResolver.SelectedModules modules = TetraItemStackProfileResolver.selectedModules(
                (IModularItem) stack.getItem(), stack, GunModuleSlots.BODY, GunModuleSlots.BARREL,
                GunModuleSlots.MAGAZINE);
        helper.assertTrue(modules != null, "real Tetra module NBT must resolve all three modules");
        helper.assertTrue(ConfiguredAmmoRecipeResolver.select(TaCZinTetra.MODULE_CONFIG,
                (IModularItem) stack.getItem(), stack) != null,
                "resolved modules must select a configured ammo recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void modularGunModulesAmmoAndIdentitySurviveItemStackNbtRoundTrip(GameTestHelper helper) {
        ItemStack original = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(original, GunModuleSlots.BODY,
                "taczintetra/body/rifle", "taczintetra/iron/");
        IModularItem.putModuleInSlot(original, GunModuleSlots.BARREL,
                "taczintetra/barrel/762", "taczintetra/iron/");
        IModularItem.putModuleInSlot(original, GunModuleSlots.MAGAZINE,
                "taczintetra/magazine/standard", "taczintetra/iron/");
        StackIdentity.ensureUniqueId(original);
        ModularGunItem gun = (ModularGunItem) original.getItem();
        gun.setCurrentAmmoCount(original, 7);
        var originalModules = TetraItemStackProfileResolver.selectedModules(gun, original,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        var originalProfile = TetraItemStackProfileResolver.tryResolve(gun, original,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(originalModules != null && originalProfile != null,
                "the original stack must resolve its real Tetra modules before save");
        if (originalModules == null || originalProfile == null) return;

        ItemStack restored = ItemStack.of(original.save(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(restored.getItem() == original.getItem(),
                "vanilla ItemStack NBT must restore the same modular gun item");
        helper.assertTrue(StackIdentity.hasUniqueId(restored)
                        && StackIdentity.of(restored) == StackIdentity.of(original),
                "the persistent per-stack identity must survive vanilla ItemStack NBT");
        helper.assertTrue(gun.getCurrentAmmoCount(restored) == 7,
                "TaCZ native loaded-ammo NBT must survive vanilla ItemStack NBT");

        var restoredModules = TetraItemStackProfileResolver.selectedModules(gun, restored,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        var restoredProfile = TetraItemStackProfileResolver.tryResolve(gun, restored,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(restoredModules != null && restoredProfile != null,
                "restored Tetra module NBT must still resolve a valid runtime profile");
        if (restoredModules == null || restoredProfile == null) return;
        helper.assertTrue(originalModules.equals(restoredModules),
                "selected Tetra module IDs and materials must remain unchanged after save/load");
        helper.assertTrue(originalProfile.equals(restoredProfile),
                "the resolved gun profile must remain unchanged after save/load");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void configuredAmmoRecipesLoadAndCraftThroughNativeTaczAmmoApi(GameTestHelper helper) {
        String[][] expectedAmmo = {
                {"ammo_9mm", "tacz:9mm"},
                {"ammo_45acp", "tacz:45acp"},
                {"ammo_556", "tacz:556"},
                {"ammo_762", "tacz:762x39"},
                {"ammo_338", "tacz:338_lapua_magnum"},
                {"ammo_12g", "tacz:12g"},
                {"ammo_40mm", "tacz:40mm"}
        };
        var probeMenu = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        var craftingGrid = new TransientCraftingContainer(probeMenu, 3, 3);
        for (String[] expected : expectedAmmo) {
            var recipeId = ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, expected[0]);
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
            helper.assertTrue(recipe instanceof AmmoResourceRecipe,
                    "configured ammo recipe must load with its custom serializer: " + recipeId);
            if (!(recipe instanceof AmmoResourceRecipe ammoRecipe)) continue;

            craftingGrid.clearContent();
            var ingredients = ammoRecipe.getIngredients();
            helper.assertTrue(!ingredients.isEmpty() && ingredients.size() <= 9,
                    "ammo recipe must have a craftable number of ingredients: " + recipeId);
            for (int index = 0; index < ingredients.size() && index < 9; index++) {
                ItemStack[] options = ingredients.get(index).getItems();
                helper.assertTrue(options.length > 0,
                        "ammo recipe ingredient must resolve to a registered item: " + recipeId + " slot=" + index);
                if (options.length > 0) craftingGrid.setItem(index, options[0].copyWithCount(1));
            }
            helper.assertTrue(ammoRecipe.matches(craftingGrid, helper.getLevel()),
                    "registered ammo recipe must match its own resolved ingredients: " + recipeId);
            ItemStack output = ammoRecipe.assemble(craftingGrid, helper.getLevel().registryAccess());
            helper.assertTrue(output.getCount() == 8,
                    "ammo recipe must preserve its configured output count: " + recipeId + " count=" + output.getCount());
            IAmmo ammoApi = IAmmo.getIAmmoOrNull(output);
            helper.assertTrue(ammoApi != null,
                    "ammo output must be recognized by TaCZ IAmmo: " + recipeId + " item=" + output.getItem());
            if (ammoApi != null) {
                helper.assertTrue(ResourceLocation.tryParse(expected[1]).equals(ammoApi.getAmmoId(output)),
                        "ammo output must carry the configured native AmmoId: " + recipeId
                                + " expected=" + expected[1] + " actual=" + ammoApi.getAmmoId(output));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void configuredPhysicalPartsAreRegistered(GameTestHelper helper) {
        for (String path : ConfiguredPartIds.localPaths(TaCZinTetra.MODULE_CONFIG)) {
            helper.assertTrue(ModItems.configuredPart(path) != null,
                    "configured physical part must have a registry entry: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void everyConfiguredPhysicalPartHasALoadedCraftingRecipe(GameTestHelper helper) {
        var probeMenu = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        var craftingGrid = new TransientCraftingContainer(probeMenu, 3, 3);
        for (String path : ConfiguredPartIds.localPaths(TaCZinTetra.MODULE_CONFIG)) {
            var recipeId = ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "parts/" + path);
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
            helper.assertTrue(recipe instanceof CraftingRecipe,
                    "survival physical-part recipe must load as a crafting recipe: " + recipeId);
            if (recipe instanceof CraftingRecipe craftingRecipe) {
                ItemStack result = craftingRecipe.getResultItem(helper.getLevel().registryAccess());
                helper.assertTrue(result.getItem() == ModItems.configuredPart(path).get(),
                        "physical-part recipe must output its configured item: " + recipeId);

                craftingGrid.clearContent();
                helper.assertTrue(craftingRecipe instanceof ShapedRecipe,
                        "configured physical-part recipe must remain shaped: " + recipeId);
                if (craftingRecipe instanceof ShapedRecipe shapedRecipe) {
                    var ingredients = shapedRecipe.getIngredients();
                    for (int row = 0; row < shapedRecipe.getHeight(); row++) {
                        for (int column = 0; column < shapedRecipe.getWidth(); column++) {
                            int ingredientIndex = row * shapedRecipe.getWidth() + column;
                            ItemStack[] choices = ingredients.get(ingredientIndex).getItems();
                            if (choices.length > 0) {
                                craftingGrid.setItem(row * 3 + column, choices[0].copyWithCount(1));
                            }
                        }
                    }
                }
                helper.assertTrue(craftingRecipe.matches(craftingGrid, helper.getLevel()),
                        "configured ingredients must match physical-part recipe: " + recipeId);
                ItemStack assembled = craftingRecipe.assemble(craftingGrid, helper.getLevel().registryAccess());
                helper.assertTrue(assembled.getItem() == ModItems.configuredPart(path).get(),
                        "matching survival ingredients must assemble the configured part: " + recipeId);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void everyConfiguredMajorModuleCombinationResolves(GameTestHelper helper) {
        for (ModuleConfig.Body body : TaCZinTetra.MODULE_CONFIG.bodies()) {
            for (ModuleConfig.Barrel barrel : TaCZinTetra.MODULE_CONFIG.barrels()) {
                ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
                IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY,
                        "taczintetra/body/" + body.id(), "taczintetra/iron/");
                IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL,
                        "taczintetra/barrel/" + barrel.id(), "taczintetra/iron/");
                IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE,
                        "taczintetra/magazine/standard", "taczintetra/iron/");
                var profile = TetraItemStackProfileResolver.tryResolve((IModularItem) stack.getItem(), stack,
                        GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
                helper.assertTrue(profile != null && profile.feed().loadedCapacity() > 0,
                        "configured Tetra combination must resolve: " + body.id() + "/" + barrel.id());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void replacingTetraBodyOnSameStackImmediatelyUpdatesResolvedProfile(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(stack);
        ModularGunItem gun = (ModularGunItem) stack.getItem();

        var pistol = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(pistol != null, "the configured pistol body must resolve before replacement");
        if (pistol == null) return;
        helper.assertTrue(pistol.fireControl().roundsPerMinute() == 400
                        && Math.abs(pistol.projectile().damage() - 5.0) < 0.001
                        && Math.abs(pistol.handling().recoilAdd() - 1.0) < 0.001
                        && !TetraItemStackProfileResolver.isTwoHanded(gun, stack),
                "pistol profile must expose its configured RPM, damage, recoil and one-handed property");

        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY,
                "taczintetra/body/rifle", "taczintetra/iron/");
        var rifle = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(rifle != null, "the replacement rifle body must resolve on the same ItemStack");
        if (rifle == null) return;
        helper.assertTrue(rifle.fireControl().roundsPerMinute() == 600
                        && Math.abs(rifle.projectile().damage() - 5.25) < 0.001
                        && Math.abs(rifle.handling().recoilAdd() - 1.3) < 0.001
                        && TetraItemStackProfileResolver.isTwoHanded(gun, stack),
                "replacing the Tetra body must immediately publish the rifle RPM, damage, recoil and two-handed property");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nativeTetraRepairSchematicAcceptsOnlyConfiguredRepairAgent(GameTestHelper helper) {
        ItemStack damaged = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(damaged);
        damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));

        ModularGunItem gun = (ModularGunItem) damaged.getItem();
        String repairSlot = gun.getRepairSlot(damaged);
        var repairSchematic = se.mickelus.tetra.module.SchematicRegistry.getSchematic("taczintetra_repair");
        helper.assertTrue(repairSchematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic,
                "Tetra's registered taczintetra_repair key must resolve to the native repair schematic");
        if (!(repairSchematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic)) return;

        ItemStack configuredRepairAgent = new ItemStack(ModItems.IRON_REPAIR.get());
        ItemStack wrongRepairAgent = new ItemStack(ModItems.GOLD_REPAIR.get());
        int configuredCount = gun.getRepairMaterialCount(damaged, configuredRepairAgent);
        boolean configuredAccepted = repairSchematic.acceptsMaterial(
                damaged, repairSlot, 0, configuredRepairAgent);
        boolean wrongAccepted = repairSchematic.acceptsMaterial(
                damaged, repairSlot, 0, wrongRepairAgent);

        helper.assertTrue(configuredCount == 1 && configuredAccepted,
                "the selected iron modules must use one configured iron repair agent through Tetra's schematic");
        helper.assertTrue(!wrongAccepted,
                "a repair agent configured for another module material must be rejected by Tetra");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nativeTetraRepairSettlementRestoresDurabilityAndConsumesAgent(GameTestHelper helper) {
        ItemStack damaged = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(damaged);
        damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));
        int damageBefore = damaged.getDamageValue();

        ModularGunItem gun = (ModularGunItem) damaged.getItem();
        String repairSlot = gun.getRepairSlot(damaged);
        var schematic = se.mickelus.tetra.module.SchematicRegistry.getSchematic("taczintetra_repair");
        helper.assertTrue(schematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic,
                "the native Tetra repair schematic must be registered before applying a repair");
        if (!(schematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic repair)) return;

        ItemStack repairAgent = new ItemStack(ModItems.IRON_REPAIR.get());
        ItemStack[] materials = {repairAgent};
        helper.assertTrue(repair.isMaterialsValid(damaged, repairSlot, materials),
                "the configured iron repair agent must satisfy Tetra's native material validation");
        ItemStack repaired = repair.applyUpgrade(damaged, materials, true, repairSlot, null);

        helper.assertTrue(repaired.getItem() == damaged.getItem()
                        && repaired.getDamageValue() < damageBefore,
                "Tetra applyUpgrade must return a repaired copy with strictly less durability damage");
        helper.assertTrue(repairAgent.isEmpty(),
                "Tetra applyUpgrade with consumeMaterials=true must consume the configured repair quantity");
        helper.assertTrue(damaged.getDamageValue() == damageBefore,
                "Tetra repair settlement must leave the input stack unchanged and return the repaired copy");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nativeTetraSpecialSocketCanBeInstalledAndRemovedWithoutChangingMajorModules(GameTestHelper helper) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        ItemStack original = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(original);

        var schematic = se.mickelus.tetra.module.SchematicRegistry.getSchematic("taczintetra/special_socket");
        helper.assertTrue(schematic != null,
                "the Tetra special-socket schematic must be registered before lifecycle verification");
        if (schematic == null) return;

        ItemStack inlay = new ItemStack(ModItems.SPECIAL_INLAY.get());
        ItemStack[] materials = {inlay.copy()};
        boolean accepts = schematic.acceptsMaterial(original, GunModuleSlots.SPECIAL, 0, inlay);
        boolean canApply = schematic.canApplyUpgrade(player, original, materials,
                GunModuleSlots.SPECIAL, java.util.Map.of());
        helper.assertTrue(accepts && canApply,
                "the native Tetra schematic must accept the configured special-inlay material");
        if (!accepts || !canApply) return;

        ItemStack installed = schematic.applyUpgrade(original, materials, true,
                GunModuleSlots.SPECIAL, player);
        ModularGunItem gun = (ModularGunItem) original.getItem();
        var installedModule = gun.getModuleFromSlot(installed, GunModuleSlots.SPECIAL);
        helper.assertTrue(installedModule != null,
                "native Tetra upgrade settlement must install the special socket");
        helper.assertTrue(materials[0].isEmpty(),
                "native Tetra upgrade settlement must consume the configured inlay");
        helper.assertTrue(gun.getModuleFromSlot(original, GunModuleSlots.SPECIAL) == null,
                "Tetra upgrade settlement must not mutate the input gun stack");
        if (installedModule == null) return;

        ItemStack[] returned = installedModule.removeModule(installed);
        helper.assertTrue(gun.getModuleFromSlot(installed, GunModuleSlots.SPECIAL) == null,
                "Tetra's native removeModule must clear the special slot");
        helper.assertTrue(gun.getModuleFromSlot(installed, GunModuleSlots.BODY) != null
                        && gun.getModuleFromSlot(installed, GunModuleSlots.BARREL) != null
                        && gun.getModuleFromSlot(installed, GunModuleSlots.MAGAZINE) != null,
                "removing the special Tetra module must preserve all major gun modules");
        helper.assertTrue(returned != null,
                "Tetra's native removeModule contract must return its documented ItemStack array");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void installedMinorModulesParticipateInRuntimeProfile(GameTestHelper helper) {
        ItemStack base = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(base);
        ItemStack attached = base.copy();
        IModularItem.putModuleInSlot(attached, GunModuleSlots.STOCK, "taczintetra/stock", "taczintetra/");
        IModularItem.putModuleInSlot(attached, GunModuleSlots.GRIP, "taczintetra/grip", "taczintetra/");
        IModularItem.putModuleInSlot(attached, GunModuleSlots.OPTIC, "taczintetra/optic", "taczintetra/");
        var baseProfile = TetraItemStackProfileResolver.tryResolve((IModularItem) base.getItem(), base,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        var attachedProfile = TetraItemStackProfileResolver.tryResolve((IModularItem) attached.getItem(), attached,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(baseProfile != null && attachedProfile != null,
                "major and minor module profiles must resolve");
        helper.assertTrue(attachedProfile.handling().recoilAdd() < baseProfile.handling().recoilAdd(),
                "installed stock must affect recoil on the runtime profile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void firingLanePreparationPreservesBlockEntitiesAndClearsOrdinaryBlocks(GameTestHelper helper) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        player.teleportTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
        player.setYRot(0.0F);
        player.setXRot(0.0F);

        var direction = player.getLookAngle().normalize();
        var start = player.getEyePosition().add(direction.scale(0.35));
        var end = player.getEyePosition().add(direction.scale(2.0));
        BlockPos protectedPos = BlockPos.containing(start.add(direction.scale(0.45)));
        BlockPos clearablePos = BlockPos.containing(start.add(direction.scale(1.25)));
        helper.assertTrue(!protectedPos.equals(clearablePos),
                "the test fixture must place the block entity and ordinary block in distinct ray cells");
        if (protectedPos.equals(clearablePos)) return;

        var level = helper.getLevel();
        level.setBlockAndUpdate(protectedPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(clearablePos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.assertTrue(level.getBlockEntity(protectedPos) != null,
                "the protected ray cell must contain a live block entity before cleanup");

        try {
            var cleanup = com.pycoder.taczintetra.client.DevServerAutomation.class.getDeclaredMethod(
                    "clearDamageProbeFiringLane", net.minecraft.server.level.ServerPlayer.class,
                    net.minecraft.world.phys.Vec3.class);
            cleanup.setAccessible(true);
            Class<?>[] parameterTypes = cleanup.getParameterTypes();
            helper.assertTrue(parameterTypes.length == 2,
                    "cleanup method must accept ServerPlayer and Vec3; parameters="
                            + java.util.Arrays.toString(parameterTypes));
            if (parameterTypes.length != 2) return;
            boolean playerMatches = parameterTypes[0].isInstance(player);
            boolean endMatches = parameterTypes[1].isInstance(end);
            helper.assertTrue(playerMatches,
                    "cleanup player argument mismatch: expected=" + parameterTypes[0].getName()
                            + ", actual=" + player.getClass().getName());
            helper.assertTrue(endMatches,
                    "cleanup ray endpoint argument mismatch: expected=" + parameterTypes[1].getName()
                            + ", actual=" + end.getClass().getName());
            if (!playerMatches || !endMatches) return;
            cleanup.invoke(null, player, end);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            helper.fail("the production firing-lane preparation method could not be invoked: " + e
                    + "; player=" + player.getClass().getName() + "; endpoint=" + end.getClass().getName());
            return;
        }

        helper.assertTrue(level.getBlockState(protectedPos).is(net.minecraft.world.level.block.Blocks.CHEST),
                "firing-lane preparation must preserve the obstructing chest block");
        helper.assertTrue(level.getBlockEntity(protectedPos) != null,
                "firing-lane preparation must preserve the chest block entity");
        helper.assertTrue(level.getBlockState(clearablePos).isAir(),
                "firing-lane preparation may clear an ordinary non-block-entity obstruction");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nativeTaczProjectileDoesNotDestroyOrModifyAnObstructingChest(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/rifle", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        ModularGunItem gun = (ModularGunItem) stack.getItem();
        var shooter = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        var target = helper.spawn(EntityType.PIG, new BlockPos(6, 1, 2));
        var level = helper.getLevel();
        var index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        helper.assertTrue(index != null && index.getGunData() != null && index.getBulletData() != null,
                "the configured rifle must resolve its native TaCZ projectile data");
        if (index == null || index.getGunData() == null || index.getBulletData() == null) return;

        AttachmentCacheProperty attachmentCache = new AttachmentCacheProperty();
        attachmentCache.eval(stack, index.getGunData());
        IGunOperator.fromLivingEntity(shooter).updateCacheProperty(attachmentCache);
        EntityKineticBullet bullet = new EntityKineticBullet(level, shooter, stack,
                gun.getGunId(stack), gun.getGunDisplayId(stack), index.getGunData().getAmmoId(),
                false, index.getGunData(), index.getBulletData());
        bullet.setPos(target.getX() - 2.5, target.getY() + target.getBbHeight() * 0.5, target.getZ());
        bullet.setNoGravity(true);
        bullet.shoot(0, -90, 1.5f, new org.joml.Vector2d(0, 0));
        var direction = bullet.getDeltaMovement().normalize();
        var targetDelta = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(bullet.position());
        double distanceAlongShot = targetDelta.dot(direction);
        double lateralMissDistance = targetDelta.subtract(direction.scale(distanceAlongShot)).length();
        helper.assertTrue(distanceAlongShot > 1.25 && lateralMissDistance < 0.25,
                "the test target must lie downrange on the actual TaCZ projectile path; along="
                        + distanceAlongShot + ", lateral=" + lateralMissDistance
                        + ", direction=" + direction + ", targetDelta=" + targetDelta);
        if (distanceAlongShot <= 1.25 || lateralMissDistance >= 0.25) return;

        BlockPos chestPos = BlockPos.containing(bullet.position().add(direction.scale(1.25)));
        level.setBlockAndUpdate(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        var chest = level.getBlockEntity(chestPos);
        helper.assertTrue(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                "the projectile path must contain a real chest block entity");
        if (!(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity)) return;
        chestEntity.setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        var trace = level.clip(new net.minecraft.world.level.ClipContext(
                bullet.position(), bullet.position().add(direction.scale(distanceAlongShot)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, shooter));
        helper.assertTrue(trace.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && trace.getBlockPos().equals(chestPos),
                "the actual projectile path must intersect the chest before the target; hit="
                        + trace.getType() + " at " + trace.getBlockPos() + ", chest=" + chestPos);
        if (trace.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || !trace.getBlockPos().equals(chestPos)) return;

        helper.assertTrue(level.addFreshEntity(bullet), "native TaCZ projectile must enter the test world");

        helper.runAtTickTime(5, () -> {
            helper.assertTrue(level.getBlockState(chestPos).is(net.minecraft.world.level.block.Blocks.CHEST),
                    "a native TaCZ projectile must not remove the obstructing chest");
            helper.assertTrue(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                    "the chest block entity must remain after native projectile collision");
            helper.assertTrue(chestEntity.getItem(0).is(net.minecraft.world.item.Items.DIAMOND)
                            && chestEntity.getItem(0).getCount() == 3,
                    "native projectile collision must preserve chest inventory contents");
            helper.succeed();
        });
    }

    private static void installMinimumMajorModules(ItemStack stack) {
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY,
                "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL,
                "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE,
                "taczintetra/magazine/standard", "taczintetra/iron/");
    }
}
