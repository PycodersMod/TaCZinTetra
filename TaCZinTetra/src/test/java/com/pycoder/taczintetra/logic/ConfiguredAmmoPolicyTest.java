package com.pycoder.taczintetra.logic;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfiguredAmmoPolicyTest {
    private static final ResourceLocation FALLBACK = new ResourceLocation("tacz", "9mm");

    @Test
    void explicitValidIdWinsAndMissingIdUsesNativeFallback() {
        assertEquals(new ResourceLocation("tacz", "40mm"),
                ConfiguredAmmoPolicy.resolve(" tacz:40mm ", FALLBACK));
        assertEquals(FALLBACK, ConfiguredAmmoPolicy.resolve("", FALLBACK));
        assertEquals(FALLBACK, ConfiguredAmmoPolicy.resolve(null, FALLBACK));
    }

    @Test
    void explicitMalformedIdDoesNotSilentlyBecomeTheFallback() {
        assertNull(ConfiguredAmmoPolicy.resolve("not a resource id", FALLBACK));
    }
}
