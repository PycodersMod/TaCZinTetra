package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TetraWorkbenchSlotLayoutTest {
    @Test
    void mapsPlayerInventoryToTetraWorkbenchContainerAfterFourWorkbenchSlots() {
        assertEquals(31, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(0));
        assertEquals(39, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(8));
        assertEquals(4, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(9));
        assertEquals(7, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(10));
        assertEquals(5, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(18));
        assertEquals(6, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(27));
        assertEquals(30, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(35));
    }

    @Test
    void emptySlotSearchNeverReturnsArmorOrOffhandContainerSlots() {
        boolean[] empty = new boolean[41];
        empty[36] = true;
        empty[40] = true;

        assertEquals(-1, TetraWorkbenchSlotLayout.firstEmptyPlayerInventoryIndex(
                empty.length, index -> empty[index]));

        empty[5] = true;
        assertEquals(5, TetraWorkbenchSlotLayout.firstEmptyPlayerInventoryIndex(
                empty.length, index -> empty[index]));
    }
}
