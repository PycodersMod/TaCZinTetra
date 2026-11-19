package com.pycoder.taczintetra.client;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.pycoder.taczintetra.logic.AmmoRecipeDefinition;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import com.pycoder.taczintetra.runtime.ExternalResourceService;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** 显示当前主手模组枪械及可选副手枪械的客户端 HUD。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class ModularGunHudHandler {
    private static final ResourceLocation TACZ_HEAT_BAR = ResourceLocation.fromNamespaceAndPath("tacz", "tac_heat_bar");
    private static final boolean HEAT_OVERLAY_SCREENSHOT_PROBE = Boolean.getBoolean("taczintetra.heat_overlay_screenshot_probe");
    private static final boolean REFIT_SCREEN_SCREENSHOT_PROBE = Boolean.getBoolean("taczintetra.refit_screen_screenshot_probe");
    private static final int RIGHT_MARGIN = 8;
    private static final int LINE_HEIGHT = 10;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int devLoggedRpm = Integer.MIN_VALUE;
    private static int heatOverlayScreenshotDelay = 20;
    private static boolean heatOverlayScreenshotSent;
    private static int refitScreenScreenshotDelay = 20;
    private static boolean refitScreenOpened;
    private static boolean refitScreenScreenshotSent;

    private ModularGunHudHandler() {
    }

    @SubscribeEvent
    public static void hideTaczHeatBarForModularGun(RenderGuiOverlayEvent.Pre event) {
        if (!TACZ_HEAT_BAR.equals(event.getOverlay().id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void captureHeatOverlayProbe(TickEvent.ClientTickEvent event) {
        if (!HEAT_OVERLAY_SCREENSHOT_PROBE || event.phase != TickEvent.Phase.END || heatOverlayScreenshotSent) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !minecraft.options.getCameraType().isFirstPerson()
                || !(minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem)) {
            return;
        }
        if (heatOverlayScreenshotDelay-- > 0) {
            return;
        }
        Screenshot.grab(minecraft.gameDirectory, "taczintetra-heat-overlay-probe",
                minecraft.getMainRenderTarget(), ignored -> { });
        heatOverlayScreenshotSent = true;
        LOGGER.info("TaCZinTetra heat overlay visual probe screenshot captured for main-hand modular gun");
    }

    @SubscribeEvent
    public static void captureRefitScreenProbe(TickEvent.ClientTickEvent event) {
        if (!REFIT_SCREEN_SCREENSHOT_PROBE || event.phase != TickEvent.Phase.END || refitScreenScreenshotSent) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof ModularGunItem)) {
            return;
        }
        if (!refitScreenOpened) {
            if (minecraft.screen != null) {
                return;
            }
            minecraft.setScreen(new GunRefitScreen());
            refitScreenOpened = true;
            LOGGER.info("TaCZinTetra refit-screen visual probe opened native TaCZ GunRefitScreen for main-hand modular gun");
            return;
        }
        if (!(minecraft.screen instanceof GunRefitScreen)) {
            return;
        }
        if (refitScreenScreenshotDelay-- > 0) {
            return;
        }
        Screenshot.grab(minecraft.gameDirectory, "taczintetra-refit-screen-probe",
                minecraft.getMainRenderTarget(), ignored -> { });
        refitScreenScreenshotSent = true;
        LOGGER.info("TaCZinTetra refit-screen visual probe screenshot captured after native screen initialization");
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ItemStack main = minecraft.player.getMainHandItem();
        if (!(main.getItem() instanceof ModularGunItem gun)) {
            return;
        }
        if (Boolean.getBoolean("taczintetra.dev_automation")) {
            int rpm = gun.getRPM(main);
            if (rpm != devLoggedRpm) {
                devLoggedRpm = rpm;
                LOGGER.info("TaCZinTetra dev HUD snapshot: hand=MAIN_HAND, roundsPerMinute={}, ammo={}, resourceCapacity={}",
                        rpm, gun.getCurrentAmmoCount(main), gun.getResourceCapacity(main));
            }
        }

        // 主手为权威来源：仅有副手枪械时绝不显示 HUD。
        ItemStack offhand = minecraft.player.getOffhandItem();
        ModularGunItem offhandGun = offhand.getItem() instanceof ModularGunItem value ? value : null;
        int y = event.getWindow().getGuiScaledHeight() - 38;
        if (offhandGun != null) {
            y = renderGun(event, minecraft, offhandGun, offhand, "副手", y);
            y -= LINE_HEIGHT;
        }
        renderGun(event, minecraft, gun, main, "主手", y);
    }

    private static int renderGun(RenderGuiOverlayEvent.Post event, Minecraft minecraft,
                                 ModularGunItem gun, ItemStack stack, String hand, int y) {
        // TaCZ's GunHudOverlay owns the standard magazine and fire-mode HUD.
        // This overlay only adds Tetra's multi-resource reserve information.
        AmmoRecipeDefinition recipe = ConfiguredAmmoRecipeResolver.select(
                TaCZinTetra.MODULE_CONFIG, gun, stack);
        if (recipe == null || recipe.cost().isEmpty()) {
            return y;
        }
        for (var entry : recipe.cost().entrySet()) {
            var external = ExternalResourceService.adapterFor(entry.getKey(), minecraft.player);
            int current = external == null ? ResourceNbtAdapter.read(stack.getOrCreateTag(),
                    ResourceInsertionService.ITEM_CHANNEL, entry.getKey())
                    : Math.max(0, external.available(minecraft.player, entry.getKey()));
            int maximum = Math.max(0, gun.getResourceCapacity(stack));
            String resource = shortResourceId(entry.getKey()) + " " + current + " / " + maximum;
            drawRight(event, minecraft, resource, y, 0xD0D0D0);
            y -= LINE_HEIGHT;
        }
        return y;
    }

    private static void drawRight(RenderGuiOverlayEvent.Post event, Minecraft minecraft,
                                  String text, int y, int color) {
        int x = event.getWindow().getGuiScaledWidth() - minecraft.font.width(text) - RIGHT_MARGIN;
        event.getGuiGraphics().drawString(minecraft.font, Component.literal(text), x, y, color, true);
    }

    private static String shortResourceId(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) return "?";
        int separator = resourceId.lastIndexOf(':');
        return separator >= 0 && separator + 1 < resourceId.length()
                ? resourceId.substring(separator + 1) : resourceId;
    }
}
