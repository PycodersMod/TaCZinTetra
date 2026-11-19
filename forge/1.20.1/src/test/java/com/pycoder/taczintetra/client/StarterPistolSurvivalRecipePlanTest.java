package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarterPistolSurvivalRecipePlanTest {
    @Test
    void placesAllThreeRecipeIngredientsInTheConfiguredShapedPattern() {
        var plan = StarterPistolSurvivalRecipePlan.menuClicks(10, 11, 12);

        assertEquals(Map.of(
                1, 1, 2, 1, 3, 1,
                4, 1, 5, 1,
                7, 1), clicksByGridSlot(plan));
    }

    @Test
    void declaresTheExactSurvivalMaterialBudgetAndExpectedRemainder() {
        var budget = StarterPistolSurvivalRecipePlan.afterOneCraft(7, 4, 6);

        assertEquals(3, budget.oakPlanks());
        assertEquals(3, budget.sticks());
        assertEquals(5, budget.woodenSlabs());
        assertEquals(1, budget.starterPistols());
    }

    @Test
    void rejectsAnIncompleteSurvivalMaterialSet() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> StarterPistolSurvivalRecipePlan.afterOneCraft(3, 4, 6));
    }

    private static Map<Integer, Integer> clicksByGridSlot(
            java.util.List<CraftingMenuRecipePlacement.Click> clicks) {
        return clicks.stream()
                .filter(click -> click.slotId() >= 1 && click.slotId() <= 9)
                .collect(java.util.stream.Collectors.toMap(
                        CraftingMenuRecipePlacement.Click::slotId,
                        CraftingMenuRecipePlacement.Click::button,
                        Integer::sum));
    }
}
