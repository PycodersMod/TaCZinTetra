package com.pycoder.taczintetra.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;

/** 面向非物品资源后端的可选服务端桥接接口。 */
public interface ExternalCapabilityAdapter {
    ResourceChannelType channelType();

    /** 可选资源过滤器；返回 true 时，适配器无需依赖附属模组的物品类。 */
    default boolean supports(String resourceId) {
        return resourceId != null && !resourceId.isBlank();
    }

    /** 返回持有者当前可用资源的权威数量。 */
    default int available(LivingEntity holder, String resourceId) {
        return 0;
    }

    boolean canExtract(LivingEntity holder, String resourceId, int amount);

    int extract(LivingEntity holder, String resourceId, int amount);

    /**
     * 后续扣款失败时，是否能够补偿并保留资源。
     * 现有适配器仍然有效；但只有每个参与适配器都明确选择加入后，
     * 才会开始多资源结算。
     */
    default boolean supportsRollback() {
        return false;
    }

    /** 补偿此前由 extract 返回的数量。 */
    default boolean rollback(LivingEntity holder, String resourceId, int amount) {
        return false;
    }

    default boolean canInsert(ItemStack gun, String resourceId, int amount) {
        return gun != null && !gun.isEmpty() && amount > 0;
    }
}
