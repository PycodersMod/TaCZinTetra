package com.pycoder.taczintetra.client;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.AmmoRecipeDefinition;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import com.pycoder.taczintetra.runtime.ExternalResourceService;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** 显示当前主手模组枪械及可选副手枪械的客户端 HUD。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class ModularGunHudHandler {
    private static final int RIGHT_MARGIN = 8;
    private static final int LINE_HEIGHT = 10;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int devLoggedRpm = Integer.MIN_VALUE;

    private ModularGunHudHandler() {
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
        String ammo = hand + " " + gun.getCurrentAmmoCount(stack) + " / " + gun.getMaxDummyAmmoAmount(stack);
        drawRight(event, minecraft, ammo, y, 0xFFFFFF);
        y -= LINE_HEIGHT;

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
