package com.pycoder.taczintetra.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds authoritative left/right-clicks for placing shaped ingredients in a vanilla 3x3 CraftingMenu. */
final class CraftingMenuRecipePlacement {
    private static final int FIRST_PLAYER_SLOT = 10;
    private static final int LAST_PLAYER_SLOT = 45;

    private CraftingMenuRecipePlacement() {
    }

    static List<Click> plan(String[] pattern, Map<Character, Integer> sourceMenuSlotByIngredient) {
        if (pattern == null || pattern.length == 0 || pattern.length > 3) {
            throw new IllegalArgumentException("A crafting table recipe must have between one and three rows");
        }
        if (sourceMenuSlotByIngredient == null || sourceMenuSlotByIngredient.isEmpty()) {
            throw new IllegalArgumentException("Every recipe ingredient needs a player inventory source slot");
        }

        int width = pattern[0] == null ? 0 : pattern[0].length();
        if (width == 0 || width > 3) {
            throw new IllegalArgumentException("A crafting table recipe row must have between one and three columns");
        }

        Map<Character, List<Integer>> ingredientSlots = new LinkedHashMap<>();
        for (int row = 0; row < pattern.length; row++) {
            String rowPattern = pattern[row];
            if (rowPattern == null || rowPattern.length() != width) {
                throw new IllegalArgumentException("Crafting pattern rows must have the same non-zero width");
            }
            for (int column = 0; column < width; column++) {
                char ingredient = rowPattern.charAt(column);
                if (ingredient == ' ') continue;
                Integer sourceSlot = sourceMenuSlotByIngredient.get(ingredient);
                if (sourceSlot == null) {
                    throw new IllegalArgumentException("Missing inventory source for recipe ingredient '" + ingredient + "'");
                }
                ingredientSlots.computeIfAbsent(ingredient, ignored -> new ArrayList<>())
                        .add(SurvivalCraftingSlotLayout.craftingMenuSlot(row, column));
            }
        }
        if (ingredientSlots.size() != sourceMenuSlotByIngredient.size()) {
            throw new IllegalArgumentException("Every provided source slot must correspond to a used recipe ingredient");
        }

        var usedSources = new java.util.HashSet<Integer>();
        for (var sourceSlot : sourceMenuSlotByIngredient.values()) {
            if (sourceSlot == null || sourceSlot < FIRST_PLAYER_SLOT || sourceSlot > LAST_PLAYER_SLOT) {
                throw new IllegalArgumentException("Ingredient sources must be player inventory slots in a crafting-table menu");
            }
            if (!usedSources.add(sourceSlot)) {
                throw new IllegalArgumentException("Each recipe ingredient must use a distinct inventory source slot");
            }
        }

        List<Click> clicks = new ArrayList<>();
        for (var entry : ingredientSlots.entrySet()) {
            int sourceSlot = sourceMenuSlotByIngredient.get(entry.getKey());
            clicks.add(new Click(sourceSlot, 0));
            for (int gridSlot : entry.getValue()) clicks.add(new Click(gridSlot, 1));
            clicks.add(new Click(sourceSlot, 0));
        }
        return List.copyOf(clicks);
    }

    record Click(int slotId, int button) {
        Click {
            if (slotId < 0 || (button != 0 && button != 1)) {
                throw new IllegalArgumentException("Invalid menu click");
            }
        }
    }
}
