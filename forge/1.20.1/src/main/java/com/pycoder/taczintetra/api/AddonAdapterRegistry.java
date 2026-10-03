package com.pycoder.taczintetra.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe addon adapter registry; registration is optional and reversible. */
public final class AddonAdapterRegistry {
    private static final CopyOnWriteArrayList<ResourceInsertionAdapter> ITEM_ADAPTERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ExternalCapabilityAdapter> EXTERNAL_ADAPTERS = new CopyOnWriteArrayList<>();

    private AddonAdapterRegistry() {
    }

    @FunctionalInterface
    public interface Registration {
        void close();
    }

    public static void register(ResourceInsertionAdapter adapter) {
        if (adapter != null && !ITEM_ADAPTERS.contains(adapter)) ITEM_ADAPTERS.add(adapter);
    }

    public static void register(ExternalCapabilityAdapter adapter) {
        if (adapter != null && !EXTERNAL_ADAPTERS.contains(adapter)) EXTERNAL_ADAPTERS.add(adapter);
    }

    /** Registers an external adapter for a bounded scope and removes it exactly once when closed. */
    public static Registration registerScoped(ExternalCapabilityAdapter adapter) {
        if (adapter == null) return () -> { };
        register(adapter);
        return new Registration() {
            private boolean closed;

            @Override
            public synchronized void close() {
                if (!closed) {
                    closed = true;
                    unregister(adapter);
                }
            }
        };
    }

    public static void unregister(ResourceInsertionAdapter adapter) { ITEM_ADAPTERS.remove(adapter); }

    public static void unregister(ExternalCapabilityAdapter adapter) { EXTERNAL_ADAPTERS.remove(adapter); }

    public static List<ResourceInsertionAdapter> itemAdapters() { return List.copyOf(ITEM_ADAPTERS); }

    public static List<ExternalCapabilityAdapter> externalAdapters() { return List.copyOf(EXTERNAL_ADAPTERS); }
}
