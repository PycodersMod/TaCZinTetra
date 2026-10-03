package com.pycoder.taczintetra.network;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackIdentityTest {
    @Test
    void normalizesNegativeHashesWithoutMinValueOverflow() {
        assertTrue(StackIdentity.normalize(-1) >= 0);
        assertTrue(StackIdentity.normalize(Integer.MIN_VALUE) >= 0);
        assertEquals(0, StackIdentity.normalize(0));
    }

    @Test
    void equalStackCopiesHaveTheSameStableIdentity() {
        assertEquals(StackIdentity.fromComponents(12, 2, 44),
                StackIdentity.fromComponents(12, 2, 44));
    }

    @Test
    void stackCountIsPartOfTheIdentity() {
        assertTrue(StackIdentity.fromComponents(12, 1, 44)
                != StackIdentity.fromComponents(12, 2, 44));
    }

    @Test
    void runtimeStateDoesNotChangeTheIdentityHash() {
        CompoundTag first = new CompoundTag();
        first.putInt("taczintetra_ammo", 12);
        first.putString("GunId", "taczintetra:modular_gun");
        first.putString("taczintetra/body", "taczintetra/body/pistol");
        CompoundTag second = first.copy();
        second.putInt("taczintetra_ammo", 3);
        second.putInt("Damage", 7);
        assertEquals(StackIdentity.canonicalTagHash(first), StackIdentity.canonicalTagHash(second));
    }

    @Test
    void reloadLifecycleMarkersDoNotInterruptTheirOwningStack() {
        CompoundTag before = new CompoundTag();
        before.putString("taczintetra/body", "taczintetra/body/pistol");
        CompoundTag during = before.copy();
        during.putLong("taczintetra_reload_started_at", 123L);
        during.putString("taczintetra_reload_state", "TACTICAL_RELOAD_FEEDING");
        assertEquals(StackIdentity.canonicalTagHash(before), StackIdentity.canonicalTagHash(during));
    }

    @Test
    void uniqueStackMarkerSeparatesTwoOtherwiseIdenticalPhysicalStacks() {
        CompoundTag first = new CompoundTag();
        CompoundTag second = new CompoundTag();
        first.putUUID("taczintetra_stack_uuid", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        second.putUUID("taczintetra_stack_uuid", UUID.fromString("00000000-0000-0000-0000-000000000002"));

        assertTrue(StackIdentity.canonicalTagHash(first) != StackIdentity.canonicalTagHash(second));
    }

    @Test
    void serverEntryPointsProvisionTheUniqueMarkerBeforeTheNextSynchronizedIntent() throws Exception {
        String identity = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/StackIdentity.java"), StandardCharsets.UTF_8);
        String reload = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/ReloadIntentMessage.java"), StandardCharsets.UTF_8);
        String offhand = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/network/OffhandShootIntentMessage.java"), StandardCharsets.UTF_8);

        assertTrue(identity.contains("ensureUniqueId"));
        assertTrue(identity.contains("randomUUID"));
        assertTrue(reload.contains("StackIdentity.ensureUniqueId(held)"));
        assertTrue(offhand.contains("StackIdentity.ensureUniqueId(offhand)"));
    }
}
