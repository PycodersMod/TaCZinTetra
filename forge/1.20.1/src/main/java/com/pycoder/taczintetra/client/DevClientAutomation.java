package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import se.mickelus.tetra.blocks.workbench.gui.WorkbenchScreen;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HolosphereEntryStore;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import com.pycoder.taczintetra.network.FireModeIntentMessage;
import com.pycoder.taczintetra.network.OffhandShootIntentMessage;
import com.pycoder.taczintetra.network.ReloadIntentMessage;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.network.ModNetwork;
import com.pycoder.taczintetra.logic.NetworkTimestampPolicy;
import org.slf4j.Logger;

/**
 * 仅用于开发的客户端冒烟测试。只有显式启用 JVM 属性后才会执行，
 * 不会影响发布版和普通开发客户端。
 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevClientAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_automation");
    private static final boolean LAN_HOST = Boolean.getBoolean("taczintetra.dev_lan_host");
    private static final boolean HOLD_HOLO = Boolean.getBoolean("taczintetra.dev_hold_holo");
    private static final boolean F06_ZERO_AMMO_PROBE = Boolean.getBoolean("taczintetra.dev_f06_zero_ammo_probe");
    private static final boolean F06_RELOAD_INTERRUPT_PROBE = Boolean.getBoolean(
            "taczintetra.dev_f06_reload_interrupt_probe");
    private static final boolean NATIVE_RELOAD_KEY_PROBE = Boolean.getBoolean(
            "taczintetra.dev_native_reload_key_probe");
    /** 此审计绝不能切换为 CZ75 等 TaCZ 原生枪械。 */
    private static final boolean RUN_NATIVE_TACZ_PROBE = false;
    private static final boolean RUN_DUAL_HAND_PROBE = Boolean.getBoolean("taczintetra.dev_dual_hand_probe");
    private static final boolean RUN_DUAL_HAND_SETUP_ONLY = Boolean.getBoolean(
            "taczintetra.dev_dual_hand_setup_only");
    private static final boolean RUN_DUAL_HAND_PROBE_WAIT_FOR_GUEST = Boolean.getBoolean(
            "taczintetra.dev_dual_hand_probe_wait_for_guest");
    private static final int DUAL_HAND_GUEST_JOIN_MAX_WAIT_TICKS = 600;
    private static final boolean RUN_NM_PROBE = Boolean.getBoolean("taczintetra.dev_nm_probe");
    private static final boolean RUN_BODY_VISUAL_PROBE = Boolean.getBoolean("taczintetra.dev_body_visual_probe");
    private static final String BODY_VISUAL_FAMILY = System.getProperty("taczintetra.dev_body_visual_family", "pistol");
    private static int ticksUntilSetup;
    private static int shotsRemaining;
    private static int drawWaitTicks;
    private static int f06SuccessfulShots;
    private static int f06DryFireAttempts;
    private static int f06AssertionFailures;
    private static boolean f06AmmoSyncPending;
    private static int f06AmmoBeforeSync;
    private static int f06AmmoSyncWaitTicks;
    private static final int F06_AMMO_SYNC_MAX_WAIT_TICKS = 40;
    private static int ticksUntilShot;
    private static boolean active;
    private static boolean setupSent;
    private static boolean drawRequested;
    private static int stateLogTicks;
    private static boolean holoProbeLogged;
    private static int holoProbeDelay;
    private static boolean holoScreenshotSent;
    private static int holoScreenshotTicks;
    private static boolean specialSchematicProbeActive;
    private static boolean holoClickSent;
    private static int holoClickDelay;
    private static boolean holoBoundsLogged;
    private static IModularItem pendingHoloModular;
    private static ItemStack pendingHoloDisplayStack;
    private static UpgradeSchematic pendingHoloSchematic;
    private static int pendingHoloOpenTicks;
    private static boolean dualModeProbeSent;
    private static boolean dualHandSetupOnlyLogged;
    private static boolean dualModeStateLogged;
    private static boolean offhandShootProbeSent;
    private static boolean offhandReloadProbeSent;
    private static int offhandReloadDelay;
    private static int dualHandDrawWaitTicks;
    private static boolean dualHandDrawCooldownObservedPositive;
    private static int offhandReloadIdentityWaitTicks;
    private static boolean reloadProbeSent;
    private static int reloadInterruptTicks;
    private static int delayedReplayTicks;
    private static ReloadIntentMessage delayedReplayIntent;
    private static int ticksSinceDualSetup;
    private static int dualHandGuestJoinWaitTicks;
    private static boolean dualHandGuestJoinReady;
    private static boolean dualHandGuestJoinTimedOut;
    private static int dualHandSetupScreenshotTicks;
    private static boolean dualHandSetupScreenshotSent;
    private static int ordinaryProbeStage;
    private static int ordinaryProbeTicks;
    private static boolean nmProbeSent;
    private static int nmProbeStage;
    private static int nmProbeTicks;
    private static int bodyVisualProbeStage;
    private static int bodyVisualProbeTicks;
    private static boolean modularCrosshairProbeSent;
    private static boolean workbenchScreenshotSent;
    private static int workbenchScreenshotTicks;
    private static boolean workbenchClickSent;
    private static int workbenchClickDelay;
    private static int actionProbeStage;
    private static int actionProbeTicks;
    private static boolean lanPublished;
    private static int jeiScreenProbeStage;
    private static int jeiFallbackTicks;
    private static boolean sophisticatedBackpackUseSent;

    private DevClientAutomation() {
    }

    @SubscribeEvent
    public static void onLoggingIn(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        active = true;
        ticksUntilSetup = 30;
        shotsRemaining = 3;
        drawWaitTicks = 0;
        f06SuccessfulShots = 0;
        f06DryFireAttempts = 0;
        f06AssertionFailures = 0;
        f06AmmoSyncPending = false;
        f06AmmoBeforeSync = -1;
        f06AmmoSyncWaitTicks = 0;
        ticksUntilShot = 0;
        setupSent = false;
        drawRequested = false;
        stateLogTicks = 0;
        holoProbeLogged = false;
        holoProbeDelay = 40;
        holoScreenshotSent = false;
        holoScreenshotTicks = -1;
        specialSchematicProbeActive = false;
        holoClickSent = false;
        holoClickDelay = -1;
        holoBoundsLogged = false;
        pendingHoloModular = null;
        pendingHoloDisplayStack = ItemStack.EMPTY;
        pendingHoloSchematic = null;
        pendingHoloOpenTicks = -1;
        dualModeProbeSent = false;
        dualModeStateLogged = false;
        offhandShootProbeSent = false;
        offhandReloadProbeSent = false;
        offhandReloadDelay = 0;
        dualHandDrawWaitTicks = 0;
        dualHandDrawCooldownObservedPositive = false;
        offhandReloadIdentityWaitTicks = 0;
        reloadProbeSent = false;
        reloadInterruptTicks = -1;
        delayedReplayTicks = -1;
        delayedReplayIntent = null;
        ticksSinceDualSetup = 0;
        dualHandGuestJoinWaitTicks = 0;
        dualHandGuestJoinReady = false;
        dualHandGuestJoinTimedOut = false;
        dualHandSetupScreenshotTicks = -1;
        dualHandSetupScreenshotSent = false;
        ordinaryProbeStage = 0;
        ordinaryProbeTicks = 0;
        nmProbeSent = false;
        nmProbeStage = 0;
        nmProbeTicks = 0;
        bodyVisualProbeStage = 0;
        bodyVisualProbeTicks = 0;
        modularCrosshairProbeSent = false;
        workbenchScreenshotSent = false;
        workbenchScreenshotTicks = -1;
        workbenchClickSent = false;
        workbenchClickDelay = -1;
        actionProbeStage = 0;
        actionProbeTicks = 0;
        lanPublished = false;
        jeiScreenProbeStage = 0;
        jeiFallbackTicks = 0;
        sophisticatedBackpackUseSent = false;
        LOGGER.info("TaCZinTetra dev client automation armed");
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ENABLED) return;
        if (event.getScreen() instanceof HoloGui) {
            LOGGER.info("TaCZinTetra dev observed Tetra HoloGui opening");
            // 新客户端通过 quick-play 启动时首先打开全息球界面。
            // 初始化与工作台流程相同的开发装备探针，
            // 避免局域网访客没有装备。
            sendDualSetup(Minecraft.getInstance());
            return;
        }
        if (!(event.getScreen() instanceof WorkbenchScreen)) return;
        LOGGER.info("TaCZinTetra dev observed Tetra WorkbenchScreen opening");
        workbenchScreenshotTicks = 20;
        workbenchClickDelay = 2;
        sendDualSetup(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (!ENABLED || !(event.getScreen() instanceof WorkbenchScreen)) return;
        LOGGER.info("TaCZinTetra dev observed Tetra WorkbenchScreen closing: clickSent={}, clickDelay={}",
                workbenchClickSent, workbenchClickDelay);
    }

    private static void sendDualSetup(Minecraft minecraft) {
        if (setupSent || minecraft.player == null || minecraft.getConnection() == null) return;
        minecraft.getConnection().sendCommand("gamemode creative");
        minecraft.getConnection().sendCommand(
                "kill @e[type=!minecraft:player,tag=!taczintetra_dev_damage_target]");
        minecraft.getConnection().sendCommand("give @p tacz:ammo_box 1");
        minecraft.getConnection().sendCommand("give @p taczintetra:special_inlay 1");
        if (RUN_BODY_VISUAL_PROBE) {
            DevBodyVisualProbeProfile profile = DevBodyVisualProbeProfile.forFamily(BODY_VISUAL_FAMILY);
            minecraft.getConnection().sendCommand("tag @p add taczintetra_dev_body_visual_" + profile.family());
            LOGGER.info("TaCZinTetra dev body visual probe requested family={}", profile.family());
        }
        minecraft.getConnection().sendCommand("item replace entity @p weapon.mainhand with taczintetra:starter_pistol");
        if (!RUN_DUAL_HAND_PROBE) {
            minecraft.getConnection().sendCommand("item replace entity @p weapon.offhand with air");
        }
        minecraft.getConnection().sendCommand("data get entity @p HandItems");
        if (RUN_BODY_VISUAL_PROBE) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            LOGGER.info("TaCZinTetra dev body visual probe selected first-person view");
        } else if (Boolean.getBoolean("taczintetra.heat_overlay_screenshot_probe")
                || Boolean.getBoolean("taczintetra.dev_ads_screenshot_probe")) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            LOGGER.info("TaCZinTetra dev first-person HUD/ADS screenshot probe enabled");
        } else {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            LOGGER.info("TaCZinTetra dev third-person dual-hand visual probe enabled");
        }
        setupSent = true;
        ticksSinceDualSetup = 0;
        ticksUntilShot = 40;
        LOGGER.info("TaCZinTetra dev dual-hand automation equipped both hands");
    }

    private static void advanceBodyVisualProbe(Minecraft minecraft) {
        if (minecraft.screen != null) {
            minecraft.setScreen(null);
            return;
        }
        if (ticksUntilSetup > 0) {
            ticksUntilSetup--;
            return;
        }
        if (!setupSent) {
            sendDualSetup(minecraft);
            return;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        boolean assembled = stack.getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem
                && stack.hasTag() && stack.getTag().getBoolean("taczintetra_dev_modules_ready");
        if (!assembled) {
            if (++bodyVisualProbeTicks == 1 || bodyVisualProbeTicks % 20 == 0) {
                LOGGER.info("TaCZinTetra dev body visual probe waiting for authoritative module assembly: family={}, elapsedTicks={}/200, item={}, modulesReady={}",
                        BODY_VISUAL_FAMILY, bodyVisualProbeTicks, stack.getItem(),
                        stack.hasTag() && stack.getTag().getBoolean("taczintetra_dev_modules_ready"));
            }
            if (bodyVisualProbeTicks >= 200) {
                bodyVisualProbeStage = 3;
                LOGGER.error("BODY_VISUAL_PROBE_TIMEOUT family={} no authoritative assembled gun arrived",
                        BODY_VISUAL_FAMILY);
            }
            return;
        }
        if (bodyVisualProbeStage == 0) {
            ((IClientPlayerGunOperator) minecraft.player).draw(stack);
            bodyVisualProbeStage = 1;
            bodyVisualProbeTicks = 0;
            LOGGER.info("TaCZinTetra dev body visual probe drew assembled family={} modules={}",
                    BODY_VISUAL_FAMILY, stack.getTag());
            return;
        }
        if (bodyVisualProbeStage == 1 && ++bodyVisualProbeTicks >= 25) {
            String screenshotName = "taczintetra-body-visual-" + BODY_VISUAL_FAMILY;
            Screenshot.grab(minecraft.gameDirectory, screenshotName, minecraft.getMainRenderTarget(),
                    path -> LOGGER.info("BODY_VISUAL_PROBE_CAPTURED family={} screenshot={}",
                            BODY_VISUAL_FAMILY, path));
            bodyVisualProbeStage = 2;
        }
    }

    private static boolean tryPassDualHandGuestGate(Minecraft minecraft) {
        if (!LAN_HOST || !RUN_DUAL_HAND_PROBE || !RUN_DUAL_HAND_PROBE_WAIT_FOR_GUEST
                || dualHandGuestJoinReady) return !dualHandGuestJoinTimedOut;
        if (dualHandGuestJoinTimedOut || minecraft.getConnection() == null) return false;
        int onlinePlayers = minecraft.getConnection().getOnlinePlayers().size();
        DevF06GuestJoinGatePolicy.Outcome outcome = DevF06GuestJoinGatePolicy.evaluate(
                onlinePlayers, ++dualHandGuestJoinWaitTicks, DUAL_HAND_GUEST_JOIN_MAX_WAIT_TICKS);
        if (outcome == DevF06GuestJoinGatePolicy.Outcome.READY) {
            dualHandGuestJoinReady = true;
            LOGGER.info("TaCZinTetra dev dual-hand probe guest gate passed: onlinePlayers={}, elapsedTicks={}",
                    onlinePlayers, dualHandGuestJoinWaitTicks);
            return true;
        }
        if (outcome == DevF06GuestJoinGatePolicy.Outcome.TIMEOUT) {
            dualHandGuestJoinTimedOut = true;
            LOGGER.error("F06_DUAL_HAND_GUEST_JOIN_TIMEOUT probe setup was not sent: onlinePlayers={}, elapsedTicks={}",
                    onlinePlayers, dualHandGuestJoinWaitTicks);
            active = false;
            return false;
        }
        if (dualHandGuestJoinWaitTicks == 1 || dualHandGuestJoinWaitTicks % 100 == 0) {
            LOGGER.info("TaCZinTetra dev dual-hand probe waiting for LAN guest: onlinePlayers={}, elapsedTicks={}/{}",
                    onlinePlayers, dualHandGuestJoinWaitTicks, DUAL_HAND_GUEST_JOIN_MAX_WAIT_TICKS);
        }
        return false;
    }

    private static void probeHolosphereCatalog() {
        if (holoProbeLogged || HolosphereEntryStore.instance == null) return;
        var entries = HolosphereEntryStore.instance.getEntries();
        var entry = entries.get("taczintetra");
        LOGGER.info("TaCZinTetra dev holosphere catalog: entries={}, customEntry={}, item={}, defaultStack={}",
                entries.size(), entry != null, entry == null ? "missing" : entry.item,
                entry == null ? "missing" : entry.getDefaultStack().getItem());
        holoProbeLogged = true;
        ModularHolosphereItem.showGui();
        if (entry != null && entry.item instanceof IModularItem modular) {
            ItemStack displayStack = entry.getDefaultStack().copy();
            IModularItem.putModuleInSlot(displayStack, "taczintetra/body",
                    "taczintetra/body/pistol", "taczintetra/wood/");
            IModularItem.putModuleInSlot(displayStack, "taczintetra/barrel",
                    "taczintetra/barrel/9mm", "taczintetra/wood/");
            IModularItem.putModuleInSlot(displayStack, "taczintetra/magazine",
                    "taczintetra/magazine/standard", "taczintetra/wood/");
            var schematic = SchematicRegistry.getSchematic("taczintetra/special_socket");
            if (schematic != null) {
                // showGui() 会安装界面并启动打开动画。同一 tick 中立即打开蓝图会与
                // 界面生命周期竞争，导致
                // 截图探针捕获过渡帧或空白帧。
                // 等 HoloGui 成为当前界面后再延迟两个客户端 tick。
                pendingHoloModular = modular;
                pendingHoloDisplayStack = displayStack;
                pendingHoloSchematic = schematic;
                pendingHoloOpenTicks = 2;
                LOGGER.info("TaCZinTetra dev holosphere scheduled special schematic: itemKey=taczintetra, slot=taczintetra/special, schematic={}, displayStack={}",
                        schematic.getKey(), displayStack.getTag());
            } else {
                LOGGER.warn("TaCZinTetra dev holosphere custom schematic missing: taczintetra/body");
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;

        // 手动输入测试夹具必须先于 Holo/Workbench/JEI 探针运行，
        // 否则探针会主动打开 GUI，阻止真实按键到达客户端输入事件。
        if (RUN_DUAL_HAND_SETUP_ONLY) {
            if (ticksUntilSetup > 0) {
                ticksUntilSetup--;
                return;
            }
            if (!setupSent) {
                sendDualSetup(minecraft);
                return;
            }
            boolean mainGunReady = minecraft.player.getMainHandItem().getItem()
                    instanceof com.pycoder.taczintetra.item.ModularGunItem;
            boolean offhandGunReady = minecraft.player.getOffhandItem().getItem()
                    instanceof com.pycoder.taczintetra.item.ModularGunItem;
            if (mainGunReady && offhandGunReady) {
                if (!dualHandSetupOnlyLogged) {
                    dualHandSetupOnlyLogged = true;
                    dualHandSetupScreenshotTicks = 20;
                    LOGGER.info("DUAL_HAND_SETUP_ONLY_READY mainIdentity={} offhandIdentity={}",
                            StackIdentity.of(minecraft.player.getMainHandItem()),
                            StackIdentity.of(minecraft.player.getOffhandItem()));
                } else if (!dualHandSetupScreenshotSent && dualHandSetupScreenshotTicks >= 0
                        && minecraft.screen == null) {
                    if (dualHandSetupScreenshotTicks > 0) {
                        dualHandSetupScreenshotTicks--;
                    } else {
                        Screenshot.grab(minecraft.gameDirectory,
                                "taczintetra-dual-hand-first-person-offhand-probe",
                                minecraft.getMainRenderTarget(), ignored -> { });
                        dualHandSetupScreenshotSent = true;
                        LOGGER.info("TaCZinTetra dev dual-hand first-person off-hand screenshot captured");
                    }
                }
            }
            return;
        }

        if (NATIVE_RELOAD_KEY_PROBE) {
            if (!setupSent) sendDualSetup(minecraft);
            return;
        }

        if (RUN_BODY_VISUAL_PROBE) {
            advanceBodyVisualProbe(minecraft);
            return;
        }

        if (HOLD_HOLO && specialSchematicProbeActive) {
            if (!holoScreenshotSent && holoScreenshotTicks >= 0 && minecraft.screen != null) {
                if (holoScreenshotTicks > 0) {
                    holoScreenshotTicks--;
                } else {
                    Screenshot.grab(minecraft.gameDirectory, "taczintetra-holosphere-special-slot-probe",
                            minecraft.getMainRenderTarget(), ignored -> { });
                    holoScreenshotSent = true;
                    LOGGER.info("TaCZinTetra dev holosphere special-slot screenshot captured from client-tick fallback: screen={}",
                            minecraft.screen.getClass().getName());
                }
            }
            return;
        }

        if (!HOLD_HOLO && !sophisticatedBackpackUseSent
                && minecraft.screen == null
                && minecraft.player.getMainHandItem().is(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", "backpack")))) {
            minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
            sophisticatedBackpackUseSent = true;
            LOGGER.info("TaCZinTetra dev Sophisticated Backpacks client probe sent real use-item packet");
        }

        if (!holoProbeLogged && minecraft.level != null && holoProbeDelay-- <= 0) {
            probeHolosphereCatalog();
        }

        if (pendingHoloOpenTicks >= 0 && pendingHoloModular != null
                && minecraft.screen instanceof HoloGui) {
            if (pendingHoloOpenTicks-- == 0) {
                HoloGui.getInstance().openSchematic(pendingHoloModular, pendingHoloDisplayStack,
                        "taczintetra/special", pendingHoloSchematic, () -> { });
                specialSchematicProbeActive = true;
                holoScreenshotSent = false;
                holoScreenshotTicks = 10;
                holoClickDelay = 40;
                LOGGER.info("TaCZinTetra dev holosphere opened special schematic after active-screen wait: itemKey=taczintetra, slot=taczintetra/special, schematic={}, displayStack={}",
                        pendingHoloSchematic.getKey(), pendingHoloDisplayStack.getTag());
                pendingHoloModular = null;
                pendingHoloDisplayStack = ItemStack.EMPTY;
                pendingHoloSchematic = null;
                pendingHoloOpenTicks = -1;
            }
        }

        // 专用 GUI 审计模式会在真实 HoloGui 激活后停止其余冒烟测试状态机，
        // 便于人工或按 PID 定向截取特殊蓝图，避免界面自动切换到工作台或 JEI。
        if (!workbenchScreenshotSent && workbenchScreenshotTicks >= 0
                && minecraft.screen instanceof WorkbenchScreen) {
            if (workbenchScreenshotTicks-- == 0) {
                Screenshot.grab(minecraft.gameDirectory, "taczintetra-workbench-screen-probe",
                        minecraft.getMainRenderTarget(), ignored -> { });
                workbenchScreenshotSent = true;
                LOGGER.info("TaCZinTetra dev workbench screenshot saved");
            }
        }

        if (minecraft.screen instanceof WorkbenchScreen workbenchScreen
                && !workbenchClickSent && workbenchClickDelay >= 0) {
            if (workbenchClickDelay > 0) {
                workbenchClickDelay--;
            } else {
                boolean handled = clickWorkbenchCraftButton(workbenchScreen);
                workbenchClickSent = true;
                LOGGER.info("TaCZinTetra dev workbench CraftButton tick click probe: handled={}", handled);
            }
        }

        if (!workbenchClickSent && workbenchClickDelay >= 0
                && minecraft.screen instanceof WorkbenchScreen) {
            // 必须在 active/JEI 状态机提前返回之前运行此逻辑。
            return;
        }

        if (active && jeiScreenProbeStage == 0 && minecraft.level != null
                && ++jeiFallbackTicks >= 500) {
            active = false;
            jeiScreenProbeStage = 1;
            LOGGER.info("TaCZinTetra dev JEI screen probe fallback armed after world warmup");
        }

        if (jeiScreenProbeStage > 0) {
            if (jeiScreenProbeStage == 1) {
                minecraft.setScreen(new InventoryScreen(minecraft.player));
                jeiScreenProbeStage = 2;
                LOGGER.info("TaCZinTetra dev JEI screen probe opened inventory");
            } else if (jeiScreenProbeStage == 2 && minecraft.screen != null) {
                Screenshot.grab(minecraft.gameDirectory, "taczintetra-jei-screen-probe",
                        minecraft.getMainRenderTarget(), ignored -> { });
                jeiScreenProbeStage = 3;
                LOGGER.info("TaCZinTetra dev JEI screen probe screenshot saved");
            }
            return;
        }

        // 登录后 Tetra 会重建客户端存储。确认自定义条目出现后再执行探测，
        // 避免过早处理登录事件，把尚未加载的目录误记为缺失。
        if (!holoProbeLogged) probeHolosphereCatalog();
        if (!active) return;

        if (delayedReplayTicks >= 0) {
            if (delayedReplayTicks-- == 0 && delayedReplayIntent != null) {
                ModNetwork.CHANNEL.sendToServer(delayedReplayIntent);
                LOGGER.info("TaCZinTetra dev delayed replay probe sent duplicate reload sequence={}",
                        delayedReplayIntent.sequence());
                delayedReplayIntent = null;
                delayedReplayTicks = -1;
            }
        }

        if (ticksUntilSetup > 0) {
            ticksUntilSetup--;
            return;
        }
        if (!setupSent) {
            sendDualSetup(minecraft);
            return;
        }
        if (LAN_HOST && !lanPublished) {
            if (minecraft.getSingleplayerServer() != null) {
                // 隔离的局域网探针使用合成用户名。此测试流程
                // 不依赖 Mojang 会话认证；
                // 正式服务器仍遵循已配置的认证策略。
                minecraft.getSingleplayerServer().setUsesAuthentication(false);
                LOGGER.info("TaCZinTetra dev LAN host disabled online authentication for isolated probe");
            }
            minecraft.getConnection().sendCommand("publish true survival 25565");
            lanPublished = true;
            LOGGER.info("TaCZinTetra dev LAN host published integrated server on port 25565");
        }
        if (!tryPassDualHandGuestGate(minecraft)) return;
        if (F06_RELOAD_INTERRUPT_PROBE) return;
        if (!RUN_DUAL_HAND_PROBE) {
            dualModeProbeSent = true;
            offhandShootProbeSent = true;
            offhandReloadProbeSent = true;
        }
        if (RUN_DUAL_HAND_PROBE && !dualModeProbeSent) {
            ticksSinceDualSetup++;
            if (!(minecraft.player.getMainHandItem().getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem)
                    || !(minecraft.player.getOffhandItem().getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem)) {
                if (ticksSinceDualSetup % 20 == 0) {
                    LOGGER.info("TaCZinTetra dev dual-hand automation waiting for client inventory sync: main={}, off={}",
                            minecraft.player.getMainHandItem().getItem(), minecraft.player.getOffhandItem().getItem());
                }
                return;
            }
            IClientPlayerGunOperator dualGunOperator = IClientPlayerGunOperator.fromLocalPlayer(minecraft.player);
            if (!drawRequested) {
                dualGunOperator.draw(minecraft.player.getMainHandItem());
                drawRequested = true;
                LOGGER.info("TaCZinTetra dev dual-hand automation requested TaCZ draw before off-hand probe");
                return;
            }
            IGunOperator dualGunServerOperator = IGunOperator.fromLivingEntity(minecraft.player);
            long drawCooldown = dualGunServerOperator.getSynDrawCoolDown();
            int elapsedDrawWaitTicks = ++dualHandDrawWaitTicks;
            DevF06DrawCooldownPolicy.Result drawResult = DevF06DrawCooldownPolicy.evaluate(
                    dualHandDrawCooldownObservedPositive, drawCooldown, elapsedDrawWaitTicks, 200);
            dualHandDrawCooldownObservedPositive = drawResult.observedPositiveCooldown();
            if (drawResult.outcome() == DevF06DrawCooldownPolicy.Outcome.WAITING_FOR_SERVER_DRAW
                    || drawResult.outcome() == DevF06DrawCooldownPolicy.Outcome.WAITING_FOR_COOLDOWN) {
                if (elapsedDrawWaitTicks == 1 || elapsedDrawWaitTicks % 20 == 0) {
                    LOGGER.info("TaCZinTetra dev dual-hand automation waiting for TaCZ server draw cooldown: elapsedTicks={}/200, cooldownMs={}",
                            elapsedDrawWaitTicks, drawCooldown);
                }
                return;
            }
            if (drawResult.outcome() == DevF06DrawCooldownPolicy.Outcome.TIMEOUT) {
                LOGGER.error("F06_DUAL_HAND_DRAW_COOLDOWN_TIMEOUT off-hand probe was not sent: acknowledgedCooldown={}, cooldownMs={}, elapsedTicks={}",
                        dualHandDrawCooldownObservedPositive, drawCooldown, dualHandDrawWaitTicks);
                dualModeProbeSent = true;
                offhandShootProbeSent = true;
                offhandReloadProbeSent = true;
                return;
            }
            if (drawResult.outcome() != DevF06DrawCooldownPolicy.Outcome.READY) return;
            int identity = StackIdentity.of(minecraft.player.getOffhandItem());
            long timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(),
                    dualGunOperator.getDataHolder().clientBaseTimestamp);
            LOGGER.info("TaCZinTetra dev dual-hand state before intent: mainItem={}, offItem={}, offIdentity={}, drawReady={}",
                    minecraft.player.getMainHandItem().getItem(), minecraft.player.getOffhandItem().getItem(), identity,
                    drawCooldown == 0L);
            ModNetwork.CHANNEL.sendToServer(new FireModeIntentMessage(true,
                    identity, 1001));
            dualModeProbeSent = true;
            LOGGER.info("TaCZinTetra dev dual-hand automation sent off-hand fire-mode intent");
            ModNetwork.CHANNEL.sendToServer(new OffhandShootIntentMessage(
                    identity, 1002, timestamp));
            offhandShootProbeSent = true;
            LOGGER.info("TaCZinTetra dev dual-hand automation sent off-hand shoot intent: identity={}", identity);
            offhandReloadDelay = 10;
            return;
        }
        if (RUN_DUAL_HAND_PROBE && offhandShootProbeSent && !offhandReloadProbeSent) {
            if (offhandReloadDelay > 0) {
                offhandReloadDelay--;
                return;
            } else {
                ItemStack offhand = minecraft.player.getOffhandItem();
                if (offhand.getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem) {
                    if (!StackIdentity.hasUniqueId(offhand)) {
                        if (++offhandReloadIdentityWaitTicks == 1 || offhandReloadIdentityWaitTicks % 20 == 0) {
                            LOGGER.info("TaCZinTetra dev dual-hand automation waiting for authoritative off-hand identity: elapsedTicks={}/100",
                                    offhandReloadIdentityWaitTicks);
                        }
                        if (offhandReloadIdentityWaitTicks >= 100) {
                            LOGGER.error("F06_OFFHAND_IDENTITY_SYNC_TIMEOUT reload intent was not sent");
                            offhandReloadProbeSent = true;
                        }
                        return;
                    }
                    int identity = StackIdentity.of(offhand);
                    ReloadIntentMessage intent = new ReloadIntentMessage(true, false, identity,
                            com.pycoder.taczintetra.runtime.ReloadSequence.next());
                    ModNetwork.CHANNEL.sendToServer(intent);
                    // 在真实连接上等待数个客户端 tick 后重放相同序列；
                    // 即使重复包延迟到达，服务端门禁
                    // 也只能接受第一次请求。
                    delayedReplayIntent = intent;
                    delayedReplayTicks = 5;
                    offhandReloadProbeSent = true;
                    LOGGER.info("TaCZinTetra dev dual-hand automation sent off-hand fill reload intent after shoot settlement: identity={}", identity);
                    return;
                }
            }
        }
        if (RUN_DUAL_HAND_PROBE && !offhandShootProbeSent && ticksSinceDualSetup >= 30) {
            ItemStack offhand = minecraft.player.getOffhandItem();
            if (offhand.getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem) {
                int identity = StackIdentity.of(offhand);
                  long timestamp = NetworkTimestampPolicy.clientTimestamp(System.currentTimeMillis(),
                          IClientPlayerGunOperator.fromLocalPlayer(minecraft.player)
                                  .getDataHolder().clientBaseTimestamp);
                ModNetwork.CHANNEL.sendToServer(new OffhandShootIntentMessage(
                        identity, 1002, timestamp));
                offhandShootProbeSent = true;
                LOGGER.info("TaCZinTetra dev dual-hand automation sent off-hand shoot intent: identity={}", identity);
            }
        }
        if (RUN_DUAL_HAND_PROBE) return;
        if (F06_ZERO_AMMO_PROBE && f06AmmoSyncPending) {
            int observedAmmo = modularAmmo(minecraft.player.getMainHandItem());
            DevF06AmmoSyncObservationPolicy.Outcome outcome = DevF06AmmoSyncObservationPolicy.evaluate(
                    f06AmmoBeforeSync, observedAmmo, f06AmmoSyncWaitTicks, F06_AMMO_SYNC_MAX_WAIT_TICKS);
            if (outcome == DevF06AmmoSyncObservationPolicy.Outcome.WAITING) {
                f06AmmoSyncWaitTicks++;
                if (f06AmmoSyncWaitTicks == 1 || f06AmmoSyncWaitTicks % 20 == 0) {
                    LOGGER.info("F06_ZERO_AMMO_SYNC_WAIT ammo={}->{} elapsedTicks={}/{}",
                            f06AmmoBeforeSync, observedAmmo, f06AmmoSyncWaitTicks,
                            F06_AMMO_SYNC_MAX_WAIT_TICKS);
                }
                return;
            }

            f06AmmoSyncPending = false;
            shotsRemaining--;
            ticksUntilShot = 12;
            if (outcome == DevF06AmmoSyncObservationPolicy.Outcome.CONFIRMED) {
                f06SuccessfulShots++;
            } else {
                f06AssertionFailures++;
            }
            LOGGER.info("F06_ZERO_AMMO_ASSERTION {} attempt={} result=SUCCESS ammo={}->{} syncTicks={} outcome={}",
                    outcome == DevF06AmmoSyncObservationPolicy.Outcome.CONFIRMED ? "PASS" : "FAIL",
                    3 - shotsRemaining, f06AmmoBeforeSync, observedAmmo,
                    f06AmmoSyncWaitTicks, outcome);
            if (shotsRemaining == 0) {
                boolean passed = f06SuccessfulShots == 1
                        && f06DryFireAttempts == 2
                        && f06AssertionFailures == 0
                        && observedAmmo == 0;
                LOGGER.info("F06_ZERO_AMMO_SUMMARY {} successfulShots={} dryFireAttempts={} failures={} finalAmmo={}",
                        passed ? "PASS" : "FAIL", f06SuccessfulShots, f06DryFireAttempts,
                        f06AssertionFailures, observedAmmo);
            }
            return;
        }
        if (ticksUntilShot > 0) {
            ticksUntilShot--;
            return;
        }
        if (reloadInterruptTicks >= 0) {
            if (reloadInterruptTicks-- == 0) {
                minecraft.getConnection().sendCommand("item replace entity @p weapon.mainhand with air");
                LOGGER.info("TaCZinTetra dev interruption probe replaced main-hand stack during reload");
                reloadInterruptTicks = -1;
            }
        }
        if (ordinaryProbeStage > 0 && minecraft.screen != null) {
            // 前面的全息球或工作台探针可能仍留有客户端界面。
            // 普通 TaCZ 探针仅供开发使用，需要
            // 处于与玩家正常操作相同的游戏输入状态。
            minecraft.setScreen(null);
        }
        if (ordinaryProbeStage == 0 && dualModeProbeSent && minecraft.screen != null) {
            // 双手探针同样会执行游戏内操作。测试原生射击前，
            // 先关闭此前 Tetra 探针留下的界面。
            minecraft.setScreen(null);
        }
        if (minecraft.screen != null) return;
        if (!RUN_NATIVE_TACZ_PROBE
                && !(minecraft.player.getMainHandItem().getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem)) {
            if (++stateLogTicks >= 20) {
                stateLogTicks = 0;
                LOGGER.info("TaCZinTetra dev modular-only automation waiting for modular main hand: item={}",
                        minecraft.player.getMainHandItem().getItem());
            }
            return;
        }
        IClientPlayerGunOperator gunOperator = (IClientPlayerGunOperator) minecraft.player;

        if (RUN_NM_PROBE && !nmProbeSent && reloadProbeSent) {
            nmProbeTicks++;
            if (nmProbeTicks >= 40) {
                minecraft.getConnection().sendCommand("tag @p add taczintetra_dev_shotgun");
                minecraft.getConnection().sendCommand(
                        "item replace entity @p weapon.mainhand with taczintetra:starter_pistol");
                nmProbeSent = true;
                nmProbeStage = 1;
                nmProbeTicks = 0;
                LOGGER.info("TaCZinTetra dev N/M probe requested shotgun body + 12g barrel");
                return;
            }
        }

        if (nmProbeStage > 0) {
            nmProbeTicks++;
            ItemStack nmGun = minecraft.player.getMainHandItem();
            if (nmProbeStage == 1 && nmGun.getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem) {
                gunOperator.draw(nmGun);
                minecraft.options.setCameraType(CameraType.FIRST_PERSON);
                nmProbeStage = 2;
                nmProbeTicks = 0;
                LOGGER.info("TaCZinTetra dev N/M probe drew shotgun modular gun");
                return;
            }
            if (nmProbeStage == 2 && nmProbeTicks >= 20) {
                if (!modularCrosshairProbeSent && nmProbeTicks >= 5) {
                    Screenshot.grab(minecraft.gameDirectory, "taczintetra-modular-crosshair-probe",
                            minecraft.getMainRenderTarget(), ignored -> { });
                    modularCrosshairProbeSent = true;
                    LOGGER.info("TaCZinTetra dev modular crosshair screenshot saved");
                }
                if (Boolean.getBoolean("taczintetra.dev_ads_screenshot_probe")) {
                    Screenshot.grab(minecraft.gameDirectory, "taczintetra-ads-hipfire-baseline",
                            minecraft.getMainRenderTarget(), ignored -> { });
                    nmProbeStage = 0;
                    actionProbeStage = 1;
                    actionProbeTicks = 0;
                    LOGGER.info("TaCZinTetra dev ADS un-aimed first-person baseline captured; native aim probe armed without firing");
                    return;
                }
                int ammoBefore = modularAmmo(minecraft.player.getMainHandItem());
                ShootResult nmResult = gunOperator.shoot();
                LOGGER.info("TaCZinTetra dev N/M probe shoot result={}, ammo={}->{}, tag={}",
                        nmResult, ammoBefore, modularAmmo(minecraft.player.getMainHandItem()),
                        minecraft.player.getMainHandItem().getTag());
                if (nmResult != ShootResult.IS_DRAWING) {
                    nmProbeStage = 0;
                    actionProbeStage = 1;
                    actionProbeTicks = 0;
                    LOGGER.info("TaCZinTetra dev action probe armed after modular shotgun shot");
                } else {
                    nmProbeTicks = 0;
                }
                return;
            }
            return;
        }

        if (actionProbeStage > 0) {
            actionProbeTicks++;
            ItemStack actionGun = minecraft.player.getMainHandItem();
            if (!(actionGun.getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem)) {
                LOGGER.warn("TaCZinTetra dev action probe aborted: modular gun left main hand");
                actionProbeStage = 0;
                ordinaryProbeStage = RUN_NATIVE_TACZ_PROBE ? 1 : 0;
                ordinaryProbeTicks = 0;
                return;
            }
            if (actionProbeStage == 1) {
                if (Boolean.getBoolean("taczintetra.dev_ads_screenshot_probe")) {
                    // TaCZ HOLD_TO_AIM synchronizes the native mapping every client tick.
                    // Keep the mapping down so its own input handler does not cancel this API probe.
                    com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true);
                }
                gunOperator.aim(true);
                float aimingZoom = ((com.tacz.guns.api.item.IGun) actionGun.getItem()).getAimingZoom(actionGun);
                LOGGER.info("TaCZinTetra dev action probe aim=true, state={}, aimingZoom={}",
                        gunOperator.isAim(), aimingZoom);
                actionProbeStage = 2;
                actionProbeTicks = 0;
                return;
            }
            if (actionProbeStage == 2) {
                // TaCZ synchronizes aim with its native input state on client ticks.
                // Keep the API-driven probe active through the animation window.
                if (Boolean.getBoolean("taczintetra.dev_ads_screenshot_probe")) {
                    com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true);
                }
                gunOperator.aim(true);
                if (actionProbeTicks >= 10) {
                    if (Boolean.getBoolean("taczintetra.dev_ads_screenshot_probe") && gunOperator.isAim()) {
                        boolean taczInputGate = com.tacz.guns.util.InputExtraCheck.isInGame();
                        boolean mouseGrabbed = minecraft.mouseHandler.isMouseGrabbed();
                        boolean windowActive = minecraft.isWindowActive();
                        String screenName = minecraft.screen == null
                                ? "none" : minecraft.screen.getClass().getName();
                        LOGGER.info("TaCZinTetra dev ADS input state: inputGate={}, mouseGrabbed={}, windowActive={}, screen={}",
                                taczInputGate, mouseGrabbed, windowActive, screenName);
                        var nativeGun = (com.tacz.guns.api.item.IGun) actionGun.getItem();
                        ResourceLocation gunId = nativeGun.getGunId(actionGun);
                        var commonGunIndex = com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gunId);
                        String aimTime = commonGunIndex
                                .map(index -> Float.toString(index.getGunData().getAimTime()))
                                .orElse("missing");
                        var aimData = gunOperator.getDataHolder();
                        LOGGER.info("TaCZinTetra dev ADS native data: gunId={}, clientIndex={}, commonIndex={}, aimTime={}, clientAimingProgress={}, oldAimingProgress={}, drawTimestamp={}",
                                gunId,
                                com.tacz.guns.api.TimelessAPI.getClientGunIndex(gunId).isPresent(),
                                commonGunIndex.isPresent(), aimTime,
                                aimData.clientAimingProgress,
                                com.tacz.guns.client.gameplay.LocalPlayerDataHolder.oldAimingProgress,
                                aimData.clientDrawTimestamp);
                        Screenshot.grab(minecraft.gameDirectory, "taczintetra-ads-aim-probe",
                                minecraft.getMainRenderTarget(), ignored -> { });
                        LOGGER.info("TaCZinTetra dev ADS screenshot captured after native aim transition: state={}, zoom={}, progress={}, ticks={}",
                                gunOperator.isAim(),
                                ((com.tacz.guns.api.item.IGun) actionGun.getItem()).getAimingZoom(actionGun),
                                gunOperator.getClientAimingProgress(1.0f), actionProbeTicks);
                    }
                    if (Boolean.getBoolean("taczintetra.dev_ads_screenshot_probe")) {
                        com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false);
                    }
                    gunOperator.aim(false);
                    LOGGER.info("TaCZinTetra dev action probe aim=false, state={}", gunOperator.isAim());
                    actionProbeStage = 3;
                    actionProbeTicks = 0;
                }
                return;
            }
            if (actionProbeStage == 3 && actionProbeTicks >= 10) {
                gunOperator.bolt();
                LOGGER.info("TaCZinTetra dev action probe bolt requested");
                actionProbeStage = 4;
                actionProbeTicks = 0;
                return;
            }
            if (actionProbeStage == 4 && actionProbeTicks >= 10) {
                gunOperator.fireSelect();
                LOGGER.info("TaCZinTetra dev action probe fire-select requested");
                actionProbeStage = 5;
                actionProbeTicks = 0;
                return;
            }
            if (actionProbeStage == 5 && actionProbeTicks >= 10) {
                gunOperator.melee();
                LOGGER.info("TaCZinTetra dev action probe melee requested");
                actionProbeStage = 0;
                ordinaryProbeStage = RUN_NATIVE_TACZ_PROBE ? 1 : 0;
                ordinaryProbeTicks = 0;
                return;
            }
            return;
        }

        if (RUN_NATIVE_TACZ_PROBE && ordinaryProbeStage > 0) {
            ordinaryProbeTicks++;
            if (ordinaryProbeStage == 1 && ordinaryProbeTicks >= 80) {
                minecraft.getConnection().sendCommand(
                        "item replace entity @p weapon.mainhand with tacz:modern_kinetic_gun{GunId:\"tacz:cz75\",GunCurrentAmmoCount:12,DummyAmmo:12,MaxDummyAmmo:12}");
                ordinaryProbeStage = 2;
                ordinaryProbeTicks = 0;
                LOGGER.info("TaCZinTetra dev ordinary TaCZ probe requested tacz:modern_kinetic_gun GunId=tacz:cz75");
                return;
            }
            if (ordinaryProbeStage == 2) {
                ItemStack ordinary = minecraft.player.getMainHandItem();
                if (ordinary.getItem() instanceof com.tacz.guns.api.item.nbt.GunItemDataAccessor gun
                        && gun.getGunId(ordinary).toString().equals("tacz:cz75")) {
                    gunOperator.draw(ordinary);
                    ordinaryProbeStage = 3;
                    ordinaryProbeTicks = 0;
                    LOGGER.info("TaCZinTetra dev ordinary TaCZ probe drew native gun: item={}, gunId={}",
                            ordinary.getItem(), gun.getGunId(ordinary));
                }
                return;
            }
            if (ordinaryProbeStage == 3 && ordinaryProbeTicks >= 20) {
                ShootResult ordinaryResult = gunOperator.shoot();
                LOGGER.info("TaCZinTetra dev ordinary TaCZ probe shoot result={}, item={}",
                        ordinaryResult, minecraft.player.getMainHandItem().getItem());
                if (ordinaryResult != ShootResult.IS_DRAWING) {
                    ordinaryProbeStage = 4;
                    active = false;
                    jeiScreenProbeStage = 1;
                } else {
                    ordinaryProbeTicks = 0;
                }
                return;
            }
            return;
        }

        // 请求的连射次数归零后，不要再发送客户端射击数据包；下方的换弹请求是异步的。
        if (shotsRemaining <= 0) return;

        if (++stateLogTicks >= 20) {
            stateLogTicks = 0;
            IGunOperator serverState = IGunOperator.fromLivingEntity(minecraft.player);
            LOGGER.info("TaCZinTetra dev draw state: serverCooldown={}, clientDrawTimestamp={}, clientReady={}, cachePresent={}",
                    serverState.getSynDrawCoolDown(), gunOperator.getDataHolder().clientDrawTimestamp,
                    gunOperator.isReadyToDraw(),
                    IGunOperator.fromLivingEntity(minecraft.player).getCacheProperty() != null);
        }
        if (!drawRequested) {
            gunOperator.draw(minecraft.player.getMainHandItem());
            drawRequested = true;
            drawWaitTicks = 0;
            ticksUntilShot = 20;
            LOGGER.info("TaCZinTetra dev client automation requested native draw");
            return;
        }
        int ammoBefore = modularAmmo(minecraft.player.getMainHandItem());
        ShootResult result = gunOperator.shoot();
        if (result == ShootResult.IS_DRAWING) {
            drawWaitTicks++;
            if (DevDrawWaitPolicy.shouldLog(drawWaitTicks)) {
                logNativeDrawProbeState(minecraft, minecraft.player.getMainHandItem(), drawWaitTicks);
            }
            if (DevDrawWaitPolicy.timedOut(drawWaitTicks)) {
                LOGGER.error("DEV_DRAW_PROBE_TIMEOUT waitTicks={} maxWaitTicks={}; automation stopped",
                        drawWaitTicks, DevDrawWaitPolicy.MAX_WAIT_TICKS);
                active = false;
                return;
            }
            // 继续等待 TaCZ 原生拔枪冷却，但保持开发探针的等待有界。
            ticksUntilShot = 2;
            return;
        }
        drawWaitTicks = 0;
        ticksUntilShot = 12;
        int ammoAfter = modularAmmo(minecraft.player.getMainHandItem());
        if (F06_ZERO_AMMO_PROBE) {
            if (ammoBefore > 0 && result == ShootResult.SUCCESS) {
                f06AmmoSyncPending = true;
                f06AmmoBeforeSync = ammoBefore;
                f06AmmoSyncWaitTicks = 0;
                LOGGER.info("F06_ZERO_AMMO_SYNC_PENDING attempt={} result=SUCCESS ammoBefore={} maxWaitTicks={}",
                        4 - shotsRemaining, ammoBefore, F06_AMMO_SYNC_MAX_WAIT_TICKS);
                return;
            }
            shotsRemaining--;
            boolean shotAccepted = ammoBefore > 0
                    && result == ShootResult.SUCCESS
                    && ammoAfter == ammoBefore - 1;
            boolean dryFireAccepted = ammoBefore == 0
                    && result == ShootResult.NO_AMMO
                    && ammoAfter == 0;
            if (shotAccepted) {
                f06SuccessfulShots++;
            } else if (dryFireAccepted) {
                f06DryFireAttempts++;
            } else {
                f06AssertionFailures++;
            }
            LOGGER.info("F06_ZERO_AMMO_ASSERTION {} attempt={} result={} ammo={}->{}",
                    shotAccepted || dryFireAccepted ? "PASS" : "FAIL",
                    3 - shotsRemaining, result, ammoBefore, ammoAfter);
        } else {
            shotsRemaining--;
            LOGGER.info("TaCZinTetra dev client automation shoot result={}, ammo={}->{}, remaining={}, tag={}",
                    result, ammoBefore, ammoAfter, shotsRemaining,
                minecraft.player.getMainHandItem().getTag());
        }
        if (shotsRemaining == 0) {
            if (F06_ZERO_AMMO_PROBE) {
                boolean passed = f06SuccessfulShots == 1
                        && f06DryFireAttempts == 2
                        && f06AssertionFailures == 0
                        && ammoAfter == 0;
                LOGGER.info("F06_ZERO_AMMO_SUMMARY {} successfulShots={} dryFireAttempts={} failures={} finalAmmo={}",
                        passed ? "PASS" : "FAIL", f06SuccessfulShots, f06DryFireAttempts,
                        f06AssertionFailures, ammoAfter);
                return;
            }
            if (!reloadProbeSent) {
                ModNetwork.CHANNEL.sendToServer(new ReloadIntentMessage(false, false,
                        StackIdentity.of(minecraft.player.getMainHandItem()),
                        com.pycoder.taczintetra.runtime.ReloadSequence.next()));
                reloadProbeSent = true;
                ticksUntilShot = 0;
                reloadInterruptTicks = -1;
                LOGGER.info("TaCZinTetra dev modular-only automation sent normal main-hand reload intent");
            }
            return;
        }
    }

    private static int modularAmmo(ItemStack stack) {
        return stack.getItem() instanceof com.pycoder.taczintetra.item.ModularGunItem gun
                ? gun.getCurrentAmmoCount(stack) : -1;
    }

    private static void logNativeDrawProbeState(Minecraft minecraft, ItemStack stack, int waitTicks) {
        var clientOperator = IClientPlayerGunOperator.fromLocalPlayer(minecraft.player);
        var nativeOperator = IGunOperator.fromLivingEntity(minecraft.player);
        var nativeGun = com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
        ResourceLocation gunId = nativeGun == null ? null : nativeGun.getGunId(stack);
        var commonIndex = gunId == null
                ? java.util.Optional.<com.tacz.guns.resource.index.CommonGunIndex>empty()
                : com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gunId);
        String drawTime = commonIndex
                .map(index -> index.getGunData() == null
                        ? "missing-data" : Float.toString(index.getGunData().getDrawTime()))
                .orElse("missing-index");
        LOGGER.warn("DEV_DRAW_PROBE_STATE waitTicks={} item={} isTaczGun={} gunId={} commonIndex={} drawTime={} drawTimestamp={} nativeDrawCooldown={} clientReadyToDraw={}",
                waitTicks,
                stack.isEmpty() ? "empty" : stack.getItem().getDescriptionId(),
                nativeGun != null,
                gunId == null ? "missing" : gunId,
                commonIndex.isPresent(),
                drawTime,
                clientOperator.getDataHolder().clientDrawTimestamp,
                nativeOperator.getSynDrawCoolDown(),
                clientOperator.isReadyToDraw());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ENABLED || holoScreenshotSent || holoScreenshotTicks < 0) return;
        if (minecraft.screen != event.getScreen()) return;
        if (!(event.getScreen() instanceof HoloGui) && !specialSchematicProbeActive) return;
        if (holoScreenshotTicks > 0) {
            holoScreenshotTicks--;
            return;
        }
        Screenshot.grab(minecraft.gameDirectory, "taczintetra-holosphere-special-slot-probe",
                minecraft.getMainRenderTarget(), ignored -> { });
        holoScreenshotSent = true;
        LOGGER.info("TaCZinTetra dev holosphere special-slot screenshot saved after Screen render: screen={}",
                minecraft.screen.getClass().getName());
    }

    @SubscribeEvent
    public static void onWorkbenchScreenRender(ScreenEvent.Render.Post event) {
        if (!ENABLED || workbenchClickSent || workbenchClickDelay < 0
                || !(event.getScreen() instanceof WorkbenchScreen workbenchScreen)) return;
        if (workbenchClickDelay > 0) {
            workbenchClickDelay--;
            return;
        }
        boolean handled = clickWorkbenchCraftButton(workbenchScreen);
        workbenchClickSent = true;
        LOGGER.info("TaCZinTetra dev workbench CraftButton render click probe: handled={}", handled);
    }

    @SubscribeEvent
    public static void onStableHoloScreenRender(ScreenEvent.Render.Post event) {
        if (!ENABLED || HOLD_HOLO || !holoScreenshotSent || holoClickSent || holoClickDelay < 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof HoloGui) || minecraft.screen != event.getScreen()) return;
        if (!holoBoundsLogged) {
            logHoloGuiBounds((HoloGui) event.getScreen());
            holoBoundsLogged = true;
        }
        if (holoClickDelay-- > 0) return;
        if (!holoClickSent) {
            boolean clicked = false;
            int hitX = -1;
            int hitY = -1;
            // 界面回调接收经过 GUI 缩放的逻辑坐标；
            // 保存的截图使用物理像素坐标（此处 GUI 缩放为 2）。
            for (int x = 0; x <= 80 && !clicked; x += 1) {
                for (int y = 45; y <= 125; y += 1) {
                    if (minecraft.screen.mouseClicked(x, y, 0)) {
                        clicked = true;
                        hitX = x;
                        hitY = y;
                        break;
                    }
                }
            }
            boolean directChildHandled = !clicked && clickVariantListChild(minecraft.screen);
            boolean opened = ((HoloGui) minecraft.screen).keyPressed(32, 0, 0);
            holoClickSent = true;
            logHoloSelectedVariant((HoloGui) minecraft.screen);
            LOGGER.info("TaCZinTetra dev holosphere special-slot GUI click probe: x={}, y={}, button=0, handled={}, directVariantChildHandled={}, spaceOpened={}",
                    hitX, hitY, clicked, directChildHandled, opened);
        }
    }

    static boolean clickWorkbenchCraftButton(WorkbenchScreen screen) {
        try {
            var field = WorkbenchScreen.class.getDeclaredField("defaultGui");
            field.setAccessible(true);
            Object root = field.get(screen);
            if (!(root instanceof GuiElement rootElement)) return false;
            return clickWorkbenchCraftButton(screen, rootElement, rootElement, 0, 0);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("TaCZinTetra dev workbench CraftButton probe failed: {}", exception.toString());
            return false;
        }
    }

    private static boolean clickWorkbenchCraftButton(WorkbenchScreen screen, GuiElement root,
                                                      Object candidate,
                                                      int parentX, int parentY) {
        if (!(candidate instanceof GuiElement element) || !element.isVisible()) return false;
        int x = parentX + element.getX();
        int y = parentY + element.getY();
        if (element.getClass().getName().contains("CraftButtonGui")) {
            int clickX = screen.getGuiLeft() + x + Math.max(1, element.getWidth() / 2);
            int clickY = screen.getGuiTop() + y + Math.max(1, element.getHeight() / 2);
            // WorkbenchScreen 会将原始逻辑屏幕坐标传给 defaultGui；根附着空间从 (0, 0) 开始，
            // 不从 AbstractContainerScreen.guiLeft/guiTop 开始。

            root.updateFocusState(0, 0, clickX, clickY);
            if (!element.hasFocus()) {
                int minX = Math.max(0, clickX - 120);
                int maxX = Math.min(screen.width, clickX + 120);
                int minY = Math.max(0, clickY - 80);
                int maxY = Math.min(screen.height, clickY + 80);
                for (int probeX = minX; probeX <= maxX && !element.hasFocus(); probeX++) {
                    for (int probeY = minY; probeY <= maxY; probeY++) {
                        root.updateFocusState(0, 0, probeX, probeY);
                        if (element.hasFocus()) {
                            clickX = probeX;
                            clickY = probeY;
                            LOGGER.info("TaCZinTetra dev workbench CraftButton attachment coordinate resolved: click={},{}",
                                    clickX, clickY);
                            break;
                        }
                    }
                }
            }
            LOGGER.info("TaCZinTetra dev workbench CraftButton bounds: x={}, y={}, size={}x{}, visible={}, focused={}, click={},{}",
                    x, y, element.getWidth(), element.getHeight(), element.isVisible(), element.hasFocus(), clickX, clickY);
            if (screen.mouseClicked(clickX, clickY, 0)) return true;
            boolean directHandled = root.onMouseClick(clickX, clickY, 0);
            LOGGER.info("TaCZinTetra dev workbench CraftButton direct root click probe: handled={}", directHandled);
            if (directHandled) return true;
        }
        for (GuiElement child : element.getChildren()) {
            if (clickWorkbenchCraftButton(screen, root, child, x, y)) return true;
        }
        return false;
    }

    private static void logHoloSelectedVariant(HoloGui screen) {
        try {
            var pageField = HoloGui.class.getDeclaredField("currentPage");
            pageField.setAccessible(true);
            Object page = pageField.get(screen);
            var stateField = page.getClass().getDeclaredField("state");
            stateField.setAccessible(true);
            Object state = stateField.get(page);
            var selected = state.getClass().getMethod("getSelectedVariant").invoke(state);
            LOGGER.info("TaCZinTetra dev holosphere selection state: page={}, selectedVariant={}",
                    page.getClass().getName(), selected == null ? "null" : selected.toString());
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("TaCZinTetra dev holosphere selection state probe failed: {}", exception.toString());
        }
    }

    private static boolean clickVariantListChild(Screen screen) {
        if (!(screen instanceof HoloGui holoGui)) return false;
        try {
            var field = HoloGui.class.getDeclaredField("currentPage");
            field.setAccessible(true);
            Object page = field.get(holoGui);
            if (page instanceof GuiElement pageElement) {
                // HoloGui 通常会在鼠标移动时从页面根节点更新焦点。
                // 向叶节点控件派发点击前，
                // 需要先复现这一步焦点传递。
                pageElement.updateFocusState(0, 0, 1, 61);
            }
            return clickVariantListChild(page, 0);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("TaCZinTetra dev direct variant click probe failed: {}", exception.toString());
            return false;
        }
    }

    private static boolean clickVariantListChild(Object candidate, int depth) {
        if (!(candidate instanceof GuiElement element) || depth > 10) return false;
        if (element.getClass().getName().contains("HoloVariantItemGui") && element.isVisible()) {
            // Tetra 的 GuiClickable 只有在页面鼠标移动处理更新焦点状态后才会接受点击。

            element.updateFocusState(0, 0, element.getX() + 1, element.getY() + 1);
            LOGGER.info("TaCZinTetra dev variant item focus probe: x={}, y={}, focused={}",
                    element.getX(), element.getY(), element.hasFocus());
            for (int x = 0; x < Math.max(1, element.getWidth()); x++) {
                for (int y = 0; y < Math.max(1, element.getHeight()); y++) {
                    if (element.onMouseClick(x, y, 0)) return true;
                }
            }
        }
        for (GuiElement child : element.getChildren()) {
            if (clickVariantListChild(child, depth + 1)) return true;
        }
        if (element.getClass().getName().contains("HoloVariantListGui") && element.isVisible()) {
            for (int x = 0; x < Math.max(1, element.getWidth()); x++) {
                for (int y = 0; y < Math.max(1, element.getHeight()); y++) {
                    if (element.onMouseClick(x, y, 0)) return true;
                }
            }
        }
        return false;
    }

    private static void logHoloGuiBounds(HoloGui screen) {
        try {
            var field = HoloGui.class.getDeclaredField("currentPage");
            field.setAccessible(true);
            Object page = field.get(screen);
            LOGGER.info("TaCZinTetra dev HoloGui bounds: screen={}x{}, page={}",
                    screen.width, screen.height, page == null ? "null" : page.getClass().getName());
            logHoloGuiElement(page, 0, 0, 0);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("TaCZinTetra dev HoloGui bounds probe failed: {}", exception.toString());
        }
    }

    private static void logHoloGuiElement(Object candidate, int parentX, int parentY, int depth) {
        if (!(candidate instanceof GuiElement element) || depth > 8) return;
        int x = parentX + element.getX();
        int y = parentY + element.getY();
        String className = element.getClass().getName();
        if (element.isVisible() || className.contains("HoloVariant")
                || className.contains("MaterialWrapper") || className.contains("HoloMaterial")) {
            LOGGER.info("TaCZinTetra dev HoloGui element: depth={}, class={}, bounds=({},{} {}x{}), visible={}, focused={}, children={}",
                    depth, element.getClass().getName(), x, y, element.getWidth(), element.getHeight(),
                    element.isVisible(), element.hasFocus(), element.getNumChildren());
        }
        for (GuiElement child : element.getChildren()) {
            logHoloGuiElement(child, x, y, depth + 1);
        }
    }
}
