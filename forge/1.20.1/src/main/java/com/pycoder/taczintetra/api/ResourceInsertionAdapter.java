package com.pycoder.taczintetra.api;

import net.minecraft.world.item.ItemStack;

/** 用于向模组枪械插入附属模组资源的可选适配器。 */
public interface ResourceInsertionAdapter {
    ResourceChannelType channelType();

    boolean supports(ItemStack resource);

    int insert(ItemStack gun, ItemStack resource, int limit);
}
