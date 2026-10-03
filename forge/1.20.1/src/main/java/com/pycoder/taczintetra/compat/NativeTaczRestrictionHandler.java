package com.pycoder.taczintetra.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.pycoder.taczintetra.TaCZinTetra;
import com.tacz.guns.api.event.common.GunFireEvent;

/** 在游戏逻辑边界处强制执行 TaCZ 原生限制。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class NativeTaczRestrictionHandler {
    private NativeTaczRestrictionHandler() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        if (NativeTaczRestrictionService.shouldBlockNativeWorkbench(id)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        if (NativeTaczRestrictionService.shouldBlockNativeItem(id)) {
            event.getCrafting().setCount(0);
        }
    }

    @SubscribeEvent
    public static void onNativeGunFire(GunFireEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getGunItemStack().getItem());
        if (NativeTaczRestrictionService.shouldBlockNativeItem(id) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }
}
