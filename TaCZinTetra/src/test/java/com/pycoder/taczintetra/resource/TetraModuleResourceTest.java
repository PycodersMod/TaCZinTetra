package com.pycoder.taczintetra.resource;

import com.pycoder.taczintetra.config.ModuleConfig;
import com.pycoder.taczintetra.config.ModuleConfigManager;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TetraModuleResourceTest {
    private static final Path ROOT = Path.of("src/main/resources/data/tetra/modules/taczintetra");

    @Test
    void modularGunHasNativeTaczGunpackDefinition() throws Exception {
        Path meta = Path.of("src/main/resources/assets/tacz/custom/taczintetra/gunpack.meta.json");
        Path gun = Path.of("src/main/resources/assets/tacz/custom/taczintetra/data/taczintetra/data/guns/modular_gun_data.json");
        assertTrue(Files.readString(meta).contains("\"namespace\": \"taczintetra\""));
        String json = Files.readString(gun);
        assertTrue(json.contains("\"ammo\": \"tacz:9mm\""));
        assertTrue(json.contains("\"reload\""));
        assertTrue(json.contains("\"fire_mode\": [\"semi\"]"));
    }

    @Test
    void modularGunHasTaczIndexEntryForRuntimeGunId() throws Exception {
        Path index = Path.of("src/main/resources/assets/tacz/custom/taczintetra/data/taczintetra/index/guns/modular_gun.json");
        String json = Files.readString(index, StandardCharsets.UTF_8);
        assertTrue(json.contains("taczintetra:modular_gun_data"));
        assertTrue(json.contains("\"display\": \"tacz:modular_gun_display\""));
        assertTrue(json.contains("\"item_type\": \"modern_kinetic\""));
        Path display = Path.of("src/main/resources/assets/tacz/custom/taczintetra/assets/tacz/display/guns/modular_gun_display.json");
        String displayJson = Files.readString(display, StandardCharsets.UTF_8);
        assertTrue(displayJson.contains("tacz:gun/m1911_geo"));
        assertTrue(displayJson.contains("tacz:gun/uv/m1911"));
        assertTrue(displayJson.contains("\"animation\": \"tacz:m1911\""));
        assertTrue(displayJson.contains("\"hud\": \"tacz:gun/hud/m1911\""));
        assertTrue(displayJson.contains("\"slot\": \"tacz:gun/slot/m1911\""));
    }

    @Test
    void sharedRigDisplayDeclaresAllPerspectiveAndActionBindings() throws Exception {
        Path display = Path.of("src/main/resources/assets/tacz/custom/taczintetra/assets/tacz/display/guns/modular_gun_display.json");
        String json = Files.readString(display, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"use_default_animation\": \"pistol\""));
        assertTrue(json.contains("\"player_animator_3rd\": \"tacz:pistol_default.player_animation\""));
        assertTrue(json.contains("\"thirdperson\""));
        assertTrue(json.contains("\"ground\""));
        assertTrue(json.contains("\"fixed\""));
        assertTrue(json.contains("\"muzzle_flash\""));
        assertTrue(json.contains("\"iron_zoom\""));
    }

    @Test
    void gunpackHasPackInfoInItsOwnNamespace() throws Exception {
        Path info = Path.of("src/main/resources/assets/tacz/custom/taczintetra/assets/taczintetra/gunpack_info.json");
        String json = Files.readString(info, StandardCharsets.UTF_8);
        assertTrue(json.contains("pack.taczintetra.name"));
        assertTrue(json.contains("pack.taczintetra.desc"));
    }

    @Test
    void starterPistolHasConfiguredCraftingRecipe() throws Exception {
        Path recipe = Path.of("src/main/resources/data/taczintetra/recipes/starter_pistol.json");
        String json = Files.readString(recipe, StandardCharsets.UTF_8);
        assertTrue(json.contains("minecraft:planks"));
        assertTrue(json.contains("minecraft:wooden_slabs"));
        assertTrue(json.contains("taczintetra:starter_pistol"));
        assertTrue(json.contains("taczintetra:configured_shaped"));
    }

    @Test
    void allModuleFilesExposeConfiguredMaterialPrefixesAndCorrectSlot() throws Exception {
        assertTrue(Files.exists(ROOT.resolve("body/pistol.json")));
        assertTrue(Files.exists(ROOT.resolve("barrel/9mm.json")));
        assertTrue(Files.exists(ROOT.resolve("magazine/standard.json")));
        assertEquals(5, countJson(ROOT.resolve("body")));
        assertEquals(7, countJson(ROOT.resolve("barrel")));
        assertEquals(1, countJson(ROOT.resolve("magazine")));
        for (String part : new String[]{"body", "barrel", "magazine", "stock", "optic", "grip"}) {
            Path partRoot = Files.isDirectory(ROOT.resolve(part)) ? ROOT.resolve(part) : ROOT.resolve(part + ".json");
            try (var files = Files.walk(partRoot)) {
                assertTrue(files.filter(path -> path.toString().endsWith(".json")).findAny().isPresent(), part);
            }
        }
        try (var files = Files.walk(ROOT)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    assertTrue(json.contains("taczintetra/"), path.toString());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

    private static long countJson(Path directory) throws Exception {
        try (var files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".json")).count();
        }
    }

    @Test
    void materialFilesContainOnlyConfiguredFinishedPartInputs() throws Exception {
        for (String material : new String[]{"wood", "stone", "iron", "gold", "netherite"}) {
            String json = Files.readString(Path.of("src/main/resources/data/tetra/materials/taczintetra/" + material + ".json"), StandardCharsets.UTF_8);
            assertTrue(json.contains("taczintetra:" + material + "_body"), material);
            assertTrue(json.contains("taczintetra:" + material + "_barrel"), material);
            assertTrue(json.contains("taczintetra:" + material + "_magazine"), material);
            assertTrue(!json.contains("minecraft:iron_ingot"), material);
        }
    }

    @Test
    void everyConfiguredModelHasAnIndependentTetraModuleResource() throws Exception {
        ModuleConfig config = ModuleConfigManager.loadOrCreate(Files.createTempDirectory("taczintetra-config"));
        assertConfiguredModules(config.bodies().stream().map(ModuleConfig.Body::id).toList(), "body");
        assertConfiguredModules(config.barrels().stream().map(ModuleConfig.Barrel::id).toList(), "barrel");
        assertConfiguredModules(config.magazines().stream().map(ModuleConfig.Magazine::id).toList(), "magazine");
    }

    @Test
    void majorModulesExposeNativeEnchantmentCapacityAndAspects() throws Exception {
        for (String slot : new String[]{"body", "barrel", "magazine"}) {
            try (var files = Files.walk(ROOT.resolve(slot))) {
                files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                    var variant = JsonParser.parseString(read(path)).getAsJsonObject()
                            .getAsJsonArray("variants").get(0).getAsJsonObject();
                    assertTrue(variant.get("magicCapacity").getAsInt() > 0, path.toString());
                    var aspects = variant.getAsJsonObject("aspects");
                    assertTrue(aspects.get("breakable").getAsInt() > 0, path.toString());
                    assertTrue(aspects.get("vanishable").getAsInt() > 0, path.toString());
                });
            }
        }
    }

    @Test
    void specialSocketHasARealMinorModuleAndSchematic() throws Exception {
        Path module = ROOT.resolve("special/socket.json");
        Path schematic = Path.of("src/main/resources/data/tetra/schematics/taczintetra/special_socket.json");
        assertTrue(Files.exists(module));
        assertTrue(Files.exists(schematic));
        assertTrue(Files.readString(module).contains("taczintetra/special"));
        String schematicJson = Files.readString(schematic);
        assertTrue(schematicJson.contains("taczintetra/special/socket"));
        assertTrue(schematicJson.contains("tetra:taczintetra/special_inlay"));
        assertTrue(Files.readString(Path.of("src/main/resources/data/tetra/materials/taczintetra/special_inlay.json"))
                .contains("taczintetra:special_inlay"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void assertConfiguredModules(java.util.List<String> ids, String slot) throws Exception {
        for (String id : ids) {
            Path file = ROOT.resolve(slot).resolve(id + ".json");
            assertTrue(Files.exists(file), file.toString());
            var object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(object.getAsJsonArray("slots").toString().contains("taczintetra/" + slot), file.toString());
            assertTrue(object.getAsJsonArray("variants").toString().contains("\"key\":\"" + id + "/\""), file.toString());
        }
    }
}
