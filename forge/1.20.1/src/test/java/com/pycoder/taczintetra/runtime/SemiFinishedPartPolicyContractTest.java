package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SemiFinishedPartPolicyContractTest {
    @Test
    void workbenchMixinGuardsEveryMaterialSlotWithEditableCatalog() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/WorkbenchTileMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ONLY_SEMI_FINISHED_PARTS.get()"));
        assertTrue(source.contains("workbench.getMaterials()"));
        assertTrue(source.contains("SemiFinishedPartPolicy.accepts(material, currentSlot)"));
        assertTrue(source.contains("callback.cancel()"));
        assertTrue(source.contains("getTargetItemStack()"));
        assertTrue(source.contains("instanceof ModularGunItem"));
    }

    @Test
    void workbenchMaterialRestrictionOnlyRunsForMajorModuleSchematics() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/WorkbenchTileMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("getCurrentSchematic()"));
        assertTrue(source.contains("SchematicType.major"));
    }

    @Test
    void policyReadsPhysicalPartItemsInsteadOfHardCodingIds() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/SemiFinishedPartPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("physicalPartItems()"));
        assertTrue(!source.contains("pistol_body"));
    }

    @Test
    void policyMustKeepMainPartMaterialsBoundToTheirWorkbenchSlot() {
        assertTrue(SemiFinishedPartPolicy.matchesSlot("taczintetra:wood_body", "taczintetra/body"));
        assertTrue(!SemiFinishedPartPolicy.matchesSlot("taczintetra:wood_magazine", "taczintetra/body"));
        assertTrue(SemiFinishedPartPolicy.matchesSlot("taczintetra:iron_barrel", "barrel"));
        assertTrue(!SemiFinishedPartPolicy.matchesSlot("taczintetra:iron_body", "taczintetra/magazine"));
    }
}
