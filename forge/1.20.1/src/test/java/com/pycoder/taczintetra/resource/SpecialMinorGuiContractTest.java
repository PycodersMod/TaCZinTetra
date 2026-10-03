package com.pycoder.taczintetra.resource;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SpecialMinorGuiContractTest {
    @Test
    void modularGunProvidesOffsetsForConfiguredFourthMinorSlot() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"));

        assertTrue(source.contains("GunModuleSlots.SPECIAL"));
        assertTrue(source.contains("getMinorGuiOffsets(ItemStack stack)"));
        assertTrue(source.contains("new GuiModuleOffsets(-12, -1, -21, 12, -12, 25, 4, 12)"));
    }
}
