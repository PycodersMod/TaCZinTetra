package com.pycoder.taczintetra.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 线程安全的附属适配器注册表；注册是可选的，并且可以撤销。 */
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

    /** 在指定作用域内注册外部适配器，并在关闭时恰好移除一次。 */
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
