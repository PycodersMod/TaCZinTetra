package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurvivalCraftingSlotLayoutTest {
    @Test
    void mapsRecipeGridCoordinatesToCraftingMenuSlots() {
        assertEquals(1, SurvivalCraftingSlotLayout.craftingMenuSlot(0, 0));
        assertEquals(5, SurvivalCraftingSlotLayout.craftingMenuSlot(1, 1));
        assertEquals(9, SurvivalCraftingSlotLayout.craftingMenuSlot(2, 2));
    }

    @Test
    void mapsPlayerInventoryCraftingCoordinatesToVanillaInventoryMenuSlots() {
        assertEquals(1, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0));
        assertEquals(2, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 1));
        assertEquals(3, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 0));
        assertEquals(4, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 1));
    }

    @Test
    void rejectsPlayerCraftingCoordinatesOutsideTwoByTwoGrid() {
        assertThrows(IllegalArgumentException.class,
                () -> SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(-1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 2));
    }

    @Test
    void mapsPlayerInventoryIndicesToCraftingMenuSlots() {
        assertEquals(36, SurvivalCraftingSlotLayout.playerInventoryMenuSlot(0));
        assertEquals(44, SurvivalCraftingSlotLayout.playerInventoryMenuSlot(8));
        assertEquals(10, SurvivalCraftingSlotLayout.playerInventoryMenuSlot(9));
        assertEquals(36, SurvivalCraftingSlotLayout.playerInventoryMenuSlot(35));
    }

    @Test
    void mapsPlayerInventoryIndicesToThreeByThreeCraftingTableSlots() {
        assertEquals(37, SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(0));
        assertEquals(45, SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(8));
        assertEquals(10, SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(9));
        assertEquals(36, SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(35));
    }

    @Test
    void rejectsInventoryIndicesOutsidePlayerInventory() {
        assertThrows(IllegalArgumentException.class,
                () -> SurvivalCraftingSlotLayout.playerInventoryMenuSlot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> SurvivalCraftingSlotLayout.playerInventoryMenuSlot(36));
    }
}
