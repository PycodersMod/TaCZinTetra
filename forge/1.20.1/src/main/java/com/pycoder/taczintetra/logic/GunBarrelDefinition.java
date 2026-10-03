package com.pycoder.taczintetra.logic;

import com.google.gson.JsonObject;

public record GunBarrelDefinition(String projectileType, int pelletsPerRound, double damage,
                                  double velocity, double gravity, double friction, double range,
                                  double armorIgnore, int pierce, double knockback, int ignite,
                                  double explosion, double heat) {
    public static GunBarrelDefinition from(JsonObject object) {
        return new GunBarrelDefinition(
                text(object, "projectile_type", "tacz:9mm"),
                positiveInt(object, "pellets_per_round", 1),
                positiveDouble(object, "damage", 1), positiveDouble(object, "velocity", 1),
                positiveDouble(object, "gravity", 1), positiveDouble(object, "friction", 1),
                positiveDouble(object, "range", 1), nonNegative(object, "armor_ignore"),
                nonNegativeInt(object, "pierce"), nonNegative(object, "knockback"),
                nonNegativeInt(object, "ignite"), nonNegative(object, "explosion"),
                nonNegative(object, "heat"));
    }

    private static String text(JsonObject o, String k, String d) { try { return o != null && o.has(k) && !o.get(k).getAsString().isBlank() ? o.get(k).getAsString() : d; } catch (RuntimeException e) { return d; } }
    private static int positiveInt(JsonObject o, String k, int d) { try { int v=o.get(k).getAsInt(); return v>0?v:d; } catch (RuntimeException e) { return d; } }
    private static int nonNegativeInt(JsonObject o, String k) { try { return Math.max(0,o.get(k).getAsInt()); } catch (RuntimeException e) { return 0; } }
    private static double positiveDouble(JsonObject o, String k, double d) { try { double v=o.get(k).getAsDouble(); return Double.isFinite(v)&&v>0?v:d; } catch (RuntimeException e) { return d; } }
    private static double nonNegative(JsonObject o, String k) { try { double v=o.get(k).getAsDouble(); return Double.isFinite(v)&&v>=0?v:0; } catch (RuntimeException e) { return 0; } }
}
