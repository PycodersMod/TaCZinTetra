package com.pycoder.taczintetra.logic;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceNbtAdapterTest {
    @Test
    void writesAndReadsNestedResourceChannels() {
        CompoundTag root = new CompoundTag();
        ResourceNbtAdapter.write(root, "item", "tacz:9mm", 12);
        assertEquals(12, ResourceNbtAdapter.read(root, "item", "tacz:9mm"));
        assertEquals(0, ResourceNbtAdapter.read(root, "item", "tacz:45"));
    }
}
