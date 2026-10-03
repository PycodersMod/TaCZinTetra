package com.pycoder.taczintetra.compat;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.api.GunRuntimeEvent;
import com.pycoder.taczintetra.api.GunSourceAccessor;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 将 TaCZ 动能子弹的伤害结果转发到公开运行时事件 API。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class GunRuntimeEventHandler {
    private GunRuntimeEventHandler() {
    }

    @SubscribeEvent
    public static void onBulletDamage(EntityHurtByGunEvent.Pre event) {
        if (!(event.getBullet() instanceof EntityKineticBullet bullet)
                || event.getAttacker() == null) {
            return;
        }
        ItemStack sourceGun = sourceGun(bullet);
        if (!(sourceGun.getItem() instanceof ModularGunItem)) return;
        LivingEntity actor = event.getAttacker();
        if (MinecraftForge.EVENT_BUS.post(new GunRuntimeEvent(
                GunRuntimeEvent.Type.BULLET_HIT, actor, sourceGun))) {
            event.setCanceled(true);
            return;
        }
        if (event.isHeadShot()) {
            MinecraftForge.EVENT_BUS.post(new GunRuntimeEvent(
                    GunRuntimeEvent.Type.BULLET_HEADSHOT, actor, sourceGun));
        }
    }

    @SubscribeEvent
    public static void onBulletKill(LivingDeathEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        Entity owner = event.getSource().getEntity();
        if (!(direct instanceof EntityKineticBullet bullet)
                || !(owner instanceof LivingEntity actor)) {
            return;
        }
        ItemStack sourceGun = sourceGun(bullet);
        if (!(sourceGun.getItem() instanceof ModularGunItem)) return;
        MinecraftForge.EVENT_BUS.post(new GunRuntimeEvent(
                GunRuntimeEvent.Type.BULLET_KILL, actor, sourceGun));
    }

    private static ItemStack sourceGun(EntityKineticBullet bullet) {
        if (bullet instanceof GunSourceAccessor source) {
            ItemStack stack = source.sourceGun();
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }
}
