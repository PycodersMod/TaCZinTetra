package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 仅处理服务端右键插入，并将剩余物品留在来源物品栈中。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class ResourceInsertionHandler {
    private ResourceInsertionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        ItemStack used = event.getItemStack();
        ItemStack other = event.getEntity().getItemInHand(event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack gun = used.getItem() instanceof ModularGunItem ? used : other;
        ItemStack resource = used.getItem() instanceof ModularGunItem ? other : used;
        int inserted = ResourceInsertionService.insert(gun, resource);
        if (inserted > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
