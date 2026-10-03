package com.pycoder.taczintetra.logic;

import net.minecraft.nbt.CompoundTag;

/** 序列化资源数量，不让账户模型依赖 ItemStack。 */
public final class ResourceNbtAdapter {
    private static final String RESOURCES = "resources";

    private ResourceNbtAdapter() {
    }

    public static int read(CompoundTag root, String channelId, String resourceId) {
        if (root == null || channelId == null || resourceId == null) {
            return 0;
        }
        CompoundTag resources = root.getCompound(RESOURCES);
        return Math.max(0, resources.getCompound(channelId).getInt(resourceId));
    }

    public static void write(CompoundTag root, String channelId, String resourceId, int amount) {
        if (root == null || channelId == null || resourceId == null || channelId.isBlank()
                || resourceId.isBlank()) {
            return;
        }
        CompoundTag resources = root.getCompound(RESOURCES);
        CompoundTag channel = resources.getCompound(channelId);
        channel.putInt(resourceId, Math.max(0, amount));
        resources.put(channelId, channel);
        root.put(RESOURCES, resources);
    }
}
