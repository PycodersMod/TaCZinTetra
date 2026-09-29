package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModularGunLifecycleContractTest {
    @Test
    void adapterExposesEveryStrictTaczLifecycleOperation() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        for (String method : new String[]{"shoot", "startBolt", "tickBolt", "startReload", "tickReload", "interruptReload", "fireSelect", "melee"}) {
            assertTrue(source.contains(" " + method + "("), method);
        }
    }

    @Test
    void modularGunDoesNotEnterTaczNativeCraftingOrAttachmentFrontEnd() throws Exception {
        String item = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        String display = Files.readString(Path.of(
                "src/main/resources/assets/tacz/custom/taczintetra/data/taczintetra/data/guns/modular_gun_data.json"),
                StandardCharsets.UTF_8);
        assertTrue(item.contains("extends ItemModularHandheld"));
        assertTrue(!item.contains("extends ModernKineticGunItem"));
        assertTrue(display.contains("\"allow_attachment_types\": []"));
    }

    @Test
    void shootMixinUsesTaczOverheatedResultBeforeDelegating() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/mixin/LivingEntityShootMixin.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("isOverheatLocked(stack)"));
        assertTrue(source.contains("ShootResult.OVERHEATED"));
        assertTrue(source.contains("isBroken(stack)"));
    }

    @Test
    void shootAdapterRepeatsBrokenAndOverheatGuardsForDirectCallers() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        int methodStart = source.indexOf("public static boolean shoot(");
        int methodEnd = source.indexOf("\n    public static boolean startBolt", methodStart);
        String shoot = source.substring(methodStart, methodEnd);
        assertTrue(shoot.contains("gun.isBroken(stack)"));
        assertTrue(shoot.contains("gun.isOverheatLocked(stack)"));
        assertTrue(shoot.indexOf("gun.isBroken(stack)") < shoot.indexOf("getCurrentAmmoCount"));
    }

    @Test
    void shootAdapterUsesTetraDurabilitySemanticsOnServer() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("!shooter.level().isClientSide"));
        assertTrue(source.contains("modular.applyDamage"));
        assertTrue(source.contains("DurabilityCostPolicy.cost(settledShots, 1, 1)"));
    }

    @Test
    void heatRuntimeStoresTickAndUsesContinuousCooling() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("taczintetra_heat_tick"));
        assertTrue(source.contains("HeatModel.cool"));
        assertTrue(source.contains("HeatModel.canUnlock"));
        assertTrue(source.contains("addShotHeat"));
        assertTrue(source.contains("independentShots(stack)"));
    }

    @Test
    void heatTickDoesNotRewriteHeldStackNbtEveryServerTick() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        int methodStart = source.indexOf("public void tickHeat(ItemStack stack, long gameTime)");
        int methodEnd = source.indexOf("\n    private void updateOverheat", methodStart);
        String tickHeat = source.substring(methodStart, methodEnd);
        assertTrue(tickHeat.contains("RUNTIME_HEAT"));
        assertTrue(!tickHeat.contains("putLong(HEAT_TICK, gameTime)"));
    }

    @Test
    void reloadLifecyclePublishesCancelableReloadEventBeforeDelegating() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("GunRuntimeEvent.Type.RELOAD"));
        assertTrue(source.contains("MinecraftForge.EVENT_BUS.post"));
    }

    @Test
    void reloadDurationUsesResolvedProfileMultiplier() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("profile.handling().reloadTimeMultiplier()"));
        assertTrue(source.contains("ReloadTimingPolicy.durationMillis"));
    }

    @Test
    void bulletOutcomeBridgeOnlyAcceptsTaczKineticBullets() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/GunRuntimeEventHandler.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("EntityKineticBullet"));
        assertTrue(source.contains("EntityHurtByGunEvent.Pre"));
        assertTrue(source.contains("LivingDeathEvent"));
        assertTrue(source.contains("GunRuntimeEvent.Type.BULLET_HIT"));
        assertTrue(source.contains("GunRuntimeEvent.Type.BULLET_HEADSHOT"));
        assertTrue(source.contains("GunRuntimeEvent.Type.BULLET_KILL"));
        assertTrue(source.contains("event.setCanceled(true)"));
    }

    @Test
    void bulletOutcomeEventsUseTheFiringGunSnapshotAndModularGuard() throws Exception {
        String handler = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/GunRuntimeEventHandler.java"), StandardCharsets.UTF_8);
        String bullet = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java"), StandardCharsets.UTF_8);
        assertTrue(handler.contains("GunSourceAccessor"));
        assertTrue(handler.contains("ModularGunItem"));
        assertTrue(handler.contains("sourceGun"));
        assertTrue(bullet.contains("implements GunSourceAccessor"));
        assertTrue(bullet.contains("gunStack.copy()"));
        assertTrue(bullet.contains("sourceGun()"));
        assertFalse(bullet.contains("actor.getMainHandItem())))"));
    }

    @Test
    void explosionBridgeTargetsNativeExplosionFlag() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("@Mixin(value = EntityKineticBullet.class, remap = false)"));
        assertTrue(source.contains("private boolean explosion"));
        assertTrue(source.contains("GunRuntimeEvent.Type.EXPLOSION_HIT"));
        assertTrue(source.contains("ci.cancel()"));
    }

    @Test
    void configuredExplosionRadiusMustReachTheNativeBulletFields() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ConfiguredProjectilePolicy.explosionRadius"));
        assertTrue(source.contains("explosionRadius = configuredExplosionRadius"));
        assertTrue(source.contains("explosion = configuredExplosionRadius > 0"));
    }

    @Test
    void configuredMotionOverridesReachNativeBulletFields() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ConfiguredProjectilePolicy.motionOverrides"));
        assertTrue(source.contains("life = (int)"));
        assertTrue(source.contains("gravity = motion.gravity()"));
        assertTrue(source.contains("friction = motion.friction()"));
        assertTrue(source.contains("igniteEntity = true"));
        assertTrue(source.contains("igniteBlock = true"));
    }

    @Test
    void bulletBridgeUsesTheFiringStackProfile() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("ItemStack gunStack"));
        assertTrue(source.contains("getDamageAmount()"));
        assertTrue(source.contains("setShotDamageMultiplier"));
        assertTrue(source.contains("profile.projectile().damage()"));
    }

    @Test
    void configuredDamageIsThePerHitValueInsteadOfASecondNativeDistanceCurve() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("method = \"getDamage(Lnet/minecraft/world/phys/Vec3;)F\""));
        assertTrue(source.contains("cancellable = true"));
        assertTrue(source.contains("ci.setReturnValue((float) profile.projectile().damage())"));
    }

    @Test
    void nativeProjectileUsesTheSelectedBarrelAmmoId() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ConfiguredAmmoRecipeResolver.projectileId"));
        assertTrue(source.contains("ConfiguredAmmoPolicy.resolve"));
        assertTrue(source.contains("ResourceLocation ammoId"));
        assertTrue(source.contains("ammoId, gun.getGunId(stack)"));
        assertFalse(source.contains("gunData.getAmmoId(), gun.getGunId(stack)"));
    }

    @Test
    void nativeProjectileUsesAConfiguredBulletDataSnapshotForTheSelectedBarrel() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ConfiguredProjectilePolicy.nativeBulletData"));
        assertTrue(source.contains("gunIndex.getBulletData()"));
        int snapshot = source.indexOf("nativeBulletData");
        int constructor = source.indexOf("new EntityKineticBullet", snapshot);
        assertTrue(snapshot >= 0 && constructor > snapshot);
        assertTrue(source.substring(constructor, Math.min(source.length(), constructor + 700))
                .contains("bulletData"));
    }

    @Test
    void malformedExplicitAmmoConfigurationCannotFallBackToTheNativeTemplate() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ConfiguredAmmoPolicy.resolve"));
        assertTrue(source.contains("configuredAmmoText != null"));
        assertFalse(source.contains("configuredAmmoId == null ? gunData.getAmmoId() : configuredAmmoId"));
    }

    @Test
    void clientFireBridgeUsesTheFiringStackRecoilProfile() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/ModularGunRecoilHandler.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("GunFireEvent"));
        assertTrue(source.contains("profile.handling().recoilAdd()"));
        assertTrue(source.contains("setXRot"));
    }

    @Test
    void hudOnlyUsesTheMainHandModularGun() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("getMainHandItem()"));
        assertTrue(source.contains("instanceof ModularGunItem"));
        assertTrue(source.contains("getCurrentAmmoCount"));
        assertTrue(source.contains("getMaxDummyAmmoAmount"));
        assertTrue(source.contains("ResourceInsertionService.ITEM_CHANNEL"));
    }

    @Test
    void hudCanReadAuthoritativeAddonResourceAmounts() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("ExternalResourceService.adapterFor"));
        assertTrue(source.contains("minecraft.player"));
    }

    @Test
    void devAutomationIsExplicitlyOptInAndUsesClientNativeInputPath() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java");
        assertTrue(Files.exists(sourcePath));
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("taczintetra.dev_automation"));
        assertTrue(source.contains("sendCommand"));
        assertTrue(source.contains("IClientPlayerGunOperator"));
        assertTrue(source.contains(".shoot()"));
        assertTrue(source.contains(".draw(minecraft.player.getMainHandItem())"));
        assertTrue(source.contains("drawRequested"));
        assertTrue(source.contains("ShootResult"));
        assertTrue(source.contains("cachePresent"));
        assertTrue(source.contains("ClientPlayerNetworkEvent.LoggingIn"));
        assertTrue(source.contains("item replace entity @p weapon.mainhand with taczintetra:starter_pistol"));
        assertTrue(server.contains("taczintetra_ammo"));
        assertTrue(server.contains("taczintetra_max_ammo"));
        assertTrue(source.contains("GunCurrentAmmoCount:12"));
        assertTrue(source.contains("DummyAmmo:12"));
        assertTrue(source.contains("MaxDummyAmmo:12"));
        assertTrue(source.contains("tacz:modern_kinetic_gun"));
        assertTrue(source.contains("offhandReloadDelay = 10"));
        assertTrue(source.contains("after shoot settlement"));
        assertTrue(source.contains("ordinaryProbeStage == 0 && dualModeProbeSent"));
        assertTrue(source.contains("actionProbeStage"));
        assertTrue(source.contains("gunOperator.aim(true)"));
        assertTrue(source.contains("gunOperator.aim(false)"));
        assertTrue(source.contains("gunOperator.bolt()"));
        assertTrue(source.contains("gunOperator.fireSelect()"));
        assertTrue(source.contains("gunOperator.melee()"));
    }

    @Test
    void optionalTetraExplosionOutcomeHasNullTypeGuard() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/taczintetra.mixins.json"), StandardCharsets.UTF_8);
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/mixin/ExplosionOutcomeMixin.java"), StandardCharsets.UTF_8);
        assertTrue(config.contains("ExplosionOutcomeMixin"));
        assertTrue(source.contains("type == null"));
        assertTrue(source.contains("setReturnValue(false)"));
    }

    @Test
    void devAutomationRunSwitchIsExplicitlyOptIn() throws Exception {
        String buildScript = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        assertTrue(buildScript.contains("titAutomation"));
        assertTrue(buildScript.contains("taczintetra.dev_automation"));
        assertTrue(buildScript.contains("titUsername"));
        assertTrue(buildScript.contains("--username"));
    }

    @Test
    void lanHostProbeIsExplicitlyOptIn() throws Exception {
        String buildScript = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"), StandardCharsets.UTF_8);
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(buildScript.contains("titLanHost"));
        assertTrue(buildScript.contains("taczintetra.dev_lan_host"));
        assertTrue(client.contains("dev_lan_host"));
        assertTrue(client.contains("publish true survival 25565"));
        assertTrue(client.contains("setUsesAuthentication(false)"));
        assertTrue(client.contains("isolated probe"));
        assertTrue(client.contains("item replace entity @p weapon.mainhand with taczintetra:starter_pistol"));
        assertTrue(client.contains("tag @p add taczintetra_dev_shotgun"));
        assertTrue(client.contains("delayed replay probe sent duplicate reload"));
        assertTrue(client.contains("delayedReplayTicks = 5"));
        assertTrue(client.contains("normal main-hand reload intent"));
        int reloadIntentIndex = client.indexOf("new ReloadIntentMessage(false, false");
        int reloadSentIndex = client.indexOf("reloadProbeSent = true", reloadIntentIndex);
        assertTrue(reloadIntentIndex >= 0);
        assertTrue(reloadSentIndex > reloadIntentIndex);
        assertTrue(client.contains("sendToServer(intent)"));
        assertTrue(Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/ReloadIntentMessage.java"), StandardCharsets.UTF_8)
                .contains("reload intent rejected"));
        assertTrue(Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeCoordinator.java"), StandardCharsets.UTF_8)
                .contains("reload interrupted by stack identity change"));
        assertTrue(server.contains("seedProbeAmmo"));
    }

    @Test
    void devProbeExercisesIndependentMajorModuleMaterials() throws Exception {
        String server = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(server.contains("GunModuleSlots.BODY, bodyId, \"taczintetra/wood/\""));
        assertTrue(server.contains("GunModuleSlots.BARREL, barrelId, \"taczintetra/iron/\""));
        assertTrue(server.contains("GunModuleSlots.MAGAZINE, \"taczintetra/magazine/standard\", \"taczintetra/gold/\""));
    }

    @Test
    void repairPathUsesTheSelectedMajorModuleMaterial() throws Exception {
        String item = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        assertTrue(item.contains("modules.body().materialId()"));
        assertTrue(item.contains("modules.barrel().materialId()"));
        assertTrue(item.contains("modules.magazine().materialId()"));
    }

    @Test
    void dualPistolRightClickUsesAnOffhandServerIntent() throws Exception {
        String network = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/network/OffhandShootIntentMessage.java"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java"), StandardCharsets.UTF_8);
        assertTrue(network.contains("PLAY_TO_SERVER"));
        assertTrue(network.contains("getOffhandItem"));
        assertTrue(network.contains("data.currentGunItem"));
        assertTrue(network.contains("operator.shoot"));
        assertTrue(network.contains("getCurrentAmmoCount"));
        assertTrue(network.contains("getHeatAmount"));
        assertTrue(network.contains("data.currentGunItem = previous"));
        assertTrue(client.contains("GLFW_MOUSE_BUTTON_RIGHT"));
        assertTrue(client.contains("OffhandShootIntentMessage"));
        assertTrue(client.contains("event.setCanceled(true)"));
        String lifecycle = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(lifecycle.contains("restoreDefaultGunSupplier"));
        assertTrue(lifecycle.contains("shooter::getMainHandItem"));
        assertTrue(client.contains("!mainTwoHanded"));
        assertTrue(client.contains("event.setCanceled(true)"));
        String coordinator = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeCoordinator.java"), StandardCharsets.UTF_8);
        assertTrue(coordinator.contains("entity.getMainHandItem() == stack"));
        assertTrue(coordinator.contains("identity != offIdentity"));
        assertTrue(coordinator.contains("identity != mainIdentity"));
    }

    @Test
    void offhandShootRejectsBothOwnAndOtherHandReloadOwnership() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/OffhandShootIntentMessage.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("handReloading(sender, InteractionHand.OFF_HAND)"));
        assertTrue(source.contains("otherHandReloading(sender, InteractionHand.OFF_HAND)"));
    }

    @Test
    void offhandFireModeIntentRejectsBothOwnAndOtherHandReloadOwnership() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/FireModeIntentMessage.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("handReloading(sender, hand)"));
        assertTrue(source.contains("otherHandReloading(sender, hand)"));
    }

    @Test
    void dualPistolRmbIsReservedForOffhandShotAndCancelsAimRelease() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java"), StandardCharsets.UTF_8);
        int methodStart = source.indexOf("public static void onMouseButton");
        int methodEnd = source.indexOf("\n    @SubscribeEvent", methodStart + 1);
        String mouseHandler = source.substring(methodStart, methodEnd);
        assertTrue(mouseHandler.contains("dualPistol"));
        assertTrue(mouseHandler.contains("GLFW.GLFW_PRESS"));
        assertTrue(mouseHandler.contains("GLFW.GLFW_RELEASE"));
        assertTrue(mouseHandler.contains("OffhandShootIntentMessage"));
        assertTrue(mouseHandler.contains("event.setCanceled(true)"));
    }

    @Test
    void devAutomationObservesNativeTaczProjectileWithoutChangingReleasePath() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("EntityJoinLevelEvent"));
        assertTrue(source.contains("EntityKineticBullet"));
        assertTrue(source.contains("taczintetra.dev_automation"));
        assertTrue(source.contains("event.getLevel().isClientSide()"));
    }

    @Test
    void modularGunReturnsTaCZEmptyAttachmentIdsInsteadOfNull() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("DefaultAssets.EMPTY_ATTACHMENT_ID"));
        assertTrue(!source.contains("getAttachmentId(ItemStack stack, AttachmentType type) { return null; }"));
        assertTrue(!source.contains("getBuiltInAttachmentId(ItemStack stack, AttachmentType type) { return null; }"));
    }

    @Test
    void modularGunUsesTheRegisteredTaCZDisplayCacheId() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("getGunDisplayId(ItemStack stack) { return DISPLAY_ID; }"));
        assertTrue(source.contains("ResourceLocation.fromNamespaceAndPath(\"tacz\", \"modular_gun_display\")"));
    }

    @Test
    void nativeScriptApiReceivesRegisteredTaczGunProxyForModularStack() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/mixin/ModernKineticGunScriptAPIMixin.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("ModernKineticGunScriptAPI.class"));
        assertTrue(source.contains("ModItems.MODERN_KINETIC_GUN.get()"));
        assertTrue(source.contains("abstractGunItem"));
        assertTrue(source.contains("setItemStack"));
    }

    @Test
    void modularShootUsesNativeScriptApiDirectlyAfterDelegateWrapperMissesShootOnce() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("new ModernKineticGunScriptAPI()"));
        assertTrue(source.contains("shootOnce"));
        assertTrue(source.contains("setDataHolder(data)"));
    }

    @Test
    void modularShootDoesNotAskTaczToConsumeInventoryAmmo() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("spawnNativeProjectile(data, gun, stack,"));
        assertTrue(source.contains("gun.reduceCurrentAmmoCount(stack, settledShots)"));
        assertTrue(source.contains("ShotSettlementPolicy.consumedRounds"));
        assertTrue(source.contains("ReloadRuntimeCoordinator.handReloading"));
        assertTrue(source.contains("ReloadRuntimeCoordinator.otherHandReloading"));
    }

    @Test
    void modularShootUsesTaczNativeProjectileConstruction() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("new EntityKineticBullet"));
        assertTrue(source.contains("doBulletSpread"));
        assertTrue(source.contains("addFreshEntity"));
    }

    @Test
    void modularProjectileUsesResolvedVelocityForEverySpawnPath() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("profile.projectile().velocity()"));
        assertTrue(source.contains("shootFromRotation(shooter, pitch.get(), yaw.get(), 0.0f,\n                        velocity"));
        assertTrue(source.contains("doBulletSpread(data, stack, shooter, bullet, index,\n                        velocity"));
        assertFalse(source.contains("shootFromRotation(shooter, pitch.get(), yaw.get(), 0.0f,\n                        1.0f"));
    }

    @Test
    void missingTaczGunDataCannotConsumeModularShot() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("int createdProjectiles = -1"));
        assertTrue(source.contains("if (isModular(stack) && createdProjectiles <= 0) return false"));
        assertTrue(source.contains("if (gunIndex == null || gunIndex.getGunData() == null || gunIndex.getBulletData() == null) return 0"));
    }

    @Test
    void modularGunCacheIsScopedToTheCurrentStackInsteadOfAnyNonNullOperatorCache() throws Exception {
        String server = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/runtime/GunHeatTickHandler.java"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/client/GunCacheTickHandler.java"), StandardCharsets.UTF_8);
        String synchronizer = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/runtime/GunCacheSynchronizer.java"), StandardCharsets.UTF_8);
        assertTrue(server.contains("GunCacheSynchronizer.ensure"));
        assertTrue(synchronizer.contains("WeakHashMap"));
        assertTrue(synchronizer.contains("previousStack == stack"));
        assertTrue(synchronizer.contains("StackIdentity.of(stack)"));
        assertTrue(synchronizer.contains("AttachmentPropertyManager.postChangeEvent"));
        assertTrue(synchronizer.contains("cache.eval"));
        assertTrue(client.contains("GunCacheSynchronizer.ensure"));
    }

    @Test
    void modularShotUpdatesNativeShooterTimestampAndCountState() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("data.lastShootTimestamp"));
        assertTrue(source.contains("data.shootTimestamp"));
        assertTrue(source.contains("data.shootCount"));
    }

    @Test
    void modularShotKeepsNativeDrawCooldownGate() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/LivingEntityShootMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("getDrawCoolDown()"));
    }

    @Test
    void modularShotKeepsNativeNetworkTimestampGate() throws Exception {
        String adapter = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        String mixin = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/LivingEntityShootMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(adapter.contains("acceptsNetworkTimestamp"));
        assertTrue(mixin.contains("acceptsNetworkTimestamp"));
        assertTrue(mixin.contains("ShootResult.NETWORK_FAIL"));
    }

    @Test
    void modularShotPublishesNativeShootEventAndTrackingBroadcastBeforeProjectiles() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        int event = source.indexOf("new GunShootEvent(");
        int broadcast = source.indexOf("new ServerMessageGunShoot(");
        int send = source.indexOf("NetworkHandler.sendToTrackingEntity(");
        int projectile = source.indexOf("createdProjectiles = spawnNativeProjectile");
        assertTrue(event >= 0);
        assertTrue(broadcast > event);
        assertTrue(send > event);
        assertTrue(broadcast > send);
        assertTrue(projectile > broadcast);
    }

    @Test
    void modularProjectilePathsPassConfiguredSpreadInsteadOfZeroOrVelocityTwice() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("SpreadPolicy.total"));
        assertTrue(source.contains("shootFromRotation(shooter, pitch.get(), yaw.get(), 0.0f,\n                        velocity, spread);"));
        assertTrue(source.contains("velocity, spread, pitch.get(), yaw.get()"));
        assertFalse(source.contains("velocity, velocity, pitch.get(), yaw.get()"));
    }

    @Test
    void clientCacheProbeConsidersAnOffhandOnlyModularGun() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/GunCacheTickHandler.java"), StandardCharsets.UTF_8);
        assertTrue(client.contains("getOffhandItem"));
    }

    @Test
    void offhandFireDoesNotOverwriteMainhandCameraRecoil() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/ModularGunRecoilHandler.java"), StandardCharsets.UTF_8);
        assertTrue(client.contains("getOffhandItem() == event.getGunItemStack()"));
    }

    @Test
    void modularReloadSeedsNativeStateBeforeDelegating() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        int state = source.indexOf("data.reloadStateType =");
        int timestamp = source.indexOf("data.reloadTimestamp = System.currentTimeMillis()", state);
        int delegate = source.indexOf("delegate().startReload(data, stack, shooter)", timestamp);
        int fallbackState = source.indexOf("data.reloadStateType =", delegate);
        int fallbackTimestamp = source.indexOf("data.reloadTimestamp = System.currentTimeMillis()", fallbackState);
        assertTrue(state >= 0);
        assertTrue(timestamp > state);
        assertTrue(delegate > timestamp);
        assertTrue(fallbackState > delegate);
        assertTrue(fallbackTimestamp > fallbackState);
    }

    @Test
    void modularReloadKeepsNativeAmmoShadowSynchronizedWithTitAuthority() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"),
                StandardCharsets.UTF_8);
        int start = source.indexOf("public static boolean startReload(");
        int startDelegate = source.indexOf("delegate().startReload(data, stack, shooter)", start);
        int startSync = source.indexOf("syncNativeAmmo(gun, stack)", start);
        int tick = source.indexOf("public static ReloadState tickReload(");
        int tickDelegate = source.indexOf("delegate().tickReload(data, stack, shooter)", tick);
        int tickSync = source.indexOf("syncNativeAmmo(gun, stack)", tick);
        assertTrue(start >= 0);
        assertTrue(startSync > start && startSync < startDelegate);
        assertTrue(tick >= 0);
        assertTrue(tickSync > tick && tickSync < tickDelegate);
        int settlement = source.indexOf("ReloadRuntimeSettlementService.settleIfComplete(gun, stack, shooter)", tick);
        int terminalSync = source.indexOf("syncNativeAmmo(gun, stack)", settlement);
        assertTrue(settlement > tickDelegate);
        assertTrue(terminalSync > settlement);
    }

    @Test
    void modularShootUsesConfiguredNmProjectileCountWithoutPelletDurabilityMultiplier() throws Exception {
        String item = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"), StandardCharsets.UTF_8);
        String adapter = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java"), StandardCharsets.UTF_8);
        assertTrue(item.contains("ShotCountCalculator.totalProjectiles"));
        assertTrue(item.contains("pellets_per_round"));
        assertTrue(adapter.contains("projectileCount(stack, independentShots)"));
        assertTrue(adapter.contains("for (int index = 0; index < projectileCount; index++)"));
        assertTrue(adapter.contains("DurabilityCostPolicy.cost(settledShots, 1, 1)"));
    }
}
