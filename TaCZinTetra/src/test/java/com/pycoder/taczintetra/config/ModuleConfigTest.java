package com.pycoder.taczintetra.config;

import com.google.gson.JsonParser;
import com.pycoder.taczintetra.logic.GunProfileResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void createsAndLoadsDefaultConfigInConfigDirectory() throws Exception {
        ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);

        assertTrue(Files.exists(tempDir.resolve("taczintetra.json")));
        assertEquals(5, config.materials().size());
        assertEquals(5, config.bodies().size());
        assertTrue(config.bodies().stream().filter(value -> value.id().equals("pistol")).findFirst().orElseThrow().twoHanded() == false);
        assertTrue(config.bodies().stream().filter(value -> value.id().equals("rifle")).findFirst().orElseThrow().twoHanded());
        assertTrue(config.barrels().stream().anyMatch(value -> value.id().equals("12g")));
        assertTrue(config.barrels().stream().anyMatch(value -> value.id().equals("40mm")));
        ModuleConfig.Barrel barrel9mm = config.barrels().stream().filter(value -> value.id().equals("9mm")).findFirst().orElseThrow();
        assertEquals(2.0, barrel9mm.stats().get("heat_per_shot"));
        assertEquals(4.0, barrel9mm.stats().get("cooling_coefficient"));
        assertEquals(1.0, barrel9mm.stats().get("range"));
        assertEquals(0.0, barrel9mm.stats().get("armor_ignore"));
        assertEquals(0.0, barrel9mm.stats().get("pierce"));
        assertEquals(1.0, barrel9mm.stats().get("knockback"));
        assertEquals(7, config.ammoRecipes().size());
        assertTrue(config.ammoRecipes().stream().anyMatch(value -> value.projectile().equals("tacz:9mm")));
        assertEquals(Set.of("optic", "stock", "grip"), config.attachments().stream().map(ModuleConfig.Attachment::id).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1.02, config.polish().get("honing").get("honing_count_multiplier"));
        assertEquals(1.2, config.heatCurves().get("inaccuracy_multiplier").valueAt(1), 0.0001);
    }

    @Test
    void parsesUserRequestedDefaultCatalogWithoutJavaHardcodedStats() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "materials": [{"id":"wood","name":"木","capacity_multiplier":1.0,"physical_part_items":["taczintetra:wood_body","taczintetra:wood_barrel","taczintetra:wood_magazine"]}],
                  "bodies": [{"id":"pistol","name":"手枪","material":"wood","base_polish":"honing"}],
                  "barrels": [{"id":"9mm","name":"9mm","material":"wood","projectile_type":"tacz:9mm"}],
                  "magazines": [{"id":"standard","name":"常规弹匣","material":"wood","feed_type":"magazine"}],
                  "attachments": [
                    {"id":"optic","name":"倍镜","slot":"optic"},
                    {"id":"stock","name":"枪托","slot":"stock"},
                    {"id":"grip","name":"握把","slot":"grip"}
                  ],
                  "polish": {"honing": {"damage_multiplier": 1.25}},
                  "heat_curves": {"rpm_multiplier": {"points":[{"heat":0,"value":1},{"heat":0.5,"value":0.8},{"heat":1,"value":0.6}]}}
                }
                """).getAsJsonObject());

        assertEquals(1, config.schemaVersion());
        assertEquals(List.of("wood"), config.materials().stream().map(ModuleConfig.Material::id).toList());
        assertEquals(List.of("taczintetra:wood_body", "taczintetra:wood_barrel", "taczintetra:wood_magazine"),
                config.materials().get(0).physicalPartItems());
        assertEquals("手枪", config.bodies().get(0).displayName());
        assertEquals(List.of("wood"), config.bodies().get(0).materials());
        assertEquals("tacz:9mm", config.barrels().get(0).projectileType());
        assertEquals("握把", config.attachments().get(2).displayName());
        assertEquals(1.25, config.polish().get("honing").get("damage_multiplier"));
        assertEquals(0.8, config.heatCurves().get("rpm_multiplier").valueAt(0.5), 0.0001);
    }

    @Test
    void invalidOrMissingCollectionsBecomeEmptyAndSchemaIsSafe() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("{\"schema_version\":-4,\"materials\":null}").getAsJsonObject());

        assertEquals(1, config.schemaVersion());
        assertTrue(config.materials().isEmpty());
        assertTrue(config.bodies().isEmpty());
        assertTrue(config.barrels().isEmpty());
        assertTrue(config.magazines().isEmpty());
        assertTrue(config.attachments().isEmpty());
    }

    @Test
    void bodyTwoHandedFlagDefaultsToFalseForOlderConfigs() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString(
                "{\"bodies\":[{\"id\":\"legacy\"}]}" ).getAsJsonObject());
        assertTrue(!config.bodies().get(0).twoHanded());
    }

    @Test
    void everyDefaultMaterialProvidesThreeMainPartInputs() {
        ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);

        assertEquals(5, config.materials().size());
        config.materials().forEach(material -> assertEquals(3, material.physicalPartItems().size(), material.id()));
    }

    @Test
    void defaultCatalogContainsTheCompleteRequestedModuleMatrix() {
        ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);
        Set<String> materials = config.materials().stream().map(ModuleConfig.Material::id).collect(java.util.stream.Collectors.toSet());
        Set<String> bodies = config.bodies().stream().map(ModuleConfig.Body::id).collect(java.util.stream.Collectors.toSet());
        Set<String> barrels = config.barrels().stream().map(ModuleConfig.Barrel::id).collect(java.util.stream.Collectors.toSet());
        Set<String> magazines = config.magazines().stream().map(ModuleConfig.Magazine::id).collect(java.util.stream.Collectors.toSet());
        Set<String> attachments = config.attachments().stream().map(ModuleConfig.Attachment::id).collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("wood", "stone", "iron", "gold", "netherite"), materials);
        assertEquals(Set.of("pistol", "rifle", "machine_gun", "sniper", "shotgun"), bodies);
        assertEquals(Set.of("9mm", "45acp", "556", "762", "338", "12g", "40mm"), barrels);
        assertEquals(Set.of("standard"), magazines);
        assertEquals(Set.of("optic", "stock", "grip"), attachments);

        config.bodies().forEach(body -> assertEquals(materials, Set.copyOf(body.materials()), body.id()));
        config.barrels().forEach(barrel -> assertEquals(materials, Set.copyOf(barrel.materials()), barrel.id()));
        config.magazines().forEach(magazine -> assertEquals(materials, Set.copyOf(magazine.materials()), magazine.id()));
    }

    @Test
      void defaultCatalogResolvesEveryBodyBarrelMagazineCombination() {
          ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);
          config.materials().forEach(material -> config.bodies().forEach(body -> config.barrels().forEach(barrel -> {
              GunProfileResolver.ResolvedGunProfile profile = ModuleConfigProfileResolver.resolve(
                      config, body.id(), barrel.id(), "standard", material.id());
              String combination = material.id() + "/" + body.id() + "/" + barrel.id();
              assertTrue(Double.isFinite(profile.projectile().damage()), combination);
              assertTrue(profile.projectile().velocity() > 0, combination);
              assertTrue(profile.projectile().range() > 0, combination);
              assertTrue(profile.projectile().armorIgnore() >= 0
                      && profile.projectile().armorIgnore() <= 1, combination);
              assertTrue(profile.projectile().pierce() >= 0, combination);
              assertTrue(Double.isFinite(profile.projectile().knockback())
                      && profile.projectile().knockback() > 0, combination);
              assertTrue(Double.isFinite(profile.handling().accuracyAdd()), combination);
              assertTrue(Double.isFinite(profile.handling().recoilAdd()), combination);
              assertTrue(profile.fireControl().roundsPerMinute() > 0, combination);
              assertTrue(profile.feed().loadedCapacity() > 0, combination);
          })));
      }

    @Test
    void migratesOlderConfigByAddingOnlyMissingDefaultCollections() throws Exception {
        Path file = tempDir.resolve("taczintetra/modules.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"schema_version\":1,\"ammo_recipes\":[]}");

        ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);

        assertEquals(0, config.ammoRecipes().size());
        assertTrue(config.materials().size() > 0);
        assertTrue(Files.exists(tempDir.resolve("taczintetra.json")));
        assertTrue(Files.exists(tempDir.resolve("taczintetra.json.legacy.bak")));
    }

    @Test
    void migratesMissingNestedPolishFieldsWithoutOverwritingUserValues() throws Exception {
        Path file = tempDir.resolve("taczintetra.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"polish\":{\"honing\":{\"damage_multiplier\":2.0}}}");

        ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);

        assertEquals(2.0, config.polish().get("honing").get("damage_multiplier"));
        assertEquals(1.02, config.polish().get("honing").get("honing_count_multiplier"));
    }

    @Test
    void explicitEmptyCatalogArraysAreNotRepopulatedAfterUserDeletion() throws Exception {
        Path file = tempDir.resolve("taczintetra.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {"schema_version":1,"materials":[],"bodies":[],"barrels":[],"magazines":[],
                 "optics":[],"stocks":[],"grips":[],"enchantments":[],"repair_agents":[],"special_inlays":[]}
                """);

        ModuleConfig config = ModuleConfigManager.loadOrCreate(tempDir);

        assertTrue(config.materials().isEmpty());
        assertTrue(config.bodies().isEmpty());
        assertTrue(config.barrels().isEmpty());
        assertTrue(config.magazines().isEmpty());
    }

    @Test
    void resourceReloadRefreshesEditableCatalogBeforeProfileCacheUse() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/data/DefinitionReloadListener.java"));
        assertTrue(source.contains("ModuleConfigManager.loadOrCreate"));
        assertTrue(source.contains("GunProfileCache.clear()"));
    }
}
