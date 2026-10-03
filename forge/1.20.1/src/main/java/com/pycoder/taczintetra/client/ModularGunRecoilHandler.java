package com.pycoder.taczintetra.client;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/** Applies the configured per-stack pitch recoil without changing TaCZ's shared gun data. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class ModularGunRecoilHandler {
    private ModularGunRecoilHandler() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide() != LogicalSide.CLIENT
                || !(event.getShooter() instanceof LocalPlayer player)
                || !(event.getGunItemStack().getItem() instanceof ModularGunItem gun)) {
            return;
        }
        // TaCZ already owns the off-hand visual animation. Do not apply its
        // recoil to the player's camera, otherwise dual-pistol fire overwrites
        // the main-hand camera recoil channel.
        if (player.getOffhandItem() == event.getGunItemStack()) return;
        var profile = TetraItemStackProfileResolver.tryResolve(gun, event.getGunItemStack(),
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null) return;
        double recoil = profile.handling().recoilAdd();
        if (!Double.isFinite(recoil) || recoil <= 0) return;
        player.setXRot(player.getXRot() - (float) Math.min(90, recoil));
    }
}
