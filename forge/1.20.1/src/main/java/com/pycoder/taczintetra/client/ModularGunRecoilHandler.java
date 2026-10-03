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

/** 按物品栈配置施加俯仰后坐力，不修改 TaCZ 共享枪械数据。 */
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
        // 副手视觉动画已由 TaCZ 控制。不要将其
        // 后坐力施加到玩家视角，否则双手持枪射击会覆盖
        // 主手的视角后坐力通道。
        if (player.getOffhandItem() == event.getGunItemStack()) return;
        var profile = TetraItemStackProfileResolver.tryResolve(gun, event.getGunItemStack(),
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null) return;
        double recoil = profile.handling().recoilAdd();
        if (!Double.isFinite(recoil) || recoil <= 0) return;
        player.setXRot(player.getXRot() - (float) Math.min(90, recoil));
    }
}
