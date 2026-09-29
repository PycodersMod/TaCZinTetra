package com.pycoder.taczintetra.logic;

import com.google.gson.JsonObject;

/** Resource channel metadata; external channels remain optional adapters. */
public record ResourceChannelDefinition(String type, boolean external) {
    public static ResourceChannelDefinition from(JsonObject object) {
        String type = "item";
        boolean external = false;
        try { if (object != null && object.has("type") && !object.get("type").getAsString().isBlank()) type = object.get("type").getAsString(); } catch (RuntimeException ignored) { }
        try { external = object.get("external").getAsBoolean(); } catch (RuntimeException ignored) { }
        return new ResourceChannelDefinition(type, external);
    }
}
