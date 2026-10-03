package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinPackagingContractTest {
    @Test
    void buildPublishesMixinConfigInJarManifest() throws Exception {
        String buildScript = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        assertTrue(buildScript.contains("MixinConfigs"), "Forge must discover the mixin config from the jar manifest");
        assertTrue(buildScript.contains("taczintetra.mixins.json"));
    }

    @Test
    void productionTaczRendererAliasRemainsCovered() throws Exception {
        String mixinSource = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/ThirdPersonOffhandRenderMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixinSource.contains("renderByItem"));
        assertTrue(mixinSource.contains("m_108829_"));
        assertTrue(mixinSource.contains("require = 0"));
    }
}
