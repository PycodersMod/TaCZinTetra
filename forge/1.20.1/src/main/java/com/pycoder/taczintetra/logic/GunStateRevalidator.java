package com.pycoder.taczintetra.logic;

import java.util.List;
import java.util.Objects;

/** 在重载或模块变更后规范化已持久化的单个物品栈状态。 */
public final class GunStateRevalidator {
    private GunStateRevalidator() {
    }

    public static State revalidate(State input) {
        if (input == null) {
            return new State(0, 0, 0, 0, List.of("semi"), "semi");
        }
        int maxAmmo = Math.max(0, input.maxAmmo());
        int maxCapacity = Math.max(0, input.maxCapacity());
        int ammo = Math.max(0, Math.min(input.currentAmmo(), maxAmmo));
        int stored = Math.max(0, Math.min(input.stored(), maxCapacity));
        List<String> modes = input.fireModes() == null ? List.of() : input.fireModes().stream()
                .filter(Objects::nonNull)
                .filter(mode -> !mode.isBlank())
                .distinct()
                .toList();
        if (modes.isEmpty()) {
            modes = List.of("semi");
        }
        String mode = modes.contains(input.fireMode()) ? input.fireMode() : modes.get(0);
        return new State(maxAmmo, ammo, maxCapacity, stored, modes, mode);
    }

    public record State(int maxAmmo, int currentAmmo, int maxCapacity, int stored,
                        List<String> fireModes, String fireMode) {
    }
}
