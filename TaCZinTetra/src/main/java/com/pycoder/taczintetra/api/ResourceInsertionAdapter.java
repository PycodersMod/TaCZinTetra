package com.pycoder.taczintetra.api;

import net.minecraft.world.item.ItemStack;

/** Optional adapter for addon-provided resource insertion into a modular gun. */
public interface ResourceInsertionAdapter {
    ResourceChannelType channelType();

    boolean supports(ItemStack resource);

    int insert(ItemStack gun, ItemStack resource, int limit);
}
