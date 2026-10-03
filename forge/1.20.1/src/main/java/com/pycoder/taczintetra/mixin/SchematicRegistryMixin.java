package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.compat.TaczRepairSchematic;
import com.pycoder.taczintetra.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.Map;

/** Keeps the dynamic repair schematic present after Tetra rebuilds its map. */
@Mixin(value = SchematicRegistry.class, remap = false)
public abstract class SchematicRegistryMixin {
    private static final ResourceLocation REPAIR_ID =
            ResourceLocation.fromNamespaceAndPath("tetra", "taczintetra_repair");

    @Shadow @Final private Map<ResourceLocation, UpgradeSchematic> schematicMap;

    @Inject(method = "setupSchematics", at = @At("TAIL"), remap = false)
    private void taczintetra$restoreRepairSchematic(Map<ResourceLocation, ?> definitions, CallbackInfo callback) {
        if (ModItems.STARTER_PISTOL.isPresent()) {
            schematicMap.put(REPAIR_ID, new TaczRepairSchematic((com.pycoder.taczintetra.item.ModularGunItem)
                    ModItems.STARTER_PISTOL.get()));
        }
    }
}
