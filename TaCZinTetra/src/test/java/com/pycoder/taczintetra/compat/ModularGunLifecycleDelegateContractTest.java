package com.pycoder.taczintetra.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularGunLifecycleDelegateContractTest {
    @Test
    void lifecycleAdapterUsesTaczRegisteredGunInsteadOfConstructingItemAfterFreeze() throws Exception {
        Path source = Path.of("src/main/java/com/pycoder/taczintetra/compat/ModularGunLifecycleAdapter.java");
        String text = Files.readString(source);
        assertTrue(text.contains("ModItems.MODERN_KINETIC_GUN.get()"));
        assertFalse(text.contains("new ModernKineticGunItem()"));
    }
}
