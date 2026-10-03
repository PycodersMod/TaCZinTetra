package com.pycoder.taczintetra.mixin;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.compat.NativeTaczRestrictionService;
import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;
import org.slf4j.Logger;

/** Removes native TaCZ recipes from the server recipe manager before indexing. */
@Mixin(net.minecraft.world.item.crafting.RecipeManager.class)
public abstract class RecipeManagerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    @ModifyArg(
            method = {"replaceRecipes", "m_44024_"},
            at = @At(value = "INVOKE", target = "Ljava/lang/Iterable;forEach(Ljava/util/function/Consumer;)V"),
            index = 0,
            remap = false,
            require = 1)
    private Consumer<Recipe<?>> taczintetra$filterNativeRecipes(Consumer<Recipe<?>> original) {
        if (!TaczInTetraForgeConfig.DISABLE_NATIVE_RECIPES.get()) return original;
        return recipe -> {
            ResourceLocation id = recipe.getId();
            if (Boolean.getBoolean("taczintetra.dev_automation") && id != null) {
                LOGGER.info("TaCZinTetra dev recipe filter observed {}", id);
            }
            ResourceLocation resultId = null;
            try {
                ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
                if (!result.isEmpty()) resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            } catch (RuntimeException ex) {
                LOGGER.warn("Unable to inspect recipe result for TaCZ restriction: {}", id, ex);
            }
            if (!NativeTaczRestrictionService.shouldBlockNativeRecipe(id, resultId)) {
                original.accept(recipe);
            }
        };
    }
}
