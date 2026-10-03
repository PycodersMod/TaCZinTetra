package com.pycoder.taczintetra.runtime;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;

import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** 将逐手换弹所有权原语接入 TaCZ 实体生命周期。 */
public final class ReloadRuntimeCoordinator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<LivingEntity, DualHandRuntimeState> STATES = new WeakHashMap<>();

    private ReloadRuntimeCoordinator() { }

    public static synchronized boolean otherHandReloading(LivingEntity entity, InteractionHand hand) {
        if (entity == null || hand == null) return false;
        DualHandRuntimeState state = STATES.get(entity);
        if (state == null) return false;
        InteractionHand other = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return state.context(other).reloading();
    }

    public static synchronized boolean handReloading(LivingEntity entity, InteractionHand hand) {
        if (entity == null || hand == null) return false;
        DualHandRuntimeState state = STATES.get(entity);
        return state != null && state.context(hand).reloading();
    }

    public static synchronized boolean begin(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (entity == null || hand == null || stack == null || stack.isEmpty()) return false;
        com.pycoder.taczintetra.network.StackIdentity.ensureUniqueId(stack);
        DualHandRuntimeState state = STATES.computeIfAbsent(entity, ignored -> new DualHandRuntimeState());
        PerHandWeaponContext context = state.context(hand);
        state.updateStackIdentity(hand, com.pycoder.taczintetra.network.StackIdentity.of(stack));
        return state.beginReload(context, stack);
    }

    public static synchronized void reconcile(LivingEntity entity) {
        if (entity == null) return;
        DualHandRuntimeState state = STATES.get(entity);
        if (state == null) return;
        boolean interrupted = state.updateStackIdentity(InteractionHand.MAIN_HAND,
                com.pycoder.taczintetra.network.StackIdentity.of(entity.getMainHandItem()));
        interrupted |= state.updateStackIdentity(InteractionHand.OFF_HAND,
                com.pycoder.taczintetra.network.StackIdentity.of(entity.getOffhandItem()));
        state.reconcile();
        if (interrupted) {
            var holder = IGunOperator.fromLivingEntity(entity).getDataHolder();
            holder.currentGunItem = entity::getMainHandItem;
            holder.reloadStateType = ReloadState.StateType.NOT_RELOADING;
            holder.reloadTimestamp = -1L;
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev reload interrupted by stack identity change; native holder reset");
            }
        }
    }

    public static synchronized void complete(LivingEntity entity, InteractionHand hand) {
        DualHandRuntimeState state = STATES.get(entity);
        if (state != null) state.completeReload(state.context(hand));
    }

    public static synchronized void interrupt(LivingEntity entity, InteractionHand hand) {
        DualHandRuntimeState state = STATES.get(entity);
        if (state != null) state.interruptReload(state.context(hand));
    }

    private static InteractionHand handFor(LivingEntity entity, ItemStack stack) {
        if (entity == null || stack == null) return null;
        if (entity.getMainHandItem() == stack) return InteractionHand.MAIN_HAND;
        if (entity.getOffhandItem() == stack) return InteractionHand.OFF_HAND;
        int identity = com.pycoder.taczintetra.network.StackIdentity.of(stack);
        int mainIdentity = com.pycoder.taczintetra.network.StackIdentity.of(entity.getMainHandItem());
        int offIdentity = com.pycoder.taczintetra.network.StackIdentity.of(entity.getOffhandItem());
        if (identity == mainIdentity && identity != offIdentity) return InteractionHand.MAIN_HAND;
        if (identity == offIdentity && identity != mainIdentity) return InteractionHand.OFF_HAND;
        return null;
    }

    public static InteractionHand handForStack(LivingEntity entity, ItemStack stack) {
        return handFor(entity, stack);
    }
}
