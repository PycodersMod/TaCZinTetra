package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.ModuleConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Checks Tetra workbench materials against the user-editable semi-finished catalog. */
public final class SemiFinishedPartPolicy {
    private SemiFinishedPartPolicy() {
    }

    public static boolean accepts(ItemStack stack) {
        return accepts(stack, null);
    }

    public static boolean accepts(ItemStack stack, String slot) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ModuleConfig config = TaCZinTetra.MODULE_CONFIG;
        if (config == null) {
            return false;
        }
        boolean semiFinishedPart = config.materials().stream()
                .flatMap(material -> material.physicalPartItems().stream())
                .anyMatch(candidate -> candidate.equals(id) && matchesSlot(candidate, slot));
        if (semiFinishedPart) return true;
        return config.repairAgents().stream()
                .map(ModuleConfig.ConfigEntry::item)
                .anyMatch(id::equals);
    }

    /** Keeps the three configured main-part item identities bound to their Tetra slot. */
    public static boolean matchesSlot(String itemId, String slot) {
        if (itemId == null || slot == null || slot.isBlank()) return true;
        String normalizedSlot = slot.replace('\\', '/');
        int slash = normalizedSlot.lastIndexOf('/');
        normalizedSlot = slash >= 0 ? normalizedSlot.substring(slash + 1) : normalizedSlot;
        if (!isMainPartSlot(normalizedSlot)) return true;
        return itemId.endsWith("_" + normalizedSlot);
    }

    private static boolean isMainPartSlot(String slot) {
        return "body".equals(slot) || "barrel".equals(slot) || "magazine".equals(slot);
    }
}
