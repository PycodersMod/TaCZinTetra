package com.pycoder.taczintetra.mixin;

import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** Adds one generic server-authoritative insertion path for standard menus. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    /** Named userdev and SRG release servers expose different literal names. */
    @Inject(method = {"clicked", "m_150399_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void taczintetra$insertCarriedResource(int slotId, int button, ClickType clickType,
                                                    Player player, CallbackInfo ci) {
        if (player.level().isClientSide || clickType != ClickType.PICKUP || button != 1
                || slotId < 0) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ItemStack carried = menu.getCarried();
        Slot target;
        try {
            target = menu.getSlot(slotId);
        } catch (IndexOutOfBoundsException ignored) {
            return;
        }
        if (carried.isEmpty() || target == null || target.getItem().isEmpty()
                || !target.isActive() || !target.mayPickup(player) || !target.mayPlace(carried)) {
            return;
        }
        int inserted = ResourceInsertionService.insert(target.getItem(), carried);
        if (inserted > 0) {
            target.setChanged();
            menu.setCarried(carried);
            ci.cancel();
        }
    }
}
