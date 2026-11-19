package com.pycoder.taczintetra.client;

import java.util.function.IntPredicate;

/** Slot mapping for Tetra 6.17's workbench menu (four workbench slots, then player inventory). */
final class TetraWorkbenchSlotLayout {
    private static final int FIRST_PLAYER_INVENTORY_SLOT = 4;
    private static final int FIRST_HOTBAR_MENU_SLOT = FIRST_PLAYER_INVENTORY_SLOT + 27;
    private static final int PLAYER_INVENTORY_SIZE = 36;

    private TetraWorkbenchSlotLayout() {
    }

    static int playerInventoryMenuSlot(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex >= PLAYER_INVENTORY_SIZE) {
            throw new IllegalArgumentException("Player inventory index must be between 0 and 35");
        }
        return inventoryIndex < 9
                ? FIRST_HOTBAR_MENU_SLOT + inventoryIndex
                : FIRST_PLAYER_INVENTORY_SLOT
                + (inventoryIndex % 9) * 3
                + (inventoryIndex - 9) / 9;
    }

    static int firstEmptyPlayerInventoryIndex(int inventoryContainerSize, IntPredicate isEmpty) {
        int scanLimit = Math.min(PLAYER_INVENTORY_SIZE, Math.max(0, inventoryContainerSize));
        for (int index = 0; index < scanLimit; index++) {
            if (isEmpty.test(index)) return index;
        }
        return -1;
    }
}
