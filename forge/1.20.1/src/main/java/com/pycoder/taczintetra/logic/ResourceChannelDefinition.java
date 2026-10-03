package com.pycoder.taczintetra.logic;

import com.google.gson.JsonObject;

/** 资源通道元数据；外部通道仍通过可选适配器接入。 */
public record ResourceChannelDefinition(String type, boolean external) {
    public static ResourceChannelDefinition from(JsonObject object) {
        String type = "item";
        boolean external = false;
        try { if (object != null && object.has("type") && !object.get("type").getAsString().isBlank()) type = object.get("type").getAsString(); } catch (RuntimeException ignored) { }
        try { external = object.get("external").getAsBoolean(); } catch (RuntimeException ignored) { }
        return new ResourceChannelDefinition(type, external);
    }
}
