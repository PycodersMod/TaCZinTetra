package com.pycoder.taczintetra.logic;

import com.google.gson.JsonObject;

/** 修复代理身份及单次消耗量。 */
public record RepairAgentDefinition(String item, int unitCost) {
    public static RepairAgentDefinition from(JsonObject object) {
        String item = "";
        int cost = 1;
        try { if (object != null && object.has("item") && !object.get("item").getAsString().isBlank()) item = object.get("item").getAsString(); } catch (RuntimeException ignored) { }
        try { int value = object.get("unit_cost").getAsInt(); if (value > 0) cost = value; } catch (RuntimeException ignored) { }
        return new RepairAgentDefinition(item, cost);
    }
}
