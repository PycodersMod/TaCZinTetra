package com.pycoder.taczintetra.client;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 显示游戏内检查模组枪械所需的持久化模块数据和运行时 NBT。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class ModularGunTooltipHandler {
    private static final String[] MODULE_KEYS = {
            "taczintetra/body", "taczintetra/barrel", "taczintetra/magazine",
            "taczintetra/optic", "taczintetra/stock", "taczintetra/grip"
    };

    private ModularGunTooltipHandler() {
    }

    @SubscribeEvent
    public static void append(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModularGunItem) || !stack.hasTag()) return;
        var tag = stack.getTag();
        if (tag == null) return;
        event.getToolTip().add(Component.literal("TaCZ NBT"));
        for (String key : MODULE_KEYS) addTagLine(event, tag, key);
        addTagLine(event, tag, "taczintetra_ammo");
        addTagLine(event, tag, "taczintetra_max_ammo");
        addTagLine(event, tag, "resources");
    }

    private static void addTagLine(ItemTooltipEvent event, net.minecraft.nbt.CompoundTag tag, String key) {
        if (!tag.contains(key)) return;
        Tag value = tag.get(key);
        event.getToolTip().add(Component.literal(key + " = " + (value == null ? "null" : value.getAsString())));
    }
}
