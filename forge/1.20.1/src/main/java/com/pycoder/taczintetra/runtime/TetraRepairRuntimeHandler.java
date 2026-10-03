package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.RepairCostResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import se.mickelus.tetra.items.modular.GatherRepairInstancesEvent;
import se.mickelus.tetra.module.schematic.RepairDefinition;
import se.mickelus.tetra.module.schematic.RepairInstance;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** 筛选 Tetra 针对单个物品栈的修复选项，不修改共享注册表定义。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class TetraRepairRuntimeHandler {
    private TetraRepairRuntimeHandler() {
    }

    @SubscribeEvent
    public static void filterRepairInstances(GatherRepairInstancesEvent event) {
        ItemStack stack = event.itemStack;
        if (!(stack.getItem() instanceof ModularGunItem gun)) {
            return;
        }
        TetraItemStackProfileResolver.SelectedModules modules = TetraItemStackProfileResolver.selectedModules(
                gun, stack, GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (modules == null) {
            return;
        }
        event.instances = Arrays.stream(event.instances)
                .map(instance -> filterInstance(instance, modules))
                .toArray(RepairInstance[]::new);
    }

    private static RepairInstance filterInstance(RepairInstance instance,
                                                  TetraItemStackProfileResolver.SelectedModules modules) {
        String slot = slotOf(instance.module() == null ? "" : instance.module().getKey());
        String partId = variantOf(instance);
        String materialId = materialIdForSlot(slot, modules);
        RepairCostResolver.Result expected = RepairCostResolver.resolve(
                TaCZinTetra.MODULE_CONFIG, slot, partId, materialId);
        if (!shouldFilterRepairDefinitions(
                TaczInTetraForgeConfig.ONLY_REPAIR_AGENTS.get(), expected.repairItem())) {
            return instance;
        }
        List<RepairDefinition> filtered = instance.definitions().stream()
                .filter(definition -> usesRepairItem(definition, expected.repairItem()))
                .toList();
        return new RepairInstance(filtered, instance.module());
    }

    static boolean shouldFilterRepairDefinitions(boolean onlyRepairAgents, String repairItem) {
        return onlyRepairAgents && repairItem != null && !repairItem.isBlank();
    }

    /** 返回当前正在修理的主要模块对应的材料。 */
    static String materialIdForSlot(String slot,
                                    TetraItemStackProfileResolver.SelectedModules modules) {
        if (modules == null || slot == null) return "";
        return switch (slot) {
            case "body" -> modules.body().materialId();
            case "barrel" -> modules.barrel().materialId();
            case "magazine" -> modules.magazine().materialId();
            default -> "";
        };
    }

    private static boolean usesRepairItem(RepairDefinition definition, String repairItem) {
        if (definition == null || definition.material == null) {
            return false;
        }
        ResourceLocation expected = ResourceLocation.tryParse(repairItem);
        if (expected == null) return false;
        return Arrays.stream(definition.material.getApplicableItemStacks())
                .map(ItemStack::getItem)
                .map(BuiltInRegistries.ITEM::getKey)
                .anyMatch(expected::equals);
    }

    private static String slotOf(String moduleKey) {
        if (moduleKey == null || moduleKey.isBlank()) return "";
        String[] parts = moduleKey.split("/");
        // Tetra 6.17 将拆分模块注册为 namespace/slot/variant。
        // variant 并不是修理类别（body/barrel/magazine）。
        for (int i = 0; i < parts.length - 1; i++) {
            if ("body".equals(parts[i]) || "barrel".equals(parts[i]) || "magazine".equals(parts[i])) {
                return parts[i];
            }
        }
        return parts[parts.length - 1];
    }

    private static String variantOf(RepairInstance instance) {
        if (instance.module() != null) {
            String key = instance.module().getKey();
            if (key != null && key.contains("/")) {
                String[] parts = key.split("/");
                return parts[parts.length - 1];
            }
        }
        return instance.definitions().stream()
                .findFirst()
                .map(definition -> definition.moduleVariant == null
                        ? "" : definition.moduleVariant.replace("/", ""))
                .orElse("");
    }
}
