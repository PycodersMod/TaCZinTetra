package com.pycoder.taczintetra.logic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 线程安全的静态档案缓存；运行时热量/弹药不会缓存在此处。 */
public final class GunProfileCache {
    private static final ConcurrentMap<GunProfileCacheKey, GunProfileResolver.ResolvedGunProfile> VALUES = new ConcurrentHashMap<>();

    private GunProfileCache() { }

    public static GunProfileResolver.ResolvedGunProfile getOrResolve(GunProfileCacheKey key,
                                                                       GunProfileResolver.PartStats body,
                                                                       GunProfileResolver.PartStats barrel,
                                                                       GunProfileResolver.PartStats magazine) {
        if (key == null) return GunProfileResolver.resolveProfiles(body, barrel, magazine);
        return VALUES.computeIfAbsent(key, ignored -> GunProfileResolver.resolveProfiles(body, barrel, magazine));
    }

    public static void clear() { VALUES.clear(); }
    public static int size() { return VALUES.size(); }
}
