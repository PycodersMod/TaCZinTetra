package com.pycoder.taczintetra.logic;

import java.util.Arrays;

/** 不可变的静态档案缓存键；刻意排除高频变化的热量数据。 */
public record GunProfileCacheKey(String[] moduleIds, String[] materialIds,
                                 int honingLevel, String[] improvementIds,
                                 int dataVersion) {
    public GunProfileCacheKey {
        moduleIds = copy(moduleIds);
        materialIds = copy(materialIds);
        improvementIds = copy(improvementIds);
        dataVersion = Math.max(0, dataVersion);
    }

    private static String[] copy(String[] values) {
        return values == null ? new String[0] : Arrays.copyOf(values, values.length);
    }

    @Override public String[] moduleIds() { return Arrays.copyOf(moduleIds, moduleIds.length); }
    @Override public String[] materialIds() { return Arrays.copyOf(materialIds, materialIds.length); }
    @Override public String[] improvementIds() { return Arrays.copyOf(improvementIds, improvementIds.length); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GunProfileCacheKey key)) return false;
        return honingLevel == key.honingLevel && dataVersion == key.dataVersion
                && Arrays.equals(moduleIds, key.moduleIds)
                && Arrays.equals(materialIds, key.materialIds)
                && Arrays.equals(improvementIds, key.improvementIds);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(moduleIds);
        result = 31 * result + Arrays.hashCode(materialIds);
        result = 31 * result + honingLevel;
        result = 31 * result + Arrays.hashCode(improvementIds);
        return 31 * result + dataVersion;
    }
}
