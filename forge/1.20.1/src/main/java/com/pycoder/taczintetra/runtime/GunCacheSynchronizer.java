package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.pycoder.taczintetra.network.StackIdentity;

/** Keeps TaCZ's property cache valid after a Tetra stack enters the active hand. */
public final class GunCacheSynchronizer {
    private static final Map<LivingEntity, CacheBinding> BINDINGS = new WeakHashMap<>();

    private GunCacheSynchronizer() {
    }

    public static synchronized void ensure(LivingEntity entity, ItemStack stack) {
        if (!(stack.getItem() instanceof ModularGunItem gun)) return;
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        CacheBinding binding = BINDINGS.get(entity);
        ItemStack previousStack = binding == null ? null : binding.stack();
        int identity = StackIdentity.of(stack);
        // TaCZ exposes one operator cache per entity, so a non-null cache is
        // not proof that it belongs to this hand's current modular stack.
        if (operator.getCacheProperty() != null
                && previousStack == stack
                && binding.identity() == identity) return;

        AttachmentPropertyManager.postChangeEvent(entity, stack);
        if (operator.getCacheProperty() != null) return;

        TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).ifPresent(index -> {
            AttachmentCacheProperty cache = new AttachmentCacheProperty();
            cache.eval(stack, index.getGunData());
            operator.updateCacheProperty(cache);
        });
        BINDINGS.put(entity, new CacheBinding(stack, identity));
    }

    private record CacheBinding(ItemStack stack, int identity) { }
}
