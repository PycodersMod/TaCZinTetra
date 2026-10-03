package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 确保已发布元数据与实际验证过的依赖矩阵保持一致。 */
class VersionCompatibilityContractTest {
    @Test
    void metadataPinsTheVerifiedForgeAndIntegrationVersions() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"), StandardCharsets.UTF_8);
        assertTrue(properties.contains("forge_version=47.4.16"));
        assertTrue(properties.contains("forge_version_range=[47,48)"));
        assertTrue(metadata.contains("versionRange=\"[6.17.0]\""));
        assertTrue(metadata.contains("versionRange=\"[1.1.8-hotfix]\""));
        assertTrue(metadata.contains("versionRange=\"[6.3.0]\""));
        assertTrue(metadata.contains("versionRange=\"[15.20.0.106]\""));
    }

    @Test
    void optionalJeiDependencyRemainsOptionalAndServerRunHasNoJeiClassPath() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        assertTrue(build.contains("compileOnly(\"mezz.jei:jei-1.20.1-common-api:15.20.0.106\")"));
        assertTrue(build.contains("compileOnly(\"mezz.jei:jei-1.20.1-forge-api:15.20.0.106\")"));
        assertTrue(build.contains("create(\"server\")"));
        assertTrue(build.contains("runtimeOnly(fg.deobf(\"mezz.jei:jei-1.20.1-forge:15.20.0.106\"))"));
    }
}
