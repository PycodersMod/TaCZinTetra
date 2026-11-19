package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CraftingMenuRecipePlacementTest {
    @Test
    void plansWoodBarrelInputsWithExactCraftingMenuSlots() {
        var clicks = CraftingMenuRecipePlacement.plan(
                new String[]{"P", "S", "P"}, Map.of('P', 10, 'S', 11));

        assertEquals(List.of(
                new CraftingMenuRecipePlacement.Click(10, 0),
                new CraftingMenuRecipePlacement.Click(1, 1),
                new CraftingMenuRecipePlacement.Click(7, 1),
                new CraftingMenuRecipePlacement.Click(10, 0),
                new CraftingMenuRecipePlacement.Click(11, 0),
                new CraftingMenuRecipePlacement.Click(4, 1),
                new CraftingMenuRecipePlacement.Click(11, 0)), clicks);
    }

    @Test
    void plansFivePlanksAndTwoSticksForTheWoodBodyRecipe() {
        var clicks = CraftingMenuRecipePlacement.plan(
                new String[]{"PPP", "P P", "S S"}, Map.of('P', 10, 'S', 11));

        assertEquals(List.of(
                new CraftingMenuRecipePlacement.Click(10, 0),
                new CraftingMenuRecipePlacement.Click(1, 1),
                new CraftingMenuRecipePlacement.Click(2, 1),
                new CraftingMenuRecipePlacement.Click(3, 1),
                new CraftingMenuRecipePlacement.Click(4, 1),
                new CraftingMenuRecipePlacement.Click(6, 1),
                new CraftingMenuRecipePlacement.Click(10, 0),
                new CraftingMenuRecipePlacement.Click(11, 0),
                new CraftingMenuRecipePlacement.Click(7, 1),
                new CraftingMenuRecipePlacement.Click(9, 1),
                new CraftingMenuRecipePlacement.Click(11, 0)), clicks);
    }

    @Test
    void plansTheStarterPistolWoodenSlabAndThreeByThreePattern() {
        var sourceSlots = new LinkedHashMap<Character, Integer>();
        sourceSlots.put('P', 10);
        sourceSlots.put('S', 11);
        sourceSlots.put('L', 12);

        var clicks = CraftingMenuRecipePlacement.plan(
                new String[]{"PPP", "PS ", "L  "}, sourceSlots);

        assertEquals(6, clicks.stream().filter(click -> click.button() == 1).count());
        assertEquals(List.of(10, 1, 2, 3, 4, 10, 11, 5, 11, 12, 7, 12), clicks.stream()
                .map(CraftingMenuRecipePlacement.Click::slotId).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 7), clicks.stream()
                .filter(click -> click.button() == 1)
                .map(CraftingMenuRecipePlacement.Click::slotId).toList());
    }

    @Test
    void rejectsMissingSourcesAndInvalidGridPatterns() {
        assertThrows(IllegalArgumentException.class,
                () -> CraftingMenuRecipePlacement.plan(new String[]{"PS"}, Map.of('P', 10)));
        assertThrows(IllegalArgumentException.class,
                () -> CraftingMenuRecipePlacement.plan(new String[]{"P   "}, Map.of('P', 10)));
        assertThrows(IllegalArgumentException.class,
                () -> CraftingMenuRecipePlacement.plan(new String[]{"P"}, Map.of('P', 0)));
    }
}
