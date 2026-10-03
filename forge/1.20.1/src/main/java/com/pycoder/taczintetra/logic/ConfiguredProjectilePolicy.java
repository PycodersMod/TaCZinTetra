package com.pycoder.taczintetra.logic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.tacz.guns.resource.pojo.data.gun.BulletData;

import java.util.Map;

/** 解析必须应用于原生实体的弹丸专属行为。 */
public final class ConfiguredProjectilePolicy {
    private static final Gson GSON = new Gson();

    private ConfiguredProjectilePolicy() { }

    public static float explosionRadius(ModuleConfig config, String barrelId) {
        if (config == null || barrelId == null || barrelId.isBlank()) return 0.0f;
        return config.barrels().stream()
                .filter(barrel -> barrelId.equals(barrel.id()))
                .map(barrel -> barrel.stats().get("explosion"))
                .filter(value -> value != null && Double.isFinite(value) && value > 0)
                .map(Double::floatValue)
                .findFirst()
                .orElse(0.0f);
    }

    /** 对原生子弹运动字段的可选绝对值覆盖项。 */
    public static MotionOverrides motionOverrides(ModuleConfig config, String barrelId) {
        if (config == null || barrelId == null || barrelId.isBlank()) {
            return MotionOverrides.empty();
        }
        Map<String, Double> stats = config.barrels().stream()
                .filter(barrel -> barrelId.equals(barrel.id()))
                .map(ModuleConfig.Barrel::stats)
                .findFirst().orElse(Map.of());
        return new MotionOverrides(optionalNonNegative(stats, "life"),
                optionalNonNegative(stats, "gravity"),
                optionalNonNegative(stats, "friction"),
                optionalPositive(stats, "ignite"));
    }

    /** 创建新的原生子弹快照，不修改 TaCZ 已注册的模板。 */
    public static BulletData nativeBulletData(ModuleConfig config, String barrelId, BulletData template) {
        if (template == null || config == null || barrelId == null || barrelId.isBlank()) return template;
        Map<String, Double> stats = config.barrels().stream()
                .filter(barrel -> barrelId.equals(barrel.id()))
                .map(ModuleConfig.Barrel::stats)
                .findFirst().orElse(Map.of());
        if (stats.isEmpty()) return template;
        JsonObject object = GSON.toJsonTree(template).getAsJsonObject();
        setPositive(object, stats, "life");
        setPositive(object, stats, "damage");
        setNonNegative(object, stats, "gravity");
        setNonNegative(object, stats, "friction");
        setNonNegative(object, stats, "knockback");
        setNonNegativeInt(object, stats, "pierce");
        setNonNegativeInt(object, stats, "bullet_amount");
        if (optionalPositive(stats, "ignite")) {
            JsonObject ignite = object.has("ignite") && object.get("ignite").isJsonObject()
                    ? object.getAsJsonObject("ignite") : new JsonObject();
            ignite.addProperty("entity", true);
            ignite.addProperty("block", true);
            object.add("ignite", ignite);
        }
        return GSON.fromJson(object, BulletData.class);
    }

    private static void setPositive(JsonObject object, Map<String, Double> stats, String key) {
        Double value = stats.get(key);
        if (value != null && Double.isFinite(value) && value > 0) object.addProperty(key, value);
    }

    private static void setNonNegative(JsonObject object, Map<String, Double> stats, String key) {
        Double value = stats.get(key);
        if (value != null && Double.isFinite(value) && value >= 0) object.addProperty(key, value);
    }

    private static void setNonNegativeInt(JsonObject object, Map<String, Double> stats, String key) {
        Double value = stats.get(key);
        if (value != null && Double.isFinite(value) && value >= 0) {
            object.addProperty(key, Math.max(0, (int) Math.round(value)));
        }
    }

    private static float optionalNonNegative(Map<String, Double> stats, String key) {
        Double value = stats.get(key);
        return value != null && Double.isFinite(value) && value >= 0
                ? value.floatValue() : Float.NaN;
    }

    private static boolean optionalPositive(Map<String, Double> stats, String key) {
        Double value = stats.get(key);
        return value != null && Double.isFinite(value) && value > 0;
    }

    public record MotionOverrides(float lifeSeconds, float gravity, float friction, boolean ignite) {
        public static MotionOverrides empty() {
            return new MotionOverrides(Float.NaN, Float.NaN, Float.NaN, false);
        }
    }
}
