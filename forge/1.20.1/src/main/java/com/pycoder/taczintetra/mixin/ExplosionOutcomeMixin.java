package com.pycoder.taczintetra.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ToolAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.craftingeffect.outcome.ExplosionOutcome;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.Map;

/** Skips only malformed optional Tetra explosion effects instead of crashing the workbench. */
@Mixin(value = ExplosionOutcome.class, remap = false)
public abstract class ExplosionOutcomeMixin {
    @Shadow
    private Level.ExplosionInteraction type;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$skipMissingExplosionType(ResourceLocation[] moduleKeys, ItemStack item,
                                                       String slot, boolean apply, Player player,
                                                       ItemStack[] materials, Map<ToolAction, Integer> tools,
                                                       Level level, UpgradeSchematic schematic, BlockPos origin,
                                                       BlockState state, boolean craftingEffects,
                                                       ItemStack[] ingredients, float stability,
                                                       CallbackInfoReturnable<Boolean> callback) {
        if (type == null) {
            callback.setReturnValue(false);
        }
    }
}
