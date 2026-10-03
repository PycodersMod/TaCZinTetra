package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Advances heat independently for each held modular gun on the server. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class GunHeatTickHandler {
    private GunHeatTickHandler() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        long gameTime = event.player.level().getGameTime();
        for (InteractionHand hand : InteractionHand.values()) {
            var stack = event.player.getItemInHand(hand);
            if (stack.getItem() instanceof ModularGunItem gun) {
                if (hand == InteractionHand.MAIN_HAND) GunCacheSynchronizer.ensure(event.player, stack);
                gun.tickHeat(stack, gameTime);
            }
        }
        ReloadRuntimeCoordinator.reconcile(event.player);
    }
}
