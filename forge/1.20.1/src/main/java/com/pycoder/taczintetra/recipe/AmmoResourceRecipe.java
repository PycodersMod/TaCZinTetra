package com.pycoder.taczintetra.recipe;

import com.pycoder.taczintetra.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** 数据驱动的无序配方，制作带有 AmmoId 标签的 TaCZ 弹药。 */
public final class AmmoResourceRecipe implements CraftingRecipe {
    private static final String AMMO_ID = "AmmoId";

    private final ShapelessRecipe delegate;
    private final String ammoId;

    public AmmoResourceRecipe(ShapelessRecipe delegate, String ammoId) {
        if (ammoId == null || ammoId.isBlank()) throw new IllegalArgumentException("ammo_id must not be blank");
        this.delegate = delegate;
        this.ammoId = ammoId;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return delegate.matches(container, level);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        return result();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return delegate.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return delegate.getIngredients();
    }

    @Override
    public ResourceLocation getId() {
        return delegate.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.AMMO_RESOURCE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public String getGroup() {
        return delegate.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
        return delegate.category();
    }

    ShapelessRecipe delegate() {
        return delegate;
    }

    String ammoId() {
        return ammoId;
    }

    private ItemStack result() {
        ItemStack result = delegate.getResultItem(RegistryAccess.EMPTY).copy();
        result.getOrCreateTag().putString(AMMO_ID, ammoId);
        return result;
    }
}
