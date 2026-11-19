package com.pycoder.taczintetra.logic;

import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Selects the TaCZ native gun display rig matching a Tetra gun-body family. */
public final class GunBodyDisplayPolicy {
    private static final String NAMESPACE = "tacz";
    private static final String PATH_PREFIX = "modular_gun";
    private static final Map<String, String> DISPLAY_SUFFIXES = Map.of(
            "pistol", "",
            "rifle", "_rifle",
            "machine_gun", "_machine_gun",
            "shotgun", "_shotgun",
            "sniper", "_sniper");
    private static final Map<String, Map<AttachmentType, String>> NATIVE_ATTACHMENT_PATHS = Map.of(
            "pistol", Map.of(
                    AttachmentType.SCOPE, "sight_acro_pistol",
                    AttachmentType.MUZZLE, "muzzle_brake_pioneer",
                    AttachmentType.GRIP, "grip_vertical_talon",
                    AttachmentType.EXTENDED_MAG, "extended_mag_1"),
            "rifle", Map.of(
                    AttachmentType.SCOPE, "sight_552",
                    AttachmentType.MUZZLE, "muzzle_brake_pioneer",
                    AttachmentType.GRIP, "grip_vertical_ranger",
                    AttachmentType.EXTENDED_MAG, "extended_mag_1",
                    AttachmentType.STOCK, "stock_m4ss"),
            "machine_gun", Map.of(
                    AttachmentType.SCOPE, "scope_lpvo_1_6",
                    AttachmentType.MUZZLE, "muzzle_brake_pioneer",
                    AttachmentType.GRIP, "grip_cobra",
                    AttachmentType.EXTENDED_MAG, "extended_mag_2"),
            "shotgun", Map.of(
                    AttachmentType.SCOPE, "sight_coyote",
                    AttachmentType.MUZZLE, "muzzle_choke_sg",
                    AttachmentType.EXTENDED_MAG, "shotgun_extended_mag_1",
                    AttachmentType.STOCK, "stock_heavy_spas_12"),
            "sniper", Map.of(
                    AttachmentType.SCOPE, "scope_standard_8x",
                    AttachmentType.MUZZLE, "muzzle_silencer_vulture",
                    AttachmentType.EXTENDED_MAG, "sniper_extended_mag_1"));
    private static final Set<ResourceLocation> MANAGED_DISPLAY_IDS = DISPLAY_SUFFIXES.values().stream()
            .map(suffix -> ResourceLocation.fromNamespaceAndPath(NAMESPACE, PATH_PREFIX + suffix + "_display"))
            .collect(Collectors.toUnmodifiableSet());

    private GunBodyDisplayPolicy() { }

    public static ResourceLocation displayIdForBody(String bodyVariantId) {
        String suffix = bodyVariantId == null ? "" : DISPLAY_SUFFIXES.getOrDefault(bodyVariantId, "");
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, PATH_PREFIX + suffix + "_display");
    }

    /** Selects an existing TaCZ scope visual for the current Tetra body family. */
    public static ResourceLocation nativeOpticAttachmentIdForBody(String bodyVariantId) {
        return nativeAttachmentIdForBody(bodyVariantId, AttachmentType.SCOPE)
                .orElseGet(() -> ResourceLocation.fromNamespaceAndPath(NAMESPACE, "sight_acro_pistol"));
    }

    /**
     * Selects a TaCZ-native visual attachment only where that TaCZ body model
     * provides a matching anchor. Tetra remains the source of installed modules.
     */
    public static Optional<ResourceLocation> nativeAttachmentIdForBody(String bodyVariantId,
                                                                         AttachmentType type) {
        if (type == null) return Optional.empty();
        String family = bodyVariantId == null ? "pistol"
                : bodyVariantId.substring(bodyVariantId.lastIndexOf('/') + 1);
        Map<AttachmentType, String> attachments = NATIVE_ATTACHMENT_PATHS.getOrDefault(family,
                NATIVE_ATTACHMENT_PATHS.get("pistol"));
        String path = attachments.get(type);
        return path == null ? Optional.empty()
                : Optional.of(ResourceLocation.fromNamespaceAndPath(NAMESPACE, path));
    }

    public static boolean isManagedDisplayId(ResourceLocation id) {
        return id != null && MANAGED_DISPLAY_IDS.contains(id);
    }
}
