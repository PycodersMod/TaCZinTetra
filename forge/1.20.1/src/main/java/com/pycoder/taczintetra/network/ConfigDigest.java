package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.config.ModuleConfig;

/** Stable-enough session digest used to reject silent client/server config drift. */
public final class ConfigDigest {
    private ConfigDigest() { }

    public static String of(ModuleConfig config) {
        return Integer.toUnsignedString(config == null ? 0 : config.hashCode(), 16);
    }
}
