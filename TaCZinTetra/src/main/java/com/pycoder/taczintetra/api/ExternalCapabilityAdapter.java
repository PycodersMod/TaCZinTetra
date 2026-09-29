package com.pycoder.taczintetra.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;

/** Optional server-side bridge for non-item resource backends. */
public interface ExternalCapabilityAdapter {
    ResourceChannelType channelType();

    /** Optional resource filter; returning true keeps the adapter independent of addon item classes. */
    default boolean supports(String resourceId) {
        return resourceId != null && !resourceId.isBlank();
    }

    /** Reports the authoritative amount available to the holder. */
    default int available(LivingEntity holder, String resourceId) {
        return 0;
    }

    boolean canExtract(LivingEntity holder, String resourceId, int amount);

    int extract(LivingEntity holder, String resourceId, int amount);

    /**
     * Whether a failed later debit can be compensated without losing resources.
     * Existing adapters remain valid, but multi-resource settlements will not
     * start unless every participating adapter opts in.
     */
    default boolean supportsRollback() {
        return false;
    }

    /** Compensates an amount previously returned by extract. */
    default boolean rollback(LivingEntity holder, String resourceId, int amount) {
        return false;
    }

    default boolean canInsert(ItemStack gun, String resourceId, int amount) {
        return gun != null && !gun.isEmpty() && amount > 0;
    }
}
