package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GunProfileCacheTest {
    @Test void cacheReusesStaticProfileAndCanBeInvalidated() {
        GunProfileCache.clear();
        GunProfileCacheKey key = new GunProfileCacheKey(new String[]{"body"}, null, 0, null, 1);
        GunProfileResolver.ResolvedGunProfile first = GunProfileCache.getOrResolve(key, null, null, null);
        GunProfileResolver.ResolvedGunProfile second = GunProfileCache.getOrResolve(key,
                new GunProfileResolver.PartStats(99, 0, 1, 1, 0, 0, 1, 1), null, null);
        assertSame(first, second);
        assertEquals(1, GunProfileCache.size());
        GunProfileCache.clear();
        assertEquals(0, GunProfileCache.size());
    }
}
