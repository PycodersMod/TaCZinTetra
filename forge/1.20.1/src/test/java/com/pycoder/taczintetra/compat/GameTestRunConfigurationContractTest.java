package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTestRunConfigurationContractTest {
    @Test
    void gameTestServerRunsThisModInTheSelectedPrivateRuntimeDirectory() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);

        assertTrue(build.contains("create(\"gameTestServer\")"),
                "Forge GameTest server run configuration must be registered");
        assertTrue(build.contains("property(\"forge.enabledGameTestNamespaces\", modId)"),
                "the GameTest server must enable this mod's test namespace");
        assertTrue(build.contains("runGameTestServer"),
                "the GameTest server must prepare dependencies before launch");
    }

    @Test
    void serverAndGameTestRunConfigurationsLoadThisModsMixins() throws Exception {
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        String expectedArg = "arg(\"-mixin.config=taczintetra.mixins.json\")";

        for (String runName : new String[] {"server", "gameTestServer"}) {
            int start = build.indexOf("create(\"" + runName + "\")");
            int next = build.indexOf("create(\"", start + 1);
            String runBlock = start < 0 ? "" : build.substring(start, next < 0 ? build.length() : next);
            assertTrue(runBlock.contains(expectedArg),
                    runName + " source run configuration must load taczintetra.mixins.json");
        }
    }

    @Test
    void gameTestsHaveADeclaredTemplateCopiedIntoTheSelectedRuntimeDirectory() throws Exception {
        String testSource = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/gametest/TaCZinTetraGameTests.java"),
                StandardCharsets.UTF_8);
        String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
        Path template = Path.of("gameteststructures/taczintetragametests.empty.snbt");

        assertTrue(testSource.contains("@GameTest(template = \"empty\""),
                "the GameTest annotations must continue to use the empty template");
        assertTrue(Files.isRegularFile(template),
                "the declared GameTest template must exist in the mod resources");
        assertTrue(build.contains("prepareGameTestStructures"),
                "GameTest structures must be copied into the selected runtime directory");
    }
}
