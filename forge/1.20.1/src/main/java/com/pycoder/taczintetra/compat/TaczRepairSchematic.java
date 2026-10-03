package com.pycoder.taczintetra.compat;

import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.module.schematic.RepairSchematic;

/** 将 Tetra 修理操作桥接到项目配置的修理材料。 */
public final class TaczRepairSchematic extends RepairSchematic {
    private final ModularGunItem gun;

    public TaczRepairSchematic(ModularGunItem gun) {
        super(gun, "taczintetra");
        this.gun = gun;
    }

    @Override
    public int getRequiredQuantity(ItemStack stack, int index, ItemStack material) {
        return index == 0 ? gun.getRepairMaterialCount(stack, material) : 0;
    }

    @Override
    public boolean acceptsMaterial(ItemStack stack, String slot, int index, ItemStack material) {
        return index == 0 && !material.isEmpty() && getRequiredQuantity(stack, index, material) > 0;
    }

    @Override
    public boolean isMaterialsValid(ItemStack stack, String slot, ItemStack[] materials) {
        return materials != null && materials.length > 0
                && acceptsMaterial(stack, slot, 0, materials[0])
                && materials[0].getCount() >= getRequiredQuantity(stack, 0, materials[0]);
    }
}
