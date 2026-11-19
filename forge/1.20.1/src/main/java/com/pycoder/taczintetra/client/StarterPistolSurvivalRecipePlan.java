package com.pycoder.taczintetra.client;

import java.util.Map;

/** Exact survival crafting contract for the starter pistol's vanilla shaped ingredients. */
final class StarterPistolSurvivalRecipePlan {
    private static final String[] PATTERN = {"PPP", "PS ", "L  "};

    private StarterPistolSurvivalRecipePlan() {
    }

    static java.util.List<CraftingMenuRecipePlacement.Click> menuClicks(
            int plankInventoryMenuSlot, int stickInventoryMenuSlot, int slabInventoryMenuSlot) {
        return CraftingMenuRecipePlacement.plan(PATTERN, Map.of(
                'P', plankInventoryMenuSlot,
                'S', stickInventoryMenuSlot,
                'L', slabInventoryMenuSlot));
    }

    static MaterialRemainder afterOneCraft(int oakPlanks, int sticks, int woodenSlabs) {
        if (oakPlanks < 4 || sticks < 1 || woodenSlabs < 1) {
            throw new IllegalArgumentException("Starter pistol requires four planks, one stick, and one wooden slab");
        }
        return new MaterialRemainder(oakPlanks - 4, sticks - 1, woodenSlabs - 1, 1);
    }

    record MaterialRemainder(int oakPlanks, int sticks, int woodenSlabs, int starterPistols) {
    }
}
