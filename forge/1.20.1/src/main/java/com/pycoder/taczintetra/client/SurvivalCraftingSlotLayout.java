package com.pycoder.taczintetra.client;

/** Maps player inventory and 3x3 crafting coordinates to vanilla CraftingMenu slot ids. */
public final class SurvivalCraftingSlotLayout {
    private SurvivalCraftingSlotLayout() {
    }

    public static int craftingMenuSlot(int row, int column) {
        if (row < 0 || row > 2 || column < 0 || column > 2) {
            throw new IllegalArgumentException("Crafting grid coordinates must be within 0..2");
        }
        return 1 + row * 3 + column;
    }

    public static int inventoryCraftingMenuSlot(int row, int column) {
        if (row < 0 || row > 1 || column < 0 || column > 1) {
            throw new IllegalArgumentException("Player crafting grid coordinates must be within 0..1");
        }
        return 1 + row * 2 + column;
    }

    public static int playerInventoryMenuSlot(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex > 35) {
            throw new IllegalArgumentException("Player inventory index must be within 0..35");
        }
        return inventoryIndex < 9 ? 36 + inventoryIndex : 10 + inventoryIndex - 9;
    }

    public static int craftingTablePlayerInventoryMenuSlot(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex > 35) {
            throw new IllegalArgumentException("Player inventory index must be within 0..35");
        }
        return inventoryIndex < 9 ? 37 + inventoryIndex : 10 + inventoryIndex - 9;
    }
}
