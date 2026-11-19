package com.pycoder.taczintetra.client;

import java.util.List;
import java.util.Map;

/** Exact starter mallet recipe from the pinned Tetra 1.20.1-6.17.0 data pack. */
final class TetraWorkbenchSurvivalProgressionPlan {
    static final String OAK_HAMMER_RECIPE_ID = "tetra:hammer/oak";
    static final List<String> OAK_HAMMER_PATTERN = List.of(" # ", " /#", "/  ");

    private TetraWorkbenchSurvivalProgressionPlan() {
    }

    static List<CraftingMenuRecipePlacement.Click> oakHammerClicks(int plankMenuSlot, int stickMenuSlot) {
        return CraftingMenuRecipePlacement.plan(OAK_HAMMER_PATTERN.toArray(String[]::new),
                Map.of('#', plankMenuSlot, '/', stickMenuSlot));
    }

    static boolean canCraftOakHammer(int oakPlanks, int sticks) {
        return oakPlanks >= 2 && sticks >= 2;
    }
}
