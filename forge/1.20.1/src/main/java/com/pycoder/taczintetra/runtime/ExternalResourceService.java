package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.api.AddonAdapterRegistry;
import com.pycoder.taczintetra.api.ExternalCapabilityAdapter;
import net.minecraft.world.entity.LivingEntity;

/** Read-only discovery and authoritative extraction for optional addon resource backends. */
public final class ExternalResourceService {
    private ExternalResourceService() { }

    public static ExternalCapabilityAdapter adapterFor(String resourceId, LivingEntity holder) {
        if (holder == null || resourceId == null || resourceId.isBlank()) return null;
        return AddonAdapterRegistry.externalAdapters().stream()
                .filter(adapter -> adapter.supports(resourceId))
                .findFirst().orElse(null);
    }

    public static int available(String resourceId, LivingEntity holder) {
        ExternalCapabilityAdapter adapter = adapterFor(resourceId, holder);
        return adapter == null ? 0 : Math.max(0, adapter.available(holder, resourceId));
    }

    public static boolean extract(ExternalCapabilityAdapter adapter, LivingEntity holder,
                                  String resourceId, int amount) {
        return adapter != null && holder != null && amount > 0
                && adapter.canExtract(holder, resourceId, amount)
                && isExactExtraction(amount, adapter.extract(holder, resourceId, amount));
    }

    /** An extraction is committed only when the backend debits exactly what was requested. */
    static boolean isExactExtraction(int requested, int extracted) {
        return requested > 0 && extracted == requested;
    }

    public static boolean rollback(ExternalCapabilityAdapter adapter, LivingEntity holder,
                                   String resourceId, int amount) {
        return adapter != null && holder != null && amount > 0
                && adapter.supportsRollback()
                && adapter.rollback(holder, resourceId, amount);
    }
}
