package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.ReloadSettlementPolicy;
import com.pycoder.taczintetra.logic.ResourceAccount;
import com.pycoder.taczintetra.api.ExternalCapabilityAdapter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.pycoder.taczintetra.logic.ExternalResourceTransactionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Commits resources and dummy ammo only after TaCZ reports reload completion. */
public final class ReloadRuntimeSettlementService {
    private static final String PENDING = "taczintetra_reload_pending";
    private static final String FILL_MODE = "taczintetra_reload_fill";
    private static final String RELOAD_STARTED_AT = "taczintetra_reload_started_at";
    private static final String RELOAD_STATE = "taczintetra_reload_state";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReloadRuntimeSettlementService() { }

    public static void markPending(ItemStack stack, boolean fillMode) {
        if (stack != null && !stack.isEmpty()) {
            stack.getOrCreateTag().putBoolean(PENDING, true);
            stack.getOrCreateTag().putBoolean(FILL_MODE, fillMode);
        }
    }

    public static void clearPending(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            stack.getOrCreateTag().remove(PENDING);
            stack.getOrCreateTag().remove(FILL_MODE);
        }
    }

    /** Clears every TiT reload marker from the stack that owned the transaction. */
    public static void clearRuntimeState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        clearPending(stack);
        var tag = stack.getOrCreateTag();
        tag.remove(RELOAD_STARTED_AT);
        tag.remove(RELOAD_STATE);
    }

    public static boolean isPending(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getOrCreateTag().getBoolean(PENDING);
    }

    public static boolean settleIfComplete(ModularGunItem gun, ItemStack stack) {
        return settleIfComplete(gun, stack, null);
    }

    public static boolean settleIfComplete(ModularGunItem gun, ItemStack stack, LivingEntity holder) {
        if (gun == null || stack == null || !isPending(stack)) return false;
        boolean fillMode = stack.getOrCreateTag().getBoolean(FILL_MODE);
        clearPending(stack);
        var modules = TetraItemStackProfileResolver.selectedModules(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (modules == null) return false;
        var recipe = ConfiguredAmmoRecipeResolver.select(TaCZinTetra.MODULE_CONFIG, gun, stack);
        if (recipe == null) return false;

        ResourceAccount resources = ResourceAccount.empty();
        for (String id : recipe.cost().keySet()) {
            ExternalCapabilityAdapter adapter = ExternalResourceService.adapterFor(id, holder);
            int amount = adapter == null
                    ? com.pycoder.taczintetra.logic.ResourceNbtAdapter.read(stack.getOrCreateTag(),
                    com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, id)
                    : Math.max(0, adapter.available(holder, id));
            resources = resources.insert(id, amount, Integer.MAX_VALUE).account();
        }
        ReloadSettlementPolicy.Result result = ReloadSettlementPolicy.settle(true,
                gun.getMaxDummyAmmoAmount(stack), gun.getCurrentAmmoCount(stack), recipe,
                resources, fillMode);
        if (result.paidBatches() <= 0) return false;
        Map<String, ExternalCapabilityAdapter> adapters = new LinkedHashMap<>();
        int externalOperations = 0;
        boolean allSupportRollback = true;
        for (String id : recipe.cost().keySet()) {
            ExternalCapabilityAdapter adapter = ExternalResourceService.adapterFor(id, holder);
            if (adapter != null) {
                adapters.put(id, adapter);
                externalOperations++;
                allSupportRollback &= adapter.supportsRollback();
            }
        }
        if (!ExternalResourceTransactionPolicy.canBegin(externalOperations, allSupportRollback)) {
            LOGGER.warn("Refusing non-atomic reload settlement with {} external resource debits", externalOperations);
            return false;
        }
        // Preflight every external backend before mutating any one of them.
        for (var entry : recipe.cost().entrySet()) {
            ExternalCapabilityAdapter adapter = adapters.get(entry.getKey());
            int consumed = entry.getValue() * result.paidBatches();
            if (adapter != null && !adapter.canExtract(holder, entry.getKey(), consumed)) return false;
        }
        List<ExternalDebit> debited = new ArrayList<>();
        for (var entry : recipe.cost().entrySet()) {
            int consumed = entry.getValue() * result.paidBatches();
            ExternalCapabilityAdapter adapter = adapters.get(entry.getKey());
            if (adapter != null) {
                int extracted = adapter.extract(holder, entry.getKey(), consumed);
                if (extracted != consumed) {
                    if (extracted > 0 && adapter.supportsRollback()
                            && !ExternalResourceService.rollback(adapter, holder, entry.getKey(), extracted)) {
                        LOGGER.error("External reload settlement could not roll back partial debit of {} from {}",
                                extracted, entry.getKey());
                    }
                    rollbackDebits(holder, debited);
                    return false;
                }
                debited.add(new ExternalDebit(adapter, entry.getKey(), extracted));
            } else {
                com.pycoder.taczintetra.logic.ResourceNbtAdapter.write(stack.getOrCreateTag(),
                        ResourceInsertionService.ITEM_CHANNEL, entry.getKey(), result.resources().amount(entry.getKey()));
            }
        }
        gun.setCurrentAmmoCount(stack, result.loadedRounds());
        return true;
    }

    private static void rollbackDebits(LivingEntity holder, List<ExternalDebit> debited) {
        for (int index = debited.size() - 1; index >= 0; index--) {
            ExternalDebit debit = debited.get(index);
            if (!ExternalResourceService.rollback(debit.adapter(), holder, debit.resourceId(), debit.amount())) {
                LOGGER.error("External reload settlement could not roll back {} of {}",
                        debit.amount(), debit.resourceId());
            }
        }
    }

    private record ExternalDebit(ExternalCapabilityAdapter adapter, String resourceId, int amount) { }
}
