package com.pycoder.taczintetra.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pycoder.taczintetra.logic.AmmoRecipeDefinition;
import com.pycoder.taczintetra.logic.HeatCurve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 用户可编辑且不可变的 Tetra 模块与平衡数值目录。 */
public record ModuleConfig(int schemaVersion, List<Material> materials, List<Body> bodies,
                           List<Barrel> barrels, List<Magazine> magazines,
                           List<Attachment> attachments, List<AmmoRecipeDefinition> ammoRecipes,
                           Map<String, Map<String, Double>> polish,
                           Map<String, HeatCurve> heatCurves,
                           List<ConfigEntry> enchantments, List<ConfigEntry> repairAgents,
                           List<ConfigEntry> specialInlays) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ModuleConfig {
        materials = List.copyOf(materials == null ? List.of() : materials);
        bodies = List.copyOf(bodies == null ? List.of() : bodies);
        barrels = List.copyOf(barrels == null ? List.of() : barrels);
        magazines = List.copyOf(magazines == null ? List.of() : magazines);
        attachments = List.copyOf(attachments == null ? List.of() : attachments);
        ammoRecipes = List.copyOf(ammoRecipes == null ? List.of() : ammoRecipes);
        polish = freezePolish(polish);
        heatCurves = Map.copyOf(heatCurves == null ? Map.of() : heatCurves);
        enchantments = List.copyOf(enchantments == null ? List.of() : enchantments);
        repairAgents = List.copyOf(repairAgents == null ? List.of() : repairAgents);
        specialInlays = List.copyOf(specialInlays == null ? List.of() : specialInlays);
    }

    public static ModuleConfig from(JsonObject object) {
        if (object == null) return empty();
        int schema = integer(object, "schema_version", CURRENT_SCHEMA_VERSION);
        if (schema < 1) schema = CURRENT_SCHEMA_VERSION;
        return new ModuleConfig(schema,
                parseMaterials(object.get("materials")),
                parseBodies(object.get("bodies")),
                parseBarrels(object.get("barrels")),
                parseMagazines(object.get("magazines")),
                parseAttachments(object),
                parseAmmoRecipes(object.get("ammo_recipes")),
                parsePolish(object.get("polish")),
                parseHeatCurves(object.get("heat_curves")),
                parseEntries(object.get("enchantments")),
                parseEntries(object.get("repair_agents")),
                parseEntries(object.get("special_inlays")));
    }

    public static ModuleConfig empty() {
        return new ModuleConfig(CURRENT_SCHEMA_VERSION, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());
    }

    private static List<Material> parseMaterials(JsonElement element) {
        List<Material> result = new ArrayList<>();
        for (JsonObject value : objects(element)) {
            String id = text(value, "id");
            if (!id.isEmpty()) result.add(new Material(id, text(value, "name"), positive(value, "capacity_multiplier", 1),
                    positive(value, "thermal_conductivity", 1), text(value, "repair_agent"), strings(value, "physical_part_items"), doubles(value, "stat_modifiers")));
        }
        return result;
    }

    private static List<Body> parseBodies(JsonElement element) {
        List<Body> result = new ArrayList<>();
        for (JsonObject value : objects(element)) {
            String id = text(value, "id");
            if (!id.isEmpty()) result.add(new Body(id, text(value, "name"), materials(value), text(value, "base_polish"), bool(value, "two_handed", false), doubles(value, "stats")));
        }
        return result;
    }

    private static List<Barrel> parseBarrels(JsonElement element) {
        List<Barrel> result = new ArrayList<>();
        for (JsonObject value : objects(element)) {
            String id = text(value, "id");
            if (!id.isEmpty()) result.add(new Barrel(id, text(value, "name"), materials(value), text(value, "projectile_type"), doubles(value, "stats")));
        }
        return result;
    }

    private static List<Magazine> parseMagazines(JsonElement element) {
        List<Magazine> result = new ArrayList<>();
        for (JsonObject value : objects(element)) {
            String id = text(value, "id");
            if (!id.isEmpty()) result.add(new Magazine(id, text(value, "name"), materials(value), text(value, "feed_type"), doubles(value, "stats")));
        }
        return result;
    }

    private static List<Attachment> parseAttachments(JsonObject root) {
        List<Attachment> result = new ArrayList<>();
        List<JsonObject> values = new ArrayList<>(objects(root.get("attachments")));
        for (String category : List.of("optics", "stocks", "grips")) {
            for (JsonObject value : objects(root.get(category))) {
                if (!value.has("slot")) value.addProperty("slot", category.substring(0, category.length() - 1));
                values.add(value);
            }
        }
        for (JsonObject value : values) {
            String id = text(value, "id");
            String slot = text(value, "slot");
            if (!id.isEmpty() && !slot.isEmpty()) result.add(new Attachment(id, text(value, "name"), slot, doubles(value, "stats")));
        }
        return result;
    }

    private static List<AmmoRecipeDefinition> parseAmmoRecipes(JsonElement element) {
        List<AmmoRecipeDefinition> result = new ArrayList<>();
        for (JsonObject value : objects(element)) result.add(AmmoRecipeDefinition.from(value));
        return result;
    }

    private static List<ConfigEntry> parseEntries(JsonElement element) {
        List<ConfigEntry> result = new ArrayList<>();
        for (JsonObject value : objects(element)) {
            String id = text(value, "id");
            if (!id.isEmpty()) result.add(new ConfigEntry(id, text(value, "name"), text(value, "slot"),
                    text(value, "item"), bool(value, "allowed", false), doubles(value, "stats")));
        }
        return result;
    }

    private static Map<String, Map<String, Double>> parsePolish(JsonElement element) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) return result;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            result.put(entry.getKey(), doubles(entry.getValue(), false));
        }
        return result;
    }

    private static Map<String, HeatCurve> parseHeatCurves(JsonElement element) {
        Map<String, HeatCurve> result = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) return result;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement pointsElement = entry.getValue();
            if (pointsElement.isJsonObject()) pointsElement = pointsElement.getAsJsonObject().get("points");
            if (pointsElement == null || !pointsElement.isJsonArray()) continue;
            List<HeatCurve.Point> points = new ArrayList<>();
            pointsElement.getAsJsonArray().forEach(value -> {
                if (!value.isJsonObject()) return;
                JsonObject point = value.getAsJsonObject();
                try {
                    points.add(new HeatCurve.Point(point.get("heat").getAsDouble(), point.get("value").getAsDouble()));
                } catch (RuntimeException ignored) { }
            });
            if (!points.isEmpty()) result.put(entry.getKey(), new HeatCurve(points));
        }
        return result;
    }

    private static List<JsonObject> objects(JsonElement element) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<JsonObject> result = new ArrayList<>();
        element.getAsJsonArray().forEach(value -> { if (value.isJsonObject()) result.add(value.getAsJsonObject()); });
        return result;
    }

    private static List<String> strings(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        object.getAsJsonArray(key).forEach(value -> { if (value.isJsonPrimitive() && !value.getAsString().isBlank()) result.add(value.getAsString()); });
        return result;
    }

    private static List<String> materials(JsonObject object) {
        List<String> values = strings(object, "materials");
        if (!values.isEmpty()) return values;
        String value = text(object, "material");
        return value.isEmpty() ? List.of() : List.of(value);
    }

    private static Map<String, Double> doubles(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonObject() ? doubles(object.get(key), false) : Map.of();
    }

    private static Map<String, Double> doubles(JsonElement element, boolean ignored) {
        if (element == null || !element.isJsonObject()) return Map.of();
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                double value = entry.getValue().getAsDouble();
                if (Double.isFinite(value)) result.put(entry.getKey(), value);
            } catch (RuntimeException ignoredException) { }
        }
        return result;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) { try { return object.has(key) ? object.get(key).getAsBoolean() : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static int integer(JsonObject object, String key, int fallback) { try { return object.has(key) ? object.get(key).getAsInt() : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static double positive(JsonObject object, String key, double fallback) { try { double value = object.get(key).getAsDouble(); return Double.isFinite(value) && value > 0 ? value : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static String text(JsonObject object, String key) { try { return object.has(key) && !object.get(key).getAsString().isBlank() ? object.get(key).getAsString() : ""; } catch (RuntimeException ignored) { return ""; } }

    private static Map<String, Map<String, Double>> freezePolish(Map<String, Map<String, Double>> values) {
        Map<String, Map<String, Double>> copy = new LinkedHashMap<>();
        if (values != null) values.forEach((key, value) -> copy.put(key, Map.copyOf(value == null ? Map.of() : value)));
        return Collections.unmodifiableMap(copy);
    }

    public record Material(String id, String displayName, double capacityMultiplier, double thermalConductivity,
                           String repairAgent, List<String> physicalPartItems, Map<String, Double> stats) {
        public Material { physicalPartItems = List.copyOf(physicalPartItems == null ? List.of() : physicalPartItems); stats = Map.copyOf(stats == null ? Map.of() : stats); }
    }
    public record Body(String id, String displayName, List<String> materials, String basePolish, boolean twoHanded, Map<String, Double> stats) { public Body { materials = List.copyOf(materials == null ? List.of() : materials); stats = Map.copyOf(stats == null ? Map.of() : stats); } }
    public record Barrel(String id, String displayName, List<String> materials, String projectileType, Map<String, Double> stats) { public Barrel { materials = List.copyOf(materials == null ? List.of() : materials); stats = Map.copyOf(stats == null ? Map.of() : stats); } }
    public record Magazine(String id, String displayName, List<String> materials, String feedType, Map<String, Double> stats) { public Magazine { materials = List.copyOf(materials == null ? List.of() : materials); stats = Map.copyOf(stats == null ? Map.of() : stats); } }
    public record Attachment(String id, String displayName, String slot, Map<String, Double> stats) { public Attachment { stats = Map.copyOf(stats == null ? Map.of() : stats); } }
    public record ConfigEntry(String id, String displayName, String slot, String item, boolean allowed,
                              Map<String, Double> stats) { public ConfigEntry { stats = Map.copyOf(stats == null ? Map.of() : stats); } }
}
