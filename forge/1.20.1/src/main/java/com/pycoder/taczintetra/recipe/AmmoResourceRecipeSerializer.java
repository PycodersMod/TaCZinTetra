package com.pycoder.taczintetra.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Serializes the editable ingredient list and the TaCZ AmmoId payload. */
public final class AmmoResourceRecipeSerializer implements RecipeSerializer<AmmoResourceRecipe> {
    @Override
    public AmmoResourceRecipe fromJson(ResourceLocation id, JsonObject json) {
        ShapelessRecipe delegate = RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json);
        String ammoId = json.get("ammo_id").getAsString();
        return new AmmoResourceRecipe(delegate, ammoId);
    }

    @Override
    public AmmoResourceRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        ShapelessRecipe delegate = RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buffer);
        return new AmmoResourceRecipe(delegate, buffer.readUtf(256));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, AmmoResourceRecipe recipe) {
        RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer, recipe.delegate());
        buffer.writeUtf(recipe.ammoId(), 256);
    }
}
