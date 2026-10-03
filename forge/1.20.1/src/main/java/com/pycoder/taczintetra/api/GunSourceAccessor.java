package com.pycoder.taczintetra.api;

import net.minecraft.world.item.ItemStack;

/** Exposes the firing-stack snapshot captured by a TaCZ bullet mixin. */
public interface GunSourceAccessor {
    ItemStack sourceGun();
}
