package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczNativeApiMigrationContractTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }

    @Test
    void modularGunUsesTaczNativeNbtAccessorAndKeepsAmmoMigrationBoundary() throws Exception {
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        assertTrue(item.contains("GunItemDataAccessor"));
        assertTrue(item.contains("GunCurrentAmmoCount"));
        assertTrue(item.contains("taczintetra_ammo"));
        assertFalse(item.contains("setCurrentAmmoCount(ItemStack stack, int count) { setDummyAmmoAmount"));
        assertTrue(item.contains("boolean useDummyAmmo(ItemStack stack) { return false; }"));
    }

    @Test
    void nativeAmmoBridgeDoesNotWriteTheNonstandardAmmoCountKey() throws Exception {
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        String accessor = source("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java");
        assertTrue(item.contains("GunItemDataAccessor.super.getCurrentAmmoCount(stack)"));
        assertFalse(accessor.contains("putInt(\"AmmoCount\""));
    }

    @Test
    void scriptApiBridgeInitializesEveryFieldNeededByTaczNativeCallbacks() throws Exception {
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/ModernKineticGunScriptAPIMixin.java");
        assertTrue(mixin.contains("gunIndex"));
        assertTrue(mixin.contains("gunId"));
        assertTrue(mixin.contains("gunDisplayId"));
        assertTrue(mixin.contains("getCommonGunIndex(gunId)"));
        assertTrue(mixin.contains("new LuaNbtAccessor(itemStack.getTag())"));
        assertTrue(mixin.contains("instanceof ModularGunItem"));
    }

    @Test
    void nativeReloadUsesAbsoluteFeedAndCooldownCheckpoints() throws Exception {
        String adapter = source("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java");
        assertFalse(adapter.contains("(feed + cooldown) * 1000.0F"));
        assertTrue(adapter.contains("Math.max(feed, cooldown)"));
        assertTrue(adapter.contains("new GunReloadEvent(shooter, stack"));
        assertTrue(adapter.contains("new ServerMessageGunReload(shooter.getId(), stack)"));
        assertFalse(adapter.contains("accepted = true;"));
    }

    @Test
    void crosshairAlwaysUsesTheExistingTaczSelectionPath() throws Exception {
        String mixins = source("src/main/resources/taczintetra.mixins.json");
        String config = source("src/main/java/com/pycoder/taczintetra/config/TaczInTetraForgeConfig.java");
        String defaults = source("src/main/resources/defaultconfigs/taczintetra.toml");
        String display = source("src/main/resources/assets/tacz/custom/taczintetra/assets/tacz/display/guns/modular_gun_display.json");
        assertFalse(mixins.contains("RenderCrosshairMixin"));
        assertFalse(config.contains("force_dot_crosshair_for_modular_gun"));
        assertFalse(defaults.contains("force_dot_crosshair_for_modular_gun"));
        assertTrue(display.contains("\"show_crosshair\": true"));
    }

    @Test
    void modularGunRefitScreenKeepsNativeStatsButHidesTaczAttachmentSlots() throws Exception {
        String mixins = source("src/main/resources/taczintetra.mixins.json");
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/ModularGunRefitScreenMixin.java");
        assertTrue(mixins.contains("\"ModularGunRefitScreenMixin\""));
        assertTrue(mixin.contains("@Mixin(GunRefitScreen.class)"));
        assertTrue(mixin.contains("method = \"addAttachmentTypeButtons\""));
        assertTrue(mixin.contains("method = \"addInventoryAttachmentButtons\""));
        assertTrue(mixin.contains("instanceof ModularGunItem"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertTrue(mixin.contains("@Shadow(remap = false)"));
        assertTrue(mixin.contains("private static boolean HIDE_GUN_PROPERTY_DIAGRAMS;"));
        assertTrue(mixin.contains("method = \"m_88315_\""));
        assertFalse(mixin.contains("method = \"init\""));
        assertTrue(mixin.contains("HIDE_GUN_PROPERTY_DIAGRAMS = false;"));
        assertTrue(mixin.contains("implements GunRefitScreenState"));
        Path stateInterfacePath = Path.of("src/main/java/com/pycoder/taczintetra/client/GunRefitScreenState.java");
        assertTrue(Files.exists(stateInterfacePath));
        String stateInterface = Files.readString(stateInterfacePath, StandardCharsets.UTF_8);
        assertTrue(stateInterface.contains("package com.pycoder.taczintetra.client;"));
        assertTrue(mixin.contains("import com.pycoder.taczintetra.client.GunRefitScreenState;"));
        assertTrue(mixins.contains("\"GunRefitScreenLifecycleMixin\""));
        String lifecycleMixin = source("src/main/java/com/pycoder/taczintetra/mixin/GunRefitScreenLifecycleMixin.java");
        assertTrue(lifecycleMixin.contains("@Mixin(Screen.class)"));
        assertTrue(lifecycleMixin.contains("import com.pycoder.taczintetra.client.GunRefitScreenState;"));
        assertTrue(lifecycleMixin.contains("\"removed\""));
        assertTrue(lifecycleMixin.contains("\"m_7861_\""));
        assertTrue(lifecycleMixin.contains("GunRefitScreenState"));
    }

    @Test
    void modularGunUsesTetraModulesForTaCZNativeVisualsWithoutEnablingTaCZRefitSlots() throws Exception {
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        assertTrue(item.contains("AttachmentItemBuilder.create().setId(attachmentId).build()"));
        assertTrue(item.contains("getModuleFromSlot(stack, moduleSlot) == null"),
                "TaCZ native visuals must appear only when the corresponding Tetra module exists");
        assertTrue(item.contains("case SCOPE -> GunModuleSlots.OPTIC;"));
        assertTrue(item.contains("case MUZZLE -> GunModuleSlots.BARREL;"));
        assertTrue(item.contains("case EXTENDED_MAG -> GunModuleSlots.MAGAZINE;"));
        assertTrue(item.contains("case GRIP -> GunModuleSlots.GRIP;"));
        assertTrue(item.contains("case STOCK -> GunModuleSlots.STOCK;"));
        assertTrue(item.contains("getAttachmentTag(ItemStack stack, AttachmentType type) { return new CompoundTag(); }"),
                "TaCZ visual proxies must not inject their native stat modifiers into the Tetra weapon profile");
        assertTrue(item.contains("getAttachmentId(ItemStack stack, AttachmentType type) {\n        return DefaultAssets.EMPTY_ATTACHMENT_ID;"),
                "gameplay attachment identity remains owned by Tetra, while the renderer reads the visual stack");
        assertTrue(item.contains("allowAttachment(ItemStack stack, ItemStack attachment) { return false; }"));
        assertTrue(item.contains("allowAttachmentType(ItemStack stack, AttachmentType type) { return false; }"),
                "visual reuse must not restore TaCZ's attachment editing grid");
    }

    @Test
    void modularGunRefitScreenRendersTheNativeTaczGunInTheClearedPreviewArea() throws Exception {
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/ModularGunRefitScreenMixin.java");
        assertTrue(mixin.contains("method = \"m_88315_\""));
        assertTrue(mixin.contains("at = @At(\"TAIL\")"));
        assertTrue(mixin.contains("nativeRenderer.renderByItem(stack, ItemDisplayContext.FIXED"),
                "the preview must invoke TaCZ's native Bedrock model path rather than its inventory slot sprite");
        assertTrue(mixin.contains("TimelessAPI.getGunDisplay(stack)"),
                "the opt-in GUI evidence must record whether TaCZ resolved this stack's actual display model");
        assertTrue(mixin.contains("instanceof ModularGunItem"));
        assertTrue(mixin.contains("graphics.guiWidth()"));
        assertTrue(mixin.contains("graphics.guiHeight()"));
        assertTrue(mixin.contains("graphics.guiWidth() - 97"),
                "the preview must stay in the right-side region outside the native stat panel");
        assertTrue(mixin.contains("graphics.guiHeight() / 2 - 20"),
                "the preview should be vertically centered after correcting the measured native render offset");
        assertTrue(mixin.contains("scale(48.0F, 48.0F, 1.0F)"),
                "the full native model should be large enough to inspect in the cleared preview area");
        assertTrue(mixin.contains("Screenshot.grab"),
                "the opt-in diagnostic must capture the rendered native screen after the preview call");
    }

    @Test
    void modularGunRegistersTheNativeTaczBedrockRenderer() throws Exception {
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        String model = source("src/main/resources/assets/taczintetra/models/item/modular_gun.json");
        assertTrue(item.contains("initializeClient(Consumer<IClientItemExtensions>"),
                "a custom IGun item must register TaCZ's Forge client renderer extension");
        assertTrue(item.contains("new GunItemRendererWrapper()"),
                "the registered renderer must be TaCZ's native Bedrock gun renderer");
        assertTrue(item.contains("getCustomRenderer()"));
        assertTrue(model.contains("\"parent\": \"builtin/entity\""),
                "the item model must opt into Forge's custom renderer path like TaCZ's native gun item");
    }

    @Test
    void crosshairScreenshotProbeWaitsUntilThePlayerIsInTheLiveGameView() throws Exception {
        String hud = source("src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java");
        assertTrue(hud.contains("minecraft.level == null || minecraft.screen != null"),
                "a reticle screenshot must not be captured while the terrain/loading screen is covering the game view");
        assertTrue(hud.contains("!minecraft.options.getCameraType().isFirstPerson()"),
                "a reticle screenshot must wait for first-person view so the center reticle is visible");

        String automation = source("src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java");
        int setupStart = automation.indexOf("private static void sendDualSetup");
        int setupEnd = automation.indexOf("private static boolean tryPassDualHandGuestGate", setupStart);
        String setup = automation.substring(setupStart, setupEnd);
        assertTrue(setup.contains("taczintetra.heat_overlay_screenshot_probe")
                        && setup.contains("taczintetra.dev_ads_screenshot_probe"),
                "both opt-in reticle and ADS probes must select first-person view");
        assertTrue(setup.contains("minecraft.options.setCameraType(CameraType.FIRST_PERSON)"),
                "the opt-in reticle probe must capture in first-person view");
    }

    @Test
    void nativeAimProbeCanBeEnabledForOpticZoomEvidence() throws Exception {
        String automation = source("src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java");
        int hipfireBaseline = automation.indexOf("taczintetra-ads-hipfire-baseline");
        int nativeAim = automation.indexOf("gunOperator.aim(true)");
        assertTrue(hipfireBaseline >= 0 && hipfireBaseline < nativeAim,
                "ADS evidence must include an un-aimed first-person baseline captured before native aim");
        assertTrue(automation.contains("if (Boolean.getBoolean(\"taczintetra.dev_ads_screenshot_probe\"))"),
                "the ADS visual comparison must stay opt-in");
        int setupStart = automation.indexOf("private static void sendDualSetup");
        int setupEnd = automation.indexOf("private static boolean tryPassDualHandGuestGate", setupStart);
        String setup = automation.substring(setupStart, setupEnd);
        assertTrue(setup.contains("taczintetra.dev_ads_screenshot_probe"),
                "the ADS screenshot probe must force first-person view before it captures the aim state");
        assertTrue(setup.contains("|| Boolean.getBoolean(\"taczintetra.dev_ads_screenshot_probe\")"),
                "the ADS probe must select first-person view even when the reticle probe is disabled");
        assertTrue(automation.contains("RUN_NM_PROBE = Boolean.getBoolean(\"taczintetra.dev_nm_probe\")"),
                "the existing native aim scenario must be explicitly opt-in rather than permanently disabled");
        assertTrue(automation.contains("Boolean.getBoolean(\"taczintetra.dev_ads_screenshot_probe\")"),
                "ADS evidence capture must remain opt-in");
        assertTrue(automation.contains("taczintetra-ads-aim-probe"),
                "the native aim transition must leave a screenshot artifact");
        assertTrue(automation.contains("gunOperator.aim(true)"));
        assertTrue(automation.contains("gunOperator.aim(false)"));
        assertTrue(automation.contains("com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true)"),
                "with TaCZ HoldToAim enabled, the test probe must hold the native aim mapping or TaCZ resets API-only aim on the next client tick");
        assertTrue(automation.contains("com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false)"),
                "the native aim mapping must be released after the ADS evidence window");
        int settledAimStage = automation.indexOf("if (actionProbeStage == 2)");
        int keepAimActive = automation.indexOf("gunOperator.aim(true)", settledAimStage);
        int captureSettledAim = automation.indexOf("taczintetra-ads-aim-probe", settledAimStage);
        assertTrue(settledAimStage >= 0 && keepAimActive > settledAimStage && captureSettledAim > keepAimActive,
                "the dev probe must maintain native aim state through the transition before capturing evidence");
    }

    @Test
    void nativeAimProbeRecordsTheInputGateAndWindowFocusState() throws Exception {
        String automation = source("src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java");
        assertTrue(automation.contains("InputExtraCheck.isInGame()"),
                "the probe must record the same native input gate that may cancel aiming");
        assertTrue(automation.contains("minecraft.mouseHandler.isMouseGrabbed()"),
                "the probe must record whether Minecraft currently owns mouse input");
        assertTrue(automation.contains("minecraft.isWindowActive()"),
                "the probe must record whether the Minecraft window is active");
        assertTrue(automation.contains("screen={}"),
                "the probe must record whether an open screen prevents native aiming");
        assertTrue(automation.contains("TimelessAPI.getCommonGunIndex(gunId)"),
                "the probe must verify the shared TaCZ gun index used by the aim animation");
        assertTrue(automation.contains("getGunData().getAimTime()"),
                "the probe must record the TaCZ native aim-time data for the current gun");
        assertTrue(automation.contains("clientAimingProgress"),
                "the probe must compare TaCZ's public aiming progress with its animation state");
    }

    @Test
    void ctrlReloadForDualPistolsDoesNotAlsoTriggerTaczMainHandReload() throws Exception {
        String mixins = source("src/main/resources/taczintetra.mixins.json");
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/OffhandReloadInputMixin.java");
        assertTrue(mixins.contains("\"OffhandReloadInputMixin\""));
        assertTrue(mixin.contains("@Mixin(value = ReloadKey.class, remap = false)"));
        assertTrue(mixin.contains("method = \"onReloadPress\""));
        assertTrue(mixin.contains("Screen.hasControlDown()"));
        assertTrue(mixin.contains("getMainHandItem().getItem() instanceof ModularGunItem"));
        assertTrue(mixin.contains("getOffhandItem().getItem() instanceof ModularGunItem"));
        assertTrue(mixin.contains("callback.cancel()"));
        String handler = source("src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java");
        assertTrue(handler.contains("decision.routeReloadToOffHand()"));
    }

    @Test
    void modularMainHandReloadUsesExistingTaczLifecycleAdapterAndLeavesNativeWeaponsAlone() throws Exception {
        String mixins = source("src/main/resources/taczintetra.mixins.json");
        Path mixinPath = Path.of("src/main/java/com/pycoder/taczintetra/mixin/MainHandReloadInputMixin.java");
        assertTrue(Files.exists(mixinPath), "the Tetra item must bypass TaCZ's AbstractGunItem-only no-op path");
        String mixin = Files.readString(mixinPath, StandardCharsets.UTF_8);
        assertTrue(mixins.contains("\"MainHandReloadInputMixin\""));
        assertTrue(mixin.contains("@Mixin(value = ReloadKey.class, remap = false)"));
        assertTrue(mixin.contains("method = \"onReloadPress\""));
        assertTrue(mixin.contains("Screen.hasControlDown()"));
        assertTrue(mixin.contains("!Screen.hasShiftDown()"),
                "ordinary main-hand R must not be cancelled before TaCZ's native IGun reload route");
        assertTrue(mixin.contains("getMainHandItem().getItem() instanceof ModularGunItem"));
        assertTrue(mixin.contains("callback.cancel()"));

        String handler = source("src/main/java/com/pycoder/taczintetra/client/ClientReloadInputHandler.java");
        assertTrue(handler.contains("GunInputRouter.shouldRouteReloadToCustomHandler(decision)"));
        assertTrue(handler.contains("ItemStack active = decision.routeReloadToOffHand() ? offHand : main"));
        assertTrue(handler.contains("ReloadIntentFactory.create("));
        assertTrue(handler.contains("ModNetwork.CHANNEL.sendToServer(intent)"));
        String adapter = source("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java");
        assertTrue(adapter.contains("delegate().startReload(data, stack, shooter)"),
                "the custom item must reuse the already existing TaCZ reload lifecycle delegate");
    }

    @Test
    void tetraProgressOverlayIsHiddenWhenModularGunIsHeldInEitherHand() throws Exception {
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/TetraBlockProgressOverlayMixin.java");
        assertTrue(mixin.contains("getMainHandItem()"));
        assertTrue(mixin.contains("getOffhandItem()"));
        assertTrue(mixin.contains("instanceof ModularGunItem"));
        assertTrue(mixin.contains("callback.cancel()"));
    }

    @Test
    void tetraOverlayRuntimeProbeIsOptInAndEmitsAtMostOnce() throws Exception {
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/TetraBlockProgressOverlayMixin.java");
        assertTrue(mixin.contains("Boolean.getBoolean(\"taczintetra.overlay_probe\")"));
        assertTrue(mixin.contains("!overlayProbeLogged"));
        assertTrue(mixin.contains("overlayProbeLogged = true"));
        assertTrue(mixin.contains("TETRA_BLOCK_PROGRESS_PROBE"));
    }

    @Test
    void runtimeIdentityIgnoresTaczCanonicalMutableGunState() throws Exception {
        String identity = source("src/main/java/com/pycoder/taczintetra/network/StackIdentity.java");
        for (String key : new String[]{"GunCurrentAmmoCount", "DummyAmmo", "GunFireMode",
                "HasBulletInBarrel", "HeatAmount", "OverHeated"}) {
            assertTrue(identity.contains("canonical.remove(\"" + key + "\")"), key);
        }
    }

    @Test
    void ammoResourceIdentificationAndCraftedAmmoUseTaczIAmmoApi() throws Exception {
        String insertion = source("src/main/java/com/pycoder/taczintetra/runtime/ResourceInsertionService.java");
        String recipe = source("src/main/java/com/pycoder/taczintetra/recipe/AmmoResourceRecipe.java");
        String probes = source("src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java");
        assertTrue(insertion.contains("IAmmo.getIAmmoOrNull(resource)"));
        assertTrue(insertion.contains("taCZAmmo.getAmmoId(resource)"));
        assertTrue(recipe.contains("IAmmo.getIAmmoOrNull(result)"));
        assertTrue(recipe.contains("ammo.setAmmoId(result, parsed)"));
        assertFalse(recipe.contains("putString(\"AmmoId\""));
        assertTrue(probes.contains("IAmmo.getIAmmoOrNull(ammo)"));
        assertFalse(probes.contains("putString(\"AmmoId\""));
    }

    @Test
    void offhandFireModeUsesTaczNativeFireSelectAfterTheExistingHandValidation() throws Exception {
        String message = source("src/main/java/com/pycoder/taczintetra/network/FireModeIntentMessage.java");
        assertTrue(message.contains("FireModeIntentPolicy.canAccept"));
        assertTrue(message.contains("ModularGunLifecycleAdapter.fireSelect("));
        assertFalse(message.contains("FireModePolicy.next("));
    }

    @Test
    void bulletKillAndBlockHitForwardingUseTaczNativeEvents() throws Exception {
        String handler = source("src/main/java/com/pycoder/taczintetra/compat/GunRuntimeEventHandler.java");
        String bulletMixin = source("src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java");
        assertTrue(handler.contains("onBulletKill(EntityKillByGunEvent event)"));
        assertTrue(handler.contains("onAmmoHitBlock(AmmoHitBlockEvent event)"));
        assertTrue(handler.contains("event.getAmmo()"));
        assertFalse(bulletMixin.contains("method = \"onHitBlock\""));
    }

    @Test
    void cacheBindingIsRecordedAfterNativeCacheEventEvenWhenNativeCacheExists() throws Exception {
        String cache = source("src/main/java/com/pycoder/taczintetra/runtime/GunCacheSynchronizer.java");
        int event = cache.indexOf("AttachmentPropertyManager.postChangeEvent(entity, stack)");
        int bind = cache.indexOf("BINDINGS.put(entity, new CacheBinding(stack, identity))");
        assertTrue(event >= 0 && bind > event);
        assertTrue(cache.contains("if (operator.getCacheProperty() == null)"));
    }

    @Test
    void bulletPropertiesDispatchThroughTaczApiUsingTheActualConstructedGunStack() throws Exception {
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java");
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        assertTrue(mixin.contains("taczintetra$captureSourceGun"));
        assertTrue(mixin.contains("target = \"Lcom/tacz/guns/entity/EntityKineticBullet;modifyProperty(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;\""));
        assertTrue(mixin.contains("@WrapOperation"));
        assertTrue(mixin.contains("@Local(argsOnly = true) ItemStack gunStack"));
        assertTrue(source("build.gradle.kts").contains("compileOnly(\"io.github.llamalad7:mixinextras-common:0.5.4\")"));
        int capture = mixin.indexOf("taczintetra$captureSourceGun");
        int storeSource = mixin.indexOf("taczintetra$sourceGun = gunStack", capture);
        int originalCall = mixin.indexOf("original.call(instance, propertyName, type, defaultValue)", capture);
        assertTrue(capture >= 0 && storeSource > capture && originalCall > storeSource);
        assertTrue(mixin.contains("taczintetra$dispatchNativeProperty"));
        assertTrue(mixin.contains("gun.modifyProperty(holder, source, shooter, propertyName, type, defaultValue)"));
        assertTrue(item.contains("GunProperties.AMMO_SPEED.name()"));
        assertTrue(item.contains("GunProperties.EFFECTIVE_RANGE.name()"));
        assertTrue(item.contains("GunProperties.ARMOR_IGNORE.name()"));
        assertFalse(mixin.contains("speed *= (float) projectile.velocity()"));
        assertFalse(mixin.contains("distanceAmount *= (float) projectile.range()"));
        assertFalse(mixin.contains("armorIgnore = Math.max(0, Math.min(1, armorIgnore +"));
    }

    @Test
    void configuredDamagePreservesTaczDistanceCurveAndUsesNativeShotMultiplier() throws Exception {
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        String mixin = source("src/main/java/com/pycoder/taczintetra/mixin/EntityKineticBulletMixin.java");
        String gameTests = source("src/main/java/com/pycoder/taczintetra/gametest/TaCZinTetraGameTests.java");
        assertFalse(item.contains("GunProperties.DAMAGE.name().equals(propertyName)"),
                "the item callback must not replace TaCZ's per-distance DAMAGE value with an absolute value");
        assertTrue(mixin.contains("gun.modifyProperty(holder, source, shooter, propertyName, type, defaultValue)"));
        assertTrue(mixin.contains("setShotDamageMultiplier(configuredDamage / baseDamage)"),
                "apply the resolved Tetra profile through TaCZ's native projectile multiplier");
        assertTrue(gameTests.contains("nativeNearDamage * damageScale"),
                "the Forge GameTest must assert the native distance tier is scaled exactly once");
        assertFalse(mixin.contains("taczintetra$useConfiguredPerHitDamage"));
        assertFalse(mixin.contains("method = \"getDamage(Lnet/minecraft/world/phys/Vec3;)F\""));
    }

    @Test
    void modularGunDoesNotRenderTetrasBlockProgressBarAsASecondCrosshair() throws Exception {
        String item = source("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java");
        assertTrue(item.contains("@Override public float getBlockProgress(ItemStack stack, LivingEntity entity)"));
        assertTrue(item.contains("getBlockProgress(ItemStack stack, LivingEntity entity) { return 0.0f; }"));
    }

    @Test
    void tetraProgressOverlayIsSuppressedOnlyWhileUsingAModularGun() throws Exception {
        Path mixinPath = Path.of("src/main/java/com/pycoder/taczintetra/mixin/TetraBlockProgressOverlayMixin.java");
        assertTrue(Files.exists(mixinPath), "the Tetra progress overlay suppression hook must exist");
        String mixin = Files.readString(mixinPath, StandardCharsets.UTF_8);
        String config = source("src/main/resources/taczintetra.mixins.json");
        assertTrue(mixin.contains("BlockProgressOverlay.class, remap = false"));
        assertTrue(mixin.contains("getMainHandItem().getItem() instanceof ModularGunItem"));
        assertTrue(mixin.contains("callback.cancel()"));
        assertTrue(mixin.contains("if ((mainGun || offGun) && Boolean.getBoolean(\"taczintetra.overlay_probe\")"),
                "the overlay diagnostic must wait until a modular gun is actually held before recording a sample");
        assertTrue(config.contains("\"TetraBlockProgressOverlayMixin\""));
    }

    @Test
    void modularGunHidesOnlyTaczHeatBarAndKeepsNativeTaczReticle() throws Exception {
        String handler = source("src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java");
        String config = source("src/main/resources/taczintetra.mixins.json");
        assertTrue(handler.contains("RenderGuiOverlayEvent.Pre"));
        assertTrue(handler.contains("ResourceLocation.fromNamespaceAndPath(\"tacz\", \"tac_heat_bar\")"));
        assertTrue(handler.contains("ModularGunItem"));
        assertTrue(handler.contains("event.setCanceled(true)"));
        assertFalse(handler.contains("VanillaGuiOverlay.CROSSHAIR"));
        assertTrue(config.contains("TetraBlockProgressOverlayMixin"));
    }

    @Test
    void heatOverlayScreenshotProbeIsExplicitlyOptInAndOneShot() throws Exception {
        String handler = source("src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java");
        assertTrue(handler.contains("Boolean.getBoolean(\"taczintetra.heat_overlay_screenshot_probe\")"));
        assertTrue(handler.contains("Screenshot.grab(minecraft.gameDirectory"));
        assertTrue(handler.contains("taczintetra-heat-overlay-probe"));
        assertTrue(handler.contains("heatOverlayScreenshotSent"));
    }

    @Test
    void refitScreenProbeOpensNativeTaczScreenAndCapturesItOnlyWhenOptedIn() throws Exception {
        String handler = source("src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java");
        assertTrue(handler.contains("Boolean.getBoolean(\"taczintetra.refit_screen_screenshot_probe\")"));
        assertTrue(handler.contains("new GunRefitScreen()"));
        assertTrue(handler.contains("Screenshot.grab(minecraft.gameDirectory, \"taczintetra-refit-screen-probe\""));
        assertTrue(handler.contains("refitScreenScreenshotSent"));
    }

    @Test
    void gunpackExporterIncludesEveryBodyFamilyDisplayJson() throws Exception {
        String exporter = source("src/main/java/com/pycoder/taczintetra/compat/TaCZGunpackExporter.java");
        String resourceRoot = "src/main/resources/assets/tacz/custom/taczintetra/assets/tacz/display/guns/";
        for (String display : new String[]{
                "modular_gun_display.json",
                "modular_gun_rifle_display.json",
                "modular_gun_machine_gun_display.json",
                "modular_gun_shotgun_display.json",
                "modular_gun_sniper_display.json"}) {
            assertTrue(Files.isRegularFile(Path.of(resourceRoot + display)),
                    "body-family display resource must be bundled: " + display);
            assertTrue(exporter.contains("assets/tacz/display/guns/" + display),
                    "TaCZ's native pack exporter must copy body-family display " + display);
        }
    }
}
