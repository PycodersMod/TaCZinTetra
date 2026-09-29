package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import com.pycoder.taczintetra.runtime.SemiFinishedPartPolicy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.module.schematic.SchematicType;
import com.pycoder.taczintetra.item.ModularGunItem;

/** Prevents configured Tetra module crafting from consuming arbitrary raw materials. */
@Mixin(value = WorkbenchTile.class, remap = false)
public abstract class WorkbenchTileMixin {
    @Inject(method = "craft", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$requireSemiFinishedMaterials(Player player, CallbackInfo callback) {
        if (!TaczInTetraForgeConfig.ONLY_SEMI_FINISHED_PARTS.get()) {
            return;
        }
        WorkbenchTile workbench = (WorkbenchTile) (Object) this;
        if (!(workbench.getTargetItemStack().getItem() instanceof ModularGunItem)) {
            return;
        }
        var schematic = workbench.getCurrentSchematic();
        if (schematic == null || schematic.getType() != SchematicType.major) {
            return;
        }
        String currentSlot = workbench.getCurrentSlot();
        for (ItemStack material : workbench.getMaterials()) {
            if (!SemiFinishedPartPolicy.accepts(material, currentSlot)) {
                callback.cancel();
                return;
            }
        }
    }
}
