package com.pycoder.taczintetra.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** 生成由客户端意图携带的非负物品栈指纹。 */
public final class StackIdentity {
    public static final String UNIQUE_ID = "taczintetra_stack_uuid";

    private StackIdentity() {
    }

    public static int of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int result = BuiltInRegistries.ITEM.getId(stack.getItem());
        return fromComponents(result, stack.getCount(),
                stack.getTag() == null ? 0 : canonicalTagHash(stack.getTag()));
    }

    /** 权威端缺少标记时，为该物品栈添加持久标记。 */
    public static void ensureUniqueId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(UNIQUE_ID)) tag.putUUID(UNIQUE_ID, UUID.randomUUID());
    }

    /**
     * 刻意排除运行时状态：服务端弹药、热量、耐久
     * 和 TaCZ 桥接标签都可能在客户端收到同步前发生变化。
     * 模块选择及其他持久配置标签仍属于物品身份。
     */
    static int canonicalTagHash(CompoundTag tag) {
        if (tag == null) return 0;
        CompoundTag canonical = tag.copy();
        canonical.remove("taczintetra_ammo");
        canonical.remove("taczintetra_max_ammo");
        canonical.remove("taczintetra_bullet");
        canonical.remove("taczintetra_heat");
        canonical.remove("taczintetra_overheat");
        canonical.remove("taczintetra_heat_tick");
        canonical.remove("taczintetra_fire_mode");
        canonical.remove("AmmoCount");
        canonical.remove("Damage");
        canonical.remove("GunId");
        canonical.remove("resources");
        canonical.remove("taczintetra_reload_fill");
        canonical.remove("taczintetra_reload_pending");
        canonical.remove("taczintetra_reload_started_at");
        canonical.remove("taczintetra_reload_state");
        return canonical.hashCode();
    }

    static int fromComponents(int itemId, int count, int tagHash) {
        int result = itemId;
        result = 31 * result + count;
        result = 31 * result + tagHash;
        return normalize(result);
    }

    static int normalize(int hash) {
        return Math.floorMod(hash, Integer.MAX_VALUE);
    }
}
