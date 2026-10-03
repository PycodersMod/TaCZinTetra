package com.pycoder.taczintetra.compat.jei;

import com.pycoder.taczintetra.compat.NativeTaczRestrictionService;
import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Optional JEI bridge. The class is only loaded when JEI is installed. */
@JeiPlugin
public final class TaczJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("taczintetra", "jei_plugin");
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (TaczInTetraForgeConfig.JEI_MODE.get() != TaczInTetraForgeConfig.JeiMode.DISABLED) return;
        java.util.List<ItemStack> disabled = java.util.stream.StreamSupport
                .stream(BuiltInRegistries.ITEM.spliterator(), false)
                .map(ItemStack::new)
                .filter(stack -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    return NativeTaczRestrictionService.shouldBlockNativeItem(id)
                            || NativeTaczRestrictionService.shouldBlockNativeWorkbench(id);
                })
                .toList();
        if (!disabled.isEmpty()) {
            registration.addItemStackInfo(disabled, Component.translatable("taczintetra.jei.disabled"));
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (TaczInTetraForgeConfig.JEI_MODE.get() != TaczInTetraForgeConfig.JeiMode.HIDE) return;
        java.util.List<ItemStack> hidden = runtime.getIngredientManager().getAllItemStacks().stream()
                .filter(stack -> NativeTaczRestrictionService.shouldHideFromJei(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())))
                .toList();
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }

        // Ingredient removal alone does not remove recipe rows already indexed by JEI.
        // Resolve every loaded recipe through JEI so custom TaCZ categories are covered.
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            int hiddenRecipes = hideNativeRecipes(runtime, level.getRecipeManager().getRecipes());
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev JEI hide probe: items={}, craftingRecipes={}, level={}",
                        hidden.size(), hiddenRecipes, level.dimension().location());
            }
        } else if (Boolean.getBoolean("taczintetra.dev_automation")) {
            LOGGER.info("TaCZinTetra dev JEI hide probe: items={}, craftingRecipes=0, level=null", hidden.size());
        }
    }

    private static int hideNativeRecipes(IJeiRuntime runtime, java.util.Collection<Recipe<?>> recipes) {
        Map<RecipeType<?>, List<Object>> grouped = new HashMap<>();
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<RecipeType<?>> registeredTypes = (List) runtime.getRecipeManager().createRecipeCategoryLookup()
                .includeHidden()
                .get()
                .map(category -> category.getRecipeType())
                .toList();
        int hidden = 0;
        for (Recipe<?> recipe : recipes) {
            ResourceLocation id = recipe.getId();
            ResourceLocation resultId = null;
            try {
                ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
                if (!result.isEmpty()) resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            } catch (RuntimeException ex) {
                LOGGER.warn("Unable to inspect JEI recipe result for TaCZ restriction: {}", id, ex);
            }
            if (!NativeTaczRestrictionService.shouldHideFromJei(id, resultId)) continue;
            List<RecipeType<?>> matchingTypes = registeredTypes.stream()
                    .filter(type -> type.getRecipeClass().isAssignableFrom(recipe.getClass()))
                    .toList();
            if (matchingTypes.isEmpty()) {
                LOGGER.debug("No JEI recipe type found for hidden TaCZ recipe {}", id);
                continue;
            }
            matchingTypes.forEach(type -> grouped.computeIfAbsent(type, ignored -> new ArrayList<>()).add(recipe));
            hidden++;
        }
        for (Map.Entry<RecipeType<?>, List<Object>> entry : grouped.entrySet()) {
            hideRecipes(runtime, entry.getKey(), entry.getValue());
        }
        return hidden;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hideRecipes(IJeiRuntime runtime, RecipeType<?> type, List<Object> recipes) {
        runtime.getRecipeManager().hideRecipes((RecipeType) type, recipes);
    }
}
