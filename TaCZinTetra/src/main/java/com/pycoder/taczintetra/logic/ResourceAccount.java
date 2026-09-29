package com.pycoder.taczintetra.logic;

import java.util.HashMap;
import java.util.Map;

/** Immutable resource-channel snapshot; mutation into ItemStack NBT is an adapter concern. */
public final class ResourceAccount {
    private final Map<String, Integer> amounts;

    private ResourceAccount(Map<String, Integer> amounts) {
        this.amounts = Map.copyOf(amounts);
    }

    public static ResourceAccount empty() {
        return new ResourceAccount(Map.of());
    }

    public int amount(String resourceId) {
        return resourceId == null ? 0 : amounts.getOrDefault(resourceId, 0);
    }

    /** Returns the number of complete batches payable from all required resources. */
    public int affordableBatches(Map<String, Integer> cost) {
        if (cost == null || cost.isEmpty()) return Integer.MAX_VALUE;
        long result = Long.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            int unit = entry.getValue() == null ? 0 : entry.getValue();
            if (unit <= 0) continue;
            result = Math.min(result, (long) amount(entry.getKey()) / unit);
        }
        return result == Long.MAX_VALUE ? 0 : (int) Math.min(Integer.MAX_VALUE, result);
    }

    /** Consumes one or more complete batches and leaves the original account unchanged. */
    public ResourceAccount consume(Map<String, Integer> cost, int batches) {
        if (cost == null || batches <= 0 || affordableBatches(cost) < batches) return this;
        Map<String, Integer> next = new HashMap<>(amounts);
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            int unit = entry.getValue() == null ? 0 : entry.getValue();
            if (unit > 0) {
                long remaining = (long) amount(entry.getKey()) - (long) unit * batches;
                next.put(entry.getKey(), (int) Math.max(0, Math.min(Integer.MAX_VALUE, remaining)));
            }
        }
        return new ResourceAccount(next);
    }

    public Insertion insert(String resourceId, int requested, int capacity) {
        if (resourceId == null || resourceId.isBlank() || requested <= 0 || capacity <= 0) {
            return new Insertion(this, 0, 0);
        }
        int accepted = Math.min(requested, Math.max(0, capacity - amount(resourceId)));
        Map<String, Integer> next = new HashMap<>(amounts);
        next.put(resourceId, amount(resourceId) + accepted);
        return new Insertion(new ResourceAccount(next), accepted, requested - accepted);
    }

    public record Insertion(ResourceAccount account, int inserted, int remainder) {
    }
}
