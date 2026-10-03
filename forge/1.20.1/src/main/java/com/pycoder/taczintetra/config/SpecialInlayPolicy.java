package com.pycoder.taczintetra.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 通过数据配置验证特殊嵌片并将其写入 NBT。 */
public final class SpecialInlayPolicy {
    public static final String NBT_KEY = "taczintetra_special_inlays";

    private SpecialInlayPolicy() {
    }

    public static Result resolve(CompoundTag tag, ModuleConfig config) {
        if (tag == null) return Result.empty();
        ListTag values = tag.getList(NBT_KEY, Tag.TAG_STRING);
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        for (Tag value : values) {
            if (value instanceof StringTag string && !string.getAsString().isBlank()) {
                requested.add(string.getAsString());
            }
        }
        return resolve(requested, config);
    }

    public static Result resolve(Collection<String> requested, ModuleConfig config) {
        return resolve(requested, config, true);
    }

    /** 仅在真实特殊槽位已安装时解析持久化的嵌片 ID。 */
    public static Result resolve(Collection<String> requested, ModuleConfig config, boolean socketInstalled) {
        if (config == null) config = ModuleConfig.empty();
        Map<String, ModuleConfig.ConfigEntry> catalog = config.specialInlays().stream()
                .collect(Collectors.toMap(ModuleConfig.ConfigEntry::id, entry -> entry,
                        (first, ignored) -> first));
        int capacity = socketInstalled ? socketCapacity(config) : 0;
        int used = 0;
        LinkedHashSet<String> active = new LinkedHashSet<>();
        LinkedHashSet<String> rejected = new LinkedHashSet<>();
        if (requested != null) {
            for (String id : new LinkedHashSet<>(requested)) {
                if (id == null || id.isBlank()) continue;
                ModuleConfig.ConfigEntry entry = catalog.get(id);
                int cost = entry == null ? 1 : cost(entry);
                if (entry == null || !entry.allowed() || cost > capacity - used) {
                    rejected.add(id);
                    continue;
                }
                active.add(id);
                used += cost;
            }
        }
        return new Result(Set.copyOf(active), Set.copyOf(rejected), capacity, used);
    }

    public static void write(CompoundTag tag, Collection<String> ids) {
        if (tag == null) return;
        ListTag values = new ListTag();
        if (ids != null) {
            for (String id : new LinkedHashSet<>(ids)) {
                if (id != null && !id.isBlank()) values.add(StringTag.valueOf(id));
            }
        }
        tag.put(NBT_KEY, values);
    }

    private static int socketCapacity(ModuleConfig config) {
        return config.specialInlays().stream()
                .filter(entry -> "socket".equals(entry.id()) || "socket".equals(entry.slot()))
                .mapToInt(SpecialInlayPolicy::capacity)
                .sum();
    }

    private static int capacity(ModuleConfig.ConfigEntry entry) {
        return Math.max(0, (int) Math.round(value(entry, "capacity", 0)));
    }

    private static int cost(ModuleConfig.ConfigEntry entry) {
        return Math.max(1, (int) Math.round(value(entry, "cost", 1)));
    }

    private static double value(ModuleConfig.ConfigEntry entry, String key, double fallback) {
        Double value = entry.stats().get(key);
        return value != null && Double.isFinite(value) ? value : fallback;
    }

    public record Result(Set<String> activeIds, Set<String> rejectedIds, int capacity, int usedCapacity) {
        public Result {
            activeIds = Set.copyOf(activeIds == null ? Set.of() : activeIds);
            rejectedIds = Set.copyOf(rejectedIds == null ? Set.of() : rejectedIds);
            capacity = Math.max(0, capacity);
            usedCapacity = Math.max(0, usedCapacity);
        }

        public static Result empty() {
            return new Result(Set.of(), Set.of(), 0, 0);
        }
    }
}
