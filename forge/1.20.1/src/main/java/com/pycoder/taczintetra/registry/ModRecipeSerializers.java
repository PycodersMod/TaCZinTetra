package com.pycoder.taczintetra.registry;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.recipe.ConfiguredShapedRecipeSerializer;
import com.pycoder.taczintetra.recipe.AmmoResourceRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TaCZinTetra.MOD_ID);

    public static final RegistryObject<RecipeSerializer<?>> CONFIGURED_SHAPED =
            RECIPE_SERIALIZERS.register("configured_shaped", ConfiguredShapedRecipeSerializer::new);

    public static final RegistryObject<RecipeSerializer<?>> AMMO_RESOURCE =
            RECIPE_SERIALIZERS.register("ammo_resource", AmmoResourceRecipeSerializer::new);

    private ModRecipeSerializers() {
    }
}
