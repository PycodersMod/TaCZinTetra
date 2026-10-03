package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 确保原生 TaCZ 注册表覆盖范围及调试配置读取正确。 */
class NativeTaczRestrictionContractTest {
    private static final Path ROOT = Path.of("src/main/java");

    @Test
    void nativeTaczCoverageIncludesGunAndAllThreeWorkbenchTypes() throws Exception {
        String source = Files.readString(ROOT.resolve("com/pycoder/taczintetra/compat/NativeTaczRestrictionService.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("modern_kinetic_gun"));
        assertTrue(source.contains("gun_smith_table"));
        assertTrue(source.contains("workbench_a"));
        assertTrue(source.contains("workbench_b"));
        assertTrue(source.contains("workbench_c"));
        assertTrue(source.contains("DISABLE_NATIVE_ITEMS"));
        assertTrue(source.contains("DISABLE_NATIVE_WORKBENCHES"));
        assertTrue(source.contains("shouldBlockNativeRecipe"));
    }

    @Test
    void nativeItemSwitchDoesNotDisableAmmoOrAttachmentResources() throws Exception {
        String source = Files.readString(ROOT.resolve("com/pycoder/taczintetra/compat/NativeTaczRestrictionService.java"), StandardCharsets.UTF_8);
        assertFalse(source.contains("\"ammo\""));
        assertFalse(source.contains("\"attachment\""));
    }

    @Test
    void debugLoggingConfigIsConsumedByStartupDiagnostics() throws Exception {
        String source = Files.readString(ROOT.resolve("com/pycoder/taczintetra/TaCZinTetra.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("DEBUG_LOGGING.get()"));
        assertTrue(source.contains("TaCZinTetra debug logging enabled"));
    }
}
