package com.pycoder.taczintetra.client;

import java.util.Map;
import java.util.Set;

/** Opt-in test fixture for rendering each supported Tetra body with TaCZ's native display rig. */
record DevBodyVisualProbeProfile(String family, String bodyModuleId, String barrelModuleId, String ammoId) {
    private static final Map<String, DevBodyVisualProbeProfile> PROFILES = Map.of(
            "pistol", new DevBodyVisualProbeProfile("pistol", "taczintetra/body/pistol",
                    "taczintetra/barrel/9mm", "tacz:9mm"),
            "rifle", new DevBodyVisualProbeProfile("rifle", "taczintetra/body/rifle",
                    "taczintetra/barrel/556", "tacz:556"),
            "machine_gun", new DevBodyVisualProbeProfile("machine_gun", "taczintetra/body/machine_gun",
                    "taczintetra/barrel/556", "tacz:556"),
            "shotgun", new DevBodyVisualProbeProfile("shotgun", "taczintetra/body/shotgun",
                    "taczintetra/barrel/12g", "tacz:12g"),
            "sniper", new DevBodyVisualProbeProfile("sniper", "taczintetra/body/sniper",
                    "taczintetra/barrel/762", "tacz:762"));

    static DevBodyVisualProbeProfile forFamily(String family) {
        if (family == null) throw new IllegalArgumentException("Body family must not be null");
        DevBodyVisualProbeProfile profile = PROFILES.get(family);
        if (profile == null) throw new IllegalArgumentException("Unsupported Tetra body family: " + family);
        return profile;
    }

    static boolean supportsFamily(String family) {
        return family != null && PROFILES.containsKey(family);
    }

    static Set<String> supportedFamilies() {
        return PROFILES.keySet();
    }
}
