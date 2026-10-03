package com.pycoder.taczintetra.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 防止 Tetra 蓝图声明无法制作的模块键。 */
class TetraSchematicResourceContractTest {
    private static final Path RESOURCE_ROOT = Path.of("src/main/resources");
    private static final String MODULE_NAMESPACE = "taczintetra";

    @Test
    void everyConfiguredSchematicOutcomeResolvesToAModuleVariantAndMaterials() throws Exception {
        Path schematicRoot = RESOURCE_ROOT.resolve("data/tetra/schematics/taczintetra");
        try (Stream<Path> files = Files.list(schematicRoot)) {
            List<Path> schematics = files.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
            assertFalse(schematics.isEmpty());
            for (Path schematic : schematics) {
                JsonObject root = parse(schematic);
                JsonArray slots = root.getAsJsonArray("slots");
                assertFalse(slots.isEmpty(), schematic.toString());
                for (var outcomeElement : root.getAsJsonArray("outcomes")) {
                    JsonObject outcome = outcomeElement.getAsJsonObject();
                    String moduleKey = outcome.get("moduleKey").getAsString();
                    String variant = outcome.get("moduleVariant").getAsString();
                    assertTrue(moduleKey.startsWith(MODULE_NAMESPACE + "/"), moduleKey);
                    assertTrue(variant.endsWith("/"), variant);
                    assertFalse(outcome.getAsJsonArray("materials").isEmpty(), moduleKey);

                    Path moduleFile = RESOURCE_ROOT.resolve("data/tetra/modules/" + moduleKey + ".json");
                    assertTrue(Files.exists(moduleFile), moduleFile.toString());
                    JsonObject module = parse(moduleFile);
                    boolean variantFound = false;
                    for (var variantElement : module.getAsJsonArray("variants")) {
                        if (variant.equals(variantElement.getAsJsonObject().get("key").getAsString())) {
                            variantFound = true;
                            assertTrue(variantElement.getAsJsonObject().has("materials"), variant);
                            break;
                        }
                    }
                    assertTrue(variantFound, moduleKey + " -> " + variant);
                }
            }
        }
    }

    private static JsonObject parse(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
