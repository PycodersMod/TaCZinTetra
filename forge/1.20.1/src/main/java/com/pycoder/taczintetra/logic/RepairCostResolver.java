package com.pycoder.taczintetra.logic;

import com.pycoder.taczintetra.config.ModuleConfig;

/** 从用户可编辑目录中解析修理材料和单次消耗数量。 */
public final class RepairCostResolver {
    private RepairCostResolver() { }

    public static Result resolve(ModuleConfig config, String slot, String partId, String materialId) {
        if (config == null) return new Result("", 1);
        ModuleConfig.Material material = config.materials().stream()
                .filter(value -> canonicalMaterialId(value.id()).equals(canonicalMaterialId(materialId)))
                .findFirst().orElse(null);
        String agent = material == null ? "" : material.repairAgent();
        ModuleConfig.ConfigEntry repairAgent = config.repairAgents().stream()
                .filter(entry -> canonicalMaterialId(entry.id()).equals(canonicalMaterialId(agent)))
                .findFirst().orElse(null);
        String repairItem = repairAgent == null ? "" : repairAgent.item().trim();
        double agentUnitCost = repairAgent == null
                ? 1
                : repairAgent.stats().getOrDefault("unit_cost", 1d);
        double configured = configValue(config, canonicalSlotId(slot), partId, "repair_count", agentUnitCost);
        int count = (int) Math.min(Integer.MAX_VALUE, Math.max(1, Math.round(configured)));
        return new Result(agent, repairItem, count);
    }

    /** Tetra 将材料键保存为带命名空间的斜杠路径，而 JSON 使用短 ID。 */
    private static String canonicalMaterialId(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) normalized = normalized.substring(slash + 1);
        int colon = normalized.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) normalized = normalized.substring(colon + 1);
        return normalized;
    }

    /** Tetra 可能提供带命名空间的槽位路径，而目录使用简短域名。 */
    private static String canonicalSlotId(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) normalized = normalized.substring(slash + 1);
        int colon = normalized.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) normalized = normalized.substring(colon + 1);
        return normalized;
    }

    private static double configValue(ModuleConfig config, String slot, String partId,
                                       String key, double fallback) {
        return switch (slot == null ? "" : slot) {
            case "body" -> config.bodies().stream().filter(value -> value.id().equals(partId))
                    .findFirst().map(value -> value.stats().getOrDefault(key, fallback)).orElse(fallback);
            case "barrel" -> config.barrels().stream().filter(value -> value.id().equals(partId))
                    .findFirst().map(value -> value.stats().getOrDefault(key, fallback)).orElse(fallback);
            case "magazine" -> config.magazines().stream().filter(value -> value.id().equals(partId))
                    .findFirst().map(value -> value.stats().getOrDefault(key, fallback)).orElse(fallback);
            default -> fallback;
        };
    }

    public record Result(String repairAgent, String repairItem, int count) {
        public Result(String repairAgent, int count) {
            this(repairAgent, "", count);
        }
    }
}
