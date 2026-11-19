package com.pycoder.taczintetra.logic;

import com.google.gson.JsonParser;
import com.tacz.guns.api.item.attachment.AttachmentType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunBodyDisplayPolicyTest {
    private static final Path DISPLAY_ROOT = Path.of(
            "src/main/resources/assets/tacz/custom/taczintetra/assets/tacz/display/guns");

    @Test
    void mapsEveryConfiguredTetraBodyToItsTaCZNativeDisplayAndKeepsPistolCompatibility() {
        assertEquals("tacz:modular_gun_display", GunBodyDisplayPolicy.displayIdForBody("pistol").toString());
        assertEquals("tacz:modular_gun_rifle_display", GunBodyDisplayPolicy.displayIdForBody("rifle").toString());
        assertEquals("tacz:modular_gun_machine_gun_display", GunBodyDisplayPolicy.displayIdForBody("machine_gun").toString());
        assertEquals("tacz:modular_gun_shotgun_display", GunBodyDisplayPolicy.displayIdForBody("shotgun").toString());
        assertEquals("tacz:modular_gun_sniper_display", GunBodyDisplayPolicy.displayIdForBody("sniper").toString());
        assertTrue(GunBodyDisplayPolicy.isManagedDisplayId(
                GunBodyDisplayPolicy.displayIdForBody("rifle")));
    }

    @Test
    void unknownOrMissingBodyUsesThePistolDisplay() {
        assertEquals("tacz:modular_gun_display", GunBodyDisplayPolicy.displayIdForBody("unknown").toString());
        assertEquals("tacz:modular_gun_display", GunBodyDisplayPolicy.displayIdForBody(null).toString());
    }

    @Test
    void mapsEveryBodyFamilyToANativeTaCZOpticForTheTetraOpticModule() {
        assertEquals("tacz:sight_acro_pistol", GunBodyDisplayPolicy.nativeOpticAttachmentIdForBody("pistol").toString());
        assertEquals("tacz:sight_552", GunBodyDisplayPolicy.nativeOpticAttachmentIdForBody("rifle").toString());
        assertEquals("tacz:scope_lpvo_1_6", GunBodyDisplayPolicy.nativeOpticAttachmentIdForBody("machine_gun").toString());
        assertEquals("tacz:sight_coyote", GunBodyDisplayPolicy.nativeOpticAttachmentIdForBody("shotgun").toString());
        assertEquals("tacz:scope_standard_8x", GunBodyDisplayPolicy.nativeOpticAttachmentIdForBody("sniper").toString());
        assertEquals("tacz:sight_acro_pistol", GunBodyDisplayPolicy.nativeOpticAttachmentIdForBody(null).toString());
    }

    @Test
    void mapsTetraComponentSlotsToNativeTaCZVisualAttachmentsByBodyFamily() {
        assertVisualAttachment("pistol", AttachmentType.SCOPE, "tacz:sight_acro_pistol");
        assertVisualAttachment("pistol", AttachmentType.MUZZLE, "tacz:muzzle_brake_pioneer");
        assertVisualAttachment("pistol", AttachmentType.GRIP, "tacz:grip_vertical_talon");
        assertVisualAttachment("pistol", AttachmentType.EXTENDED_MAG, "tacz:extended_mag_1");
        assertUnsupported("pistol", AttachmentType.STOCK);

        assertVisualAttachment("rifle", AttachmentType.SCOPE, "tacz:sight_552");
        assertVisualAttachment("rifle", AttachmentType.MUZZLE, "tacz:muzzle_brake_pioneer");
        assertVisualAttachment("rifle", AttachmentType.GRIP, "tacz:grip_vertical_ranger");
        assertVisualAttachment("rifle", AttachmentType.EXTENDED_MAG, "tacz:extended_mag_1");
        assertVisualAttachment("rifle", AttachmentType.STOCK, "tacz:stock_m4ss");

        assertVisualAttachment("machine_gun", AttachmentType.SCOPE, "tacz:scope_lpvo_1_6");
        assertVisualAttachment("machine_gun", AttachmentType.MUZZLE, "tacz:muzzle_brake_pioneer");
        assertVisualAttachment("machine_gun", AttachmentType.GRIP, "tacz:grip_cobra");
        assertVisualAttachment("machine_gun", AttachmentType.EXTENDED_MAG, "tacz:extended_mag_2");
        assertUnsupported("machine_gun", AttachmentType.STOCK);

        assertVisualAttachment("shotgun", AttachmentType.SCOPE, "tacz:sight_coyote");
        assertVisualAttachment("shotgun", AttachmentType.MUZZLE, "tacz:muzzle_choke_sg");
        assertVisualAttachment("shotgun", AttachmentType.EXTENDED_MAG, "tacz:shotgun_extended_mag_1");
        assertVisualAttachment("shotgun", AttachmentType.STOCK, "tacz:stock_heavy_spas_12");
        assertUnsupported("shotgun", AttachmentType.GRIP);

        assertVisualAttachment("sniper", AttachmentType.SCOPE, "tacz:scope_standard_8x");
        assertVisualAttachment("sniper", AttachmentType.MUZZLE, "tacz:muzzle_silencer_vulture");
        assertVisualAttachment("sniper", AttachmentType.EXTENDED_MAG, "tacz:sniper_extended_mag_1");
        assertUnsupported("sniper", AttachmentType.GRIP);
        assertUnsupported("sniper", AttachmentType.STOCK);
        assertUnsupported("rifle", AttachmentType.LASER);
    }

    private static void assertVisualAttachment(String body, AttachmentType type, String expectedId) {
        assertEquals(expectedId, GunBodyDisplayPolicy.nativeAttachmentIdForBody(body, type)
                .orElseThrow().toString(), body + "/" + type);
    }

    private static void assertUnsupported(String body, AttachmentType type) {
        assertTrue(GunBodyDisplayPolicy.nativeAttachmentIdForBody(body, type).isEmpty(),
                body + "/" + type + " has no matching native TaCZ body mount");
    }

    @Test
    void everyBodyDisplayReferencesTheVerifiedTaCZNativeModelAndTexture() throws Exception {
        Map<String, NativeVisual> visuals = Map.of(
                "pistol", new NativeVisual("modular_gun_display.json", "tacz:gun/m1911_geo", "tacz:gun/uv/m1911"),
                "rifle", new NativeVisual("modular_gun_rifle_display.json", "tacz:gun/m4a1_geo", "tacz:gun/uv/m4a1"),
                "machine_gun", new NativeVisual("modular_gun_machine_gun_display.json", "tacz:gun/m249_geo", "tacz:gun/uv/m249"),
                "shotgun", new NativeVisual("modular_gun_shotgun_display.json", "tacz:gun/m870_geo", "tacz:gun/uv/m870"),
                "sniper", new NativeVisual("modular_gun_sniper_display.json", "tacz:gun/ai_awp_geo", "tacz:gun/uv/ai_awp"));

        for (var entry : visuals.entrySet()) {
            Path file = DISPLAY_ROOT.resolve(entry.getValue().fileName());
            var display = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(entry.getValue().model(), display.get("model").getAsString(), entry.getKey());
            assertEquals(entry.getValue().texture(), display.get("texture").getAsString(), entry.getKey());
            assertTrue(display.get("show_crosshair").getAsBoolean(), entry.getKey());
            assertEquals("tacz:" + entry.getValue().fileName().replace(".json", ""),
                    GunBodyDisplayPolicy.displayIdForBody(entry.getKey()).toString(), entry.getKey());
        }
    }

    private record NativeVisual(String fileName, String model, String texture) { }
}
