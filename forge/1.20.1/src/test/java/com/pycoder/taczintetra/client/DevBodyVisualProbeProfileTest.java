package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevBodyVisualProbeProfileTest {
    @Test
    void mapsEverySupportedBodyFamilyToItsTetraBodyBarrelAndAmmo() {
        assertProfile("pistol", "taczintetra/body/pistol", "taczintetra/barrel/9mm", "tacz:9mm");
        assertProfile("rifle", "taczintetra/body/rifle", "taczintetra/barrel/556", "tacz:556");
        assertProfile("machine_gun", "taczintetra/body/machine_gun", "taczintetra/barrel/556", "tacz:556");
        assertProfile("shotgun", "taczintetra/body/shotgun", "taczintetra/barrel/12g", "tacz:12g");
        assertProfile("sniper", "taczintetra/body/sniper", "taczintetra/barrel/762", "tacz:762");
    }

    @Test
    void rejectsUnknownFamilyInsteadOfSilentlyShowingPistol() {
        assertThrows(IllegalArgumentException.class, () -> DevBodyVisualProbeProfile.forFamily("unknown"));
        assertThrows(IllegalArgumentException.class, () -> DevBodyVisualProbeProfile.forFamily(null));
    }

    @Test
    void exposesExactlyTheFiveBodyFamilies() {
        assertEquals(Set.of("pistol", "rifle", "machine_gun", "shotgun", "sniper"),
                DevBodyVisualProbeProfile.supportedFamilies());
    }

    private static void assertProfile(String family, String body, String barrel, String ammo) {
        var profile = DevBodyVisualProbeProfile.forFamily(family);
        assertEquals(body, profile.bodyModuleId());
        assertEquals(barrel, profile.barrelModuleId());
        assertEquals(ammo, profile.ammoId());
    }
}
