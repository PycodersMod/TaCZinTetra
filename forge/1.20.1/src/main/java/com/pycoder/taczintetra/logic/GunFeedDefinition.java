package com.pycoder.taczintetra.logic;

import com.google.gson.JsonObject;

public record GunFeedDefinition(String feedType, int resourceBaseCapacity,
                                double reloadTimeMultiplier) {
    public static GunFeedDefinition from(JsonObject object) {
        String type = "magazine";
        try { if (object != null && object.has("feed_type") && !object.get("feed_type").getAsString().isBlank()) type = object.get("feed_type").getAsString(); } catch (RuntimeException ignored) { }
        int capacity = 1;
        double reload = 1;
        try { int v=object.get("resource_base_capacity").getAsInt(); if(v>0) capacity=v; } catch (RuntimeException ignored) { }
        try { double v=object.get("reload_time_multiplier").getAsDouble(); if(Double.isFinite(v)&&v>0) reload=v; } catch (RuntimeException ignored) { }
        return new GunFeedDefinition(type, capacity, reload);
    }
}
