package com.pycoder.taczintetra.recipe;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import com.google.gson.JsonObject;

/** Reads the user-editable TOML recipe while retaining vanilla shaped-recipe networking. */
public final class ConfiguredShapedRecipeSerializer implements RecipeSerializer<ShapedRecipe> {
    @Override
    public ShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
        return (ShapedRecipe) ConfiguredStarterRecipe.from(new ShapedRecipe.Serializer().fromJson(id, json));
    }

    @Override
    public ShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, ShapedRecipe recipe) {
        RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
    }
}
