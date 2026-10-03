package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.api.AddonAdapterRegistry;
import com.pycoder.taczintetra.api.ResourceChannelType;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 服务端单向资源插入；不提供从枪械向背包返还资源的操作。 */
public final class ResourceInsertionService {
    public static final String ITEM_CHANNEL = "taczintetra:item";

    private ResourceInsertionService() {
    }

    public static int insert(ItemStack gun, ItemStack resource) {
        if (gun == null || resource == null || gun.isEmpty() || resource.isEmpty()
                || !(gun.getItem() instanceof ModularGunItem modularGun)) {
            return 0;
        }
        for (var adapter : AddonAdapterRegistry.itemAdapters()) {
            if (adapter.channelType() == ResourceChannelType.ITEM && adapter.supports(resource)) {
                int accepted = Math.max(0, Math.min(resource.getCount(), adapter.insert(gun, resource, resource.getCount())));
                if (accepted > 0) {
                    resource.shrink(accepted);
                    return accepted;
                }
                // 即使辅助模组当前暂时拒绝插入，
                // 该资源仍归辅助模组所有；不得将其库存复制到核心 NBT。
                return 0;
            }
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(resource.getItem());
        if (key == null || key.getPath().isBlank()) {
            return 0;
        }
        String resourceId = key.toString();
        if ("tacz:ammo".equals(resourceId)) {
            String ammoId = resource.getOrCreateTag().getString("AmmoId");
            ResourceLocation parsedAmmoId = ResourceLocation.tryParse(ammoId);
            if (parsedAmmoId != null) {
                resourceId = parsedAmmoId.toString();
            }
        }
        var recipe = ConfiguredAmmoRecipeResolver.select(TaCZinTetra.MODULE_CONFIG, modularGun, gun);
        if (recipe == null || !recipe.cost().containsKey(resourceId)) {
            return 0;
        }
        int capacity = modularGun.getResourceCapacity(gun);
        int current = ResourceNbtAdapter.read(gun.getOrCreateTag(), ITEM_CHANNEL, resourceId);
        int accepted = Math.min(resource.getCount(), Math.max(0, capacity - current));
        if (accepted <= 0) {
            return 0;
        }
        ResourceNbtAdapter.write(gun.getOrCreateTag(), ITEM_CHANNEL, resourceId, current + accepted);
        resource.shrink(accepted);
        return accepted;
    }
}
