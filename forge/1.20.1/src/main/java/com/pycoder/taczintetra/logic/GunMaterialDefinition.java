package com.pycoder.taczintetra.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 容量、热量与修理阶段使用的材料数据。 */
public record GunMaterialDefinition(List<String> physicalPartItems, double capacityMultiplier,
                                    double thermalConductivity, String repairAgent,
                                    Map<String, Double> statModifiers) {
    public static GunMaterialDefinition from(JsonObject object) {
        List<String> items = new ArrayList<>();
        try {
            if (object != null && object.get("physical_part_items").isJsonArray()) {
                for (JsonElement value : object.getAsJsonArray("physical_part_items")) {
                    if (value.isJsonPrimitive() && !value.getAsString().isBlank()) items.add(value.getAsString());
                }
            }
        } catch (RuntimeException ignored) { }
        Map<String, Double> modifiers = new java.util.LinkedHashMap<>();
        try {
            if (object != null && object.get("stat_modifiers").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("stat_modifiers").entrySet()) {
                    double value = entry.getValue().getAsDouble();
                    if (Double.isFinite(value)) modifiers.put(entry.getKey(), value);
                }
            }
        } catch (RuntimeException ignored) { }
        return new GunMaterialDefinition(List.copyOf(items), positive(object, "resource_capacity_multiplier", 1),
                positive(object, "thermal_conductivity", 1), text(object, "repair_agent", ""), Map.copyOf(modifiers));
    }

    private static String text(JsonObject o, String k, String d) { try { return o != null && o.has(k) && !o.get(k).getAsString().isBlank() ? o.get(k).getAsString() : d; } catch (RuntimeException e) { return d; } }
    private static double positive(JsonObject o, String k, double d) { try { double v=o.get(k).getAsDouble(); return Double.isFinite(v)&&v>0?v:d; } catch (RuntimeException e) { return d; } }
}
