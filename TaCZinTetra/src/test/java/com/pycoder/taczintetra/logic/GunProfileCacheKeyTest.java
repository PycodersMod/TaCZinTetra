package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GunProfileCacheKeyTest {
    @Test void keyCopiesArraysAndExcludesRuntimeHeat() {
        String[] modules = {"body", "barrel"};
        GunProfileCacheKey key = new GunProfileCacheKey(modules, new String[]{"standard"}, 2, new String[]{"optic"}, 1);
        modules[0] = "changed";
        assertEquals("body", key.moduleIds()[0]);
        assertEquals(key, new GunProfileCacheKey(new String[]{"body", "barrel"}, new String[]{"standard"}, 2, new String[]{"optic"}, 1));
        assertEquals(0, new GunProfileCacheKey(null, null, -2, null, -1).dataVersion());
    }
}
