package com.pycoder.taczintetra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;

/** 加载可编辑 JSON 目录；文件缺失时根据随包默认值创建。 */
public final class ModuleConfigManager {
    private static final String RELATIVE_PATH = "taczintetra.json";
    private static final String LEGACY_RELATIVE_PATH = "taczintetra/modules.json";
    private static final String DEFAULT_RESOURCE = "/defaultconfigs/taczintetra.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    private ModuleConfigManager() { }

    public static ModuleConfig loadOrCreate(Path configDirectory) {
        Path file = configDirectory.resolve(RELATIVE_PATH);
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) migrateOrCopyDefaults(configDirectory, file);
            else reconcileLegacyDirectory(configDirectory, file);
            JsonObject current;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                current = JsonParser.parseReader(reader).getAsJsonObject();
            }
            if (mergeMissingDefaults(current) | ensureNineCategories(current)) writeJson(file, current);
            deleteEmptyLegacyDirectory(configDirectory.resolve("taczintetra"));
            return ModuleConfig.from(current);
        } catch (Exception exception) {
            LOGGER.warn("Could not load {}; using an empty safe configuration", file, exception);
            return ModuleConfig.empty();
        }
    }

    private static void migrateOrCopyDefaults(Path configDirectory, Path target) throws IOException {
        Path legacy = configDirectory.resolve(LEGACY_RELATIVE_PATH);
        if (Files.exists(legacy)) {
            Files.copy(legacy, target, java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
            Files.copy(legacy, target.resolveSibling(target.getFileName() + ".legacy.bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(legacy);
            deleteEmptyLegacyDirectory(legacy.getParent());
            return;
        }
        copyDefaults(target);
        deleteEmptyLegacyDirectory(configDirectory.resolve("taczintetra"));
    }

    private static void reconcileLegacyDirectory(Path configDirectory, Path target) throws IOException {
        Path directory = configDirectory.resolve("taczintetra");
        Path legacy = directory.resolve("modules.json");
        if (Files.notExists(legacy)) {
            deleteEmptyLegacyDirectory(directory);
            return;
        }
        Path backup = target.resolveSibling(target.getFileName() + ".legacy.bak");
        if (Files.notExists(backup)) Files.copy(legacy, backup);
        Files.deleteIfExists(legacy);
        Files.deleteIfExists(directory.resolve("modules.json.bak"));
        deleteEmptyLegacyDirectory(directory);
    }

    private static void deleteEmptyLegacyDirectory(Path directory) throws IOException {
        if (directory != null && Files.isDirectory(directory)) {
            try (var entries = Files.list(directory)) {
                if (entries.findAny().isEmpty()) Files.deleteIfExists(directory);
            }
        }
    }

    private static boolean ensureNineCategories(JsonObject current) {
        boolean changed = false;
        String[] names = {"barrels", "bodies", "magazines", "optics", "stocks", "grips", "enchantments", "repair_agents", "special_inlays"};
        for (String name : names) {
            if (!current.has(name)) { current.add(name, new com.google.gson.JsonArray()); changed = true; }
        }
        if (current.has("attachments") && current.get("attachments").isJsonArray()) {
            for (var element : current.getAsJsonArray("attachments")) {
                if (!element.isJsonObject()) continue;
                String slot = element.getAsJsonObject().has("slot") ? element.getAsJsonObject().get("slot").getAsString() : "";
                String target = slot.contains("optic") || slot.contains("sight") ? "optics" : slot.contains("stock") ? "stocks" : "grips";
                current.getAsJsonArray(target).add(element.deepCopy());
            }
            current.remove("attachments");
            changed = true;
        }
        return changed;
    }

    private static void copyDefaults(Path target) throws IOException {
        try (InputStream stream = ModuleConfigManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) throw new IOException("Packaged module defaults are missing");
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
            Files.move(temporary, target);
        }
    }

    private static boolean mergeMissingDefaults(JsonObject current) throws IOException {
        JsonObject defaults;
        try (InputStream stream = ModuleConfigManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) throw new IOException("Packaged module defaults are missing");
            defaults = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        boolean changed = false;
        return mergeObjects(current, defaults);
    }

    private static boolean mergeObjects(JsonObject current, JsonObject defaults) {
        boolean changed = false;
        for (var entry : defaults.entrySet()) {
            if (!current.has(entry.getKey())) {
                current.add(entry.getKey(), entry.getValue().deepCopy());
                changed = true;
            } else if (current.get(entry.getKey()).isJsonObject() && entry.getValue().isJsonObject()) {
                changed |= mergeObjects(current.getAsJsonObject(entry.getKey()), entry.getValue().getAsJsonObject());
            }
        }
        return changed;
    }

    private static void writeJson(Path target, JsonObject object) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(object, writer);
        }
        Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
