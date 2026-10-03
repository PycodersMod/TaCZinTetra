package com.pycoder.taczintetra.api;

import net.minecraft.world.item.ItemStack;

/** 暴露 TaCZ 子弹 mixin 捕获的射击物品栈快照。 */
public interface GunSourceAccessor {
    ItemStack sourceGun();
}
