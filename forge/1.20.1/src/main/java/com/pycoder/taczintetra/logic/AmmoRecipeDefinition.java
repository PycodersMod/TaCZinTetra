package com.pycoder.taczintetra.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Data-driven projectile and per-batch resource cost. */
public record AmmoRecipeDefinition(String projectile, String feedType, int batchSize, Map<String, Integer> cost) {
    public AmmoRecipeDefinition(String projectile, int batchSize, Map<String, Integer> cost) {
        this(projectile, "magazine", batchSize, cost);
    }

    public static AmmoRecipeDefinition from(JsonObject object) {
        String projectile = text(object, "projectile", "tacz:9mm");
        String feedType = text(object, "feed_type", "magazine");
        int batch = positiveInt(object, "batch_size", 1);
        Map<String, Integer> costs = new LinkedHashMap<>();
        try {
            if (object != null && object.get("cost").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("cost").entrySet()) {
                    int amount = entry.getValue().getAsInt();
                    if (amount > 0) costs.put(entry.getKey(), amount);
                }
            }
        } catch (RuntimeException ignored) { }
        return new AmmoRecipeDefinition(projectile, feedType, batch, Map.copyOf(costs));
    }
    private static String text(JsonObject o, String k, String d) { try { return o != null && o.has(k) && !o.get(k).getAsString().isBlank() ? o.get(k).getAsString() : d; } catch (RuntimeException e) { return d; } }
    private static int positiveInt(JsonObject o, String k, int d) { try { int v=o.get(k).getAsInt(); return v>0?v:d; } catch (RuntimeException e) { return d; } }
}
