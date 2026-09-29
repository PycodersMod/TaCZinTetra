package com.pycoder.taczintetra.config;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialInlayPolicyTest {
    @Test
    void readsOnlyAllowedInlaysAndRespectsSocketCapacity() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"special_inlays":[
                  {"id":"socket","allowed":true,"stats":{"capacity":2}},
                  {"id":"taczintetra:tracer","allowed":true,"stats":{"cost":1}},
                  {"id":"taczintetra:overpressure","allowed":true,"stats":{"cost":2}},
                  {"id":"taczintetra:disabled","allowed":false,"stats":{"cost":1}}
                ]}
                """).getAsJsonObject());
        CompoundTag tag = new CompoundTag();
        SpecialInlayPolicy.write(tag, List.of("taczintetra:tracer", "taczintetra:overpressure", "taczintetra:disabled"));

        var result = SpecialInlayPolicy.resolve(tag, config);

        assertEquals(Set.of("taczintetra:tracer"), result.activeIds());
        assertEquals(Set.of("taczintetra:overpressure", "taczintetra:disabled"), result.rejectedIds());
        assertEquals(2, result.capacity());
        assertEquals(1, result.usedCapacity());
    }

    @Test
    void malformedAndDuplicateNbtEntriesDoNotBypassCapacity() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"special_inlays":[
                  {"id":"socket","allowed":true,"stats":{"capacity":1}},
                  {"id":"a","allowed":true,"stats":{"cost":1}},
                  {"id":"b","allowed":true,"stats":{"cost":1}}
                ]}
                """).getAsJsonObject());
        CompoundTag tag = new CompoundTag();
        SpecialInlayPolicy.write(tag, List.of("a", "a", "b", "", "not-an-id"));

        var result = SpecialInlayPolicy.resolve(tag, config);

        assertEquals(Set.of("a"), result.activeIds());
        assertTrue(result.rejectedIds().contains("b"));
        assertEquals(1, result.usedCapacity());
    }

    @Test
    void specialInlayEntryCapacityDoesNotCreateExtraSockets() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"special_inlays":[
                  {"id":"socket","allowed":true,"stats":{"capacity":1}},
                  {"id":"trait","slot":"special","allowed":true,"stats":{"capacity":99}}
                ]}
                """).getAsJsonObject());

        var result = SpecialInlayPolicy.resolve(List.of("trait"), config);

        assertEquals(1, result.capacity());
        assertEquals(Set.of("trait"), result.activeIds());
    }

    @Test
    void explicitInlayNbtRequiresAnInstalledSpecialSocket() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"special_inlays":[
                  {"id":"socket","allowed":true,"stats":{"capacity":1}},
                  {"id":"tracer","allowed":true,"stats":{"cost":1}}
                ]}
                """).getAsJsonObject());

        var withoutSocket = SpecialInlayPolicy.resolve(List.of("tracer"), config, false);
        var withSocket = SpecialInlayPolicy.resolve(List.of("tracer"), config, true);

        assertTrue(withoutSocket.activeIds().isEmpty());
        assertTrue(withoutSocket.rejectedIds().contains("tracer"));
        assertEquals(Set.of("tracer"), withSocket.activeIds());
    }
}
