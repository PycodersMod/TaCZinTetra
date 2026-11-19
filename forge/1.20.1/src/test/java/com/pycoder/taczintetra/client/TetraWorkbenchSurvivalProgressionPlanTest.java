package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TetraWorkbenchSurvivalProgressionPlanTest {
    @Test
    void mapsTheOfficialOakHammerRecipeToTheExactCraftingMenuSlots() {
        assertEquals(List.of(
                new CraftingMenuRecipePlacement.Click(10, 0),
                new CraftingMenuRecipePlacement.Click(2, 1),
                new CraftingMenuRecipePlacement.Click(6, 1),
                new CraftingMenuRecipePlacement.Click(10, 0),
                new CraftingMenuRecipePlacement.Click(11, 0),
                new CraftingMenuRecipePlacement.Click(5, 1),
                new CraftingMenuRecipePlacement.Click(7, 1),
                new CraftingMenuRecipePlacement.Click(11, 0)),
                TetraWorkbenchSurvivalProgressionPlan.oakHammerClicks(10, 11));
    }

    @Test
    void oakHammerRecipeRequiresTwoPlanksAndTwoSticks() {
        assertTrue(TetraWorkbenchSurvivalProgressionPlan.canCraftOakHammer(2, 2));
        assertFalse(TetraWorkbenchSurvivalProgressionPlan.canCraftOakHammer(1, 2));
        assertFalse(TetraWorkbenchSurvivalProgressionPlan.canCraftOakHammer(2, 1));
    }

    @Test
    void recipePlanUsesTheVerifiedTetraRecipeIdAndPattern() {
        assertEquals("tetra:hammer/oak", TetraWorkbenchSurvivalProgressionPlan.OAK_HAMMER_RECIPE_ID);
        assertEquals(List.of(" # ", " /#", "/  "),
                TetraWorkbenchSurvivalProgressionPlan.OAK_HAMMER_PATTERN);
    }
}
