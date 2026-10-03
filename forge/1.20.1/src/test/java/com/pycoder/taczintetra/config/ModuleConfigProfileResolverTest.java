package com.pycoder.taczintetra.config;

import com.google.gson.JsonParser;
import com.pycoder.taczintetra.logic.GunProfileResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;

class ModuleConfigProfileResolverTest {
    @Test
    void selectedMajorModuleMaterialsMustRespectEachModelAllowList() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"materials":[{"id":"wood"},{"id":"iron"}],
                 "bodies":[{"id":"pistol","materials":["wood"]}],
                 "barrels":[{"id":"9mm","materials":["iron"]}],
                 "magazines":[{"id":"standard","materials":["wood","iron"]}]}
                """).getAsJsonObject());

        assertTrue(ModuleConfigProfileResolver.isSelectionValid(
                config, "pistol", "9mm", "standard", "wood", "iron", "wood"));
        assertTrue(!ModuleConfigProfileResolver.isSelectionValid(
                config, "pistol", "9mm", "standard", "iron", "iron", "wood"));
    }

    @Test
    void resolvesConfiguredStatsWithoutEmbeddingBalanceValuesInCode() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"bodies":[{"id":"pistol","stats":{"damage_add":4,"rpm_add":200}}],
                 "barrels":[{"id":"9mm","stats":{"damage_multiplier":1.5}}],
                 "magazines":[{"id":"standard","stats":{"capacity_multiplier":2}}]}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(config, "pistol", "9mm", "standard");
        assertEquals(6.0, profile.projectile().damage());
        assertEquals(200, profile.fireControl().roundsPerMinute());
        assertEquals(2, profile.feed().loadedCapacity());
        assertEquals(2, profile.feed().resourceCapacity());
    }

    @Test
    void resourceCapacityUsesMaterialOnceWhileLoadedCapacityUsesBodyMultiplier() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"materials":[{"id":"gold","capacity_multiplier":1.5}],
                 "bodies":[{"id":"machine","stats":{"loaded_capacity_multiplier":2}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{"resource_base_capacity":10}}]}
                """).getAsJsonObject());
        GunProfileResolver.ResolvedGunProfile profile =
                ModuleConfigProfileResolver.resolve(config, "machine", "9mm", "standard", "gold");
        assertEquals(20, profile.feed().loadedCapacity());
        assertEquals(15, profile.feed().resourceCapacity());
    }

    @Test
    void resolvesIndependentMaterialForEachMajorModule() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"materials":[
                   {"id":"wood","capacity_multiplier":0.8,"stat_modifiers":{"weight":0.8}},
                   {"id":"iron","capacity_multiplier":1.0,"stat_modifiers":{"weight":1.1}},
                   {"id":"gold","capacity_multiplier":1.5,"stat_modifiers":{"weight":0.9}}],
                 "bodies":[{"id":"pistol","stats":{"loaded_capacity_multiplier":1.0}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{"resource_base_capacity":10}}]}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", "wood", "iron", "gold", 0);

        assertEquals(10, profile.feed().loadedCapacity());
        assertEquals(15, profile.feed().resourceCapacity());
        assertEquals(0.8 * 1.1 * 0.9, profile.handling().weightMultiplier(), 0.000001);
    }

    @Test
    void configuredAttachmentsModifyOnlyTheirSelectedProfileDomains() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"attachments":[
                  {"id":"optic","slot":"optic","stats":{"aiming_zoom":2.0}},
                  {"id":"stock","slot":"stock","stats":{"recoil_multiplier":0.5}},
                  {"id":"grip","slot":"grip","stats":{"handling_multiplier":2.0}}]}
                """).getAsJsonObject());
        GunProfileResolver.ResolvedGunProfile base = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(0, 0, 1, 1, 1, 4, 1, 1), null, null);

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.applyAttachments(
                config, base, true, true, true);

        assertEquals(2.0, profile.handling().accuracyAdd());
        assertEquals(2.0, profile.handling().recoilAdd());
        assertEquals(2.0, profile.visual().aimingZoom());
        assertEquals(1, profile.feed().loadedCapacity());
    }

    @Test
    void selectedAttachmentIdMustChooseMatchingEntryInsteadOfFirstEntryInSlot() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"attachments":[
                  {"id":"light_stock","slot":"stock","stats":{"recoil_multiplier":0.9}},
                  {"id":"heavy_stock","slot":"stock","stats":{"recoil_multiplier":0.4}}]}
                """).getAsJsonObject());
        GunProfileResolver.ResolvedGunProfile base = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(0, 0, 1, 1, 0, 10, 1, 1), null, null);

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.applyAttachments(
                config, base, "heavy_stock", null, null);

        assertEquals(4.0, profile.handling().recoilAdd());
    }

    @Test
    void missingOpticDoesNotReceiveConfiguredOpticZoom() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"attachments":[{"id":"optic","slot":"optic","stats":{"aiming_zoom":2.0}}]}
                """).getAsJsonObject());
        GunProfileResolver.ResolvedGunProfile base = GunProfileResolver.resolveProfiles(null, null, null);
        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.applyAttachments(
                config, base, false, false, false);
        assertEquals(1.0, profile.visual().aimingZoom());
    }

    @Test
    void bodyBasePolishAppliesConfiguredMultipliers() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"bodies":[{"id":"pistol","base_polish":"honing","stats":{"damage_add":10,"rpm_add":100}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{}}],
                 "polish":{"honing":{"damage_multiplier":1.5,"rpm_multiplier":1.2}}}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", null, 1);

        assertEquals(15.0, profile.projectile().damage());
        assertEquals(120, profile.fireControl().roundsPerMinute());
    }

    @Test
    void zeroHoningDoesNotApplyConfiguredPolishBonus() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"bodies":[{"id":"pistol","base_polish":"honing","stats":{"damage_add":10,"rpm_add":100}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{}}],
                 "polish":{"honing":{"damage_multiplier":1.5,"rpm_multiplier":1.2}}}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", null, 0);

        assertEquals(10.0, profile.projectile().damage());
        assertEquals(100, profile.fireControl().roundsPerMinute());
    }

    @Test
    void honingCountUsesConfiguredPerHoneMultiplier() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"bodies":[{"id":"pistol","base_polish":"honing","stats":{"damage_add":10,"rpm_add":100}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{}}],
                 "polish":{"honing":{"damage_multiplier":1.0,"rpm_multiplier":1.0,"honing_count_multiplier":1.1}}}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", null, 2);

        assertEquals(12.1, profile.projectile().damage(), 0.0001);
        assertEquals(121, profile.fireControl().roundsPerMinute());
    }

    @Test
    void invalidPolishMultipliersFallBackToNeutralValues() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"bodies":[{"id":"pistol","base_polish":"honing","stats":{"damage_add":10,"rpm_add":100}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{}}],
                 "polish":{"honing":{"damage_multiplier":-2,"rpm_multiplier":0,"honing_count_multiplier":-1}}}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", null, 2);

        assertEquals(10.0, profile.projectile().damage());
        assertEquals(100, profile.fireControl().roundsPerMinute());
    }

    @Test
    void overflowingHoningMultiplierFallsBackToFiniteProfile() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"bodies":[{"id":"pistol","base_polish":"honing","stats":{"damage_add":10,"rpm_add":100}}],
                 "barrels":[{"id":"9mm","stats":{}}],
                 "magazines":[{"id":"standard","stats":{}}],
                 "polish":{"honing":{"honing_count_multiplier":1000000}}}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", null, 1000);

        assertTrue(Double.isFinite(profile.projectile().damage()));
        assertTrue(Double.isFinite(profile.fireControl().roundsPerMinute()));
    }

    @Test
    void weightAndReloadTimeCanBeAddedByJsonWithoutJavaChanges() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"materials":[{"id":"iron","capacity_multiplier":1.0,"stat_modifiers":{"weight":1.1}}],
                 "bodies":[{"id":"pistol","materials":["iron"],"base_polish":"honing","stats":{"weight_multiplier":1.5,"reload_time_multiplier":1.2}}],
                 "barrels":[{"id":"9mm","stats":{"weight_multiplier":2.0,"reload_time_multiplier":0.5}}],
                 "magazines":[{"id":"standard","stats":{}}],
                 "polish":{"honing":{"reload_time_multiplier":0.8}}}
                """).getAsJsonObject());

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                config, "pistol", "9mm", "standard", "iron", 1);

        assertEquals(3.3, profile.handling().weightMultiplier(), 0.000001);
        assertEquals(0.48, profile.handling().reloadTimeMultiplier(), 0.000001);
    }

    @Test
    void allowedSpecialInlayStatsModifyProfileWithoutHardcodedBalanceValues() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"special_inlays":[
                  {"id":"socket","allowed":true,"stats":{"capacity":1}},
                  {"id":"tracer","allowed":true,"stats":{"damage_multiplier":1.2,"rpm_multiplier":1.1,"accuracy_add":0.25}}
                ]}
                """).getAsJsonObject());
        GunProfileResolver.ResolvedGunProfile base = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(10, 100, 1, 1, 0, 0, 1, 1), null, null);

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.applySpecialInlays(
                config, base, List.of("tracer"));

        assertEquals(12.0, profile.projectile().damage());
        assertEquals(110, profile.fireControl().roundsPerMinute());
        assertEquals(0.25, profile.handling().accuracyAdd());
    }

    @Test
    void specialInlayProjectileStatsRemainDataDrivenForRangeArmorAndPierce() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"special_inlays":[
                  {"id":"socket","allowed":true,"stats":{"capacity":1}},
                  {"id":"armor_piercer","allowed":true,"stats":{
                    "range_multiplier":1.25,"armor_ignore_add":0.4,"pierce_add":2}}
                ]}
                """).getAsJsonObject());
        GunProfileResolver.ResolvedGunProfile base = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(10, 100, 1, 1, 0, 0, 1, 1),
                new GunProfileResolver.PartStats(5, 100, 1, 1, 0, 1.0, 1, 1), null);

        GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.applySpecialInlays(
                config, base, List.of("armor_piercer"));

        assertEquals(1.25, profile.projectile().range());
        assertEquals(0.4, profile.projectile().armorIgnore());
        assertEquals(2, profile.projectile().pierce());
    }
}
