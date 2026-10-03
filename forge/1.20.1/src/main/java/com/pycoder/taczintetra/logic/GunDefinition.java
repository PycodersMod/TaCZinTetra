package com.pycoder.taczintetra.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 安全的数据驱动枪械主体定义，独立于 Forge 重载流程解析。 */
public record GunDefinition(String weaponClass, List<String> fireModes, int shotsPerTrigger,
                            double loadedCapacityMultiplier, int roundsPerMinute,
                            boolean dualWield, String heatId) {
    public static GunDefinition from(JsonObject object) {
        if (object == null) {
            return defaults();
        }
        return new GunDefinition(
                text(object, "weapon_class", "pistol"),
                modes(object.get("fire_modes")),
                positiveInt(object, "shots_per_trigger", 1),
                positiveDouble(object, "loaded_capacity_multiplier", 1.0),
                positiveInt(object, "rounds_per_minute", 300),
                bool(object, "dual_wield", false),
                text(object, "heat", "taczintetra:standard"));
    }

    private static GunDefinition defaults() {
        return new GunDefinition("pistol", List.of("semi"), 1, 1.0, 300, false,
                "taczintetra:standard");
    }

    private static List<String> modes(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of("semi");
        }
        JsonArray array = element.getAsJsonArray();
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement mode : array) {
            if (mode.isJsonPrimitive() && !mode.getAsString().isBlank()) {
                result.add(mode.getAsString());
            }
        }
        return result.isEmpty() ? List.of("semi") : List.copyOf(result);
    }

    private static String text(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && !value.getAsString().isBlank()
                ? value.getAsString() : fallback;
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        try {
            int value = object.get(key).getAsInt();
            return value > 0 ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double positiveDouble(JsonObject object, String key, double fallback) {
        try {
            double value = object.get(key).getAsDouble();
            return Double.isFinite(value) && value > 0 ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
