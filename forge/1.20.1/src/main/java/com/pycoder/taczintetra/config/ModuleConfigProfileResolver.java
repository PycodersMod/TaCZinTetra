package com.pycoder.taczintetra.config;

import com.pycoder.taczintetra.logic.GunProfileResolver;
import com.pycoder.taczintetra.logic.ResourceCapacityCalculator;

import java.util.Map;
import java.util.Collection;

/** 将选中的三个 Tetra 变体转换为纯运行时档案。 */
public final class ModuleConfigProfileResolver {
    private ModuleConfigProfileResolver() {
    }

    /** 创建档案前，分别验证每个主要模型所选的材料。 */
    public static boolean isSelectionValid(ModuleConfig config, String bodyId, String barrelId,
                                           String magazineId, String bodyMaterialId,
                                           String barrelMaterialId, String magazineMaterialId) {
        if (config == null) return false;
        return allowed(config.bodies().stream().filter(value -> value.id().equals(bodyId)).findFirst().orElse(null), bodyMaterialId)
                && allowed(config.barrels().stream().filter(value -> value.id().equals(barrelId)).findFirst().orElse(null), barrelMaterialId)
                && allowed(config.magazines().stream().filter(value -> value.id().equals(magazineId)).findFirst().orElse(null), magazineMaterialId);
    }

    private static boolean allowed(ModuleConfig.Body model, String materialId) {
        return model != null && (model.materials().isEmpty() || model.materials().contains(materialId));
    }

    private static boolean allowed(ModuleConfig.Barrel model, String materialId) {
        return model != null && (model.materials().isEmpty() || model.materials().contains(materialId));
    }

    private static boolean allowed(ModuleConfig.Magazine model, String materialId) {
        return model != null && (model.materials().isEmpty() || model.materials().contains(materialId));
    }

    public static GunProfileResolver.ResolvedGunProfile resolve(ModuleConfig config,
                                                                  String bodyId,
                                                                  String barrelId,
                                                                  String magazineId) {
        if (config == null) config = ModuleConfig.empty();
        return resolve(config, bodyId, barrelId, magazineId, null);
    }

    public static GunProfileResolver.ResolvedGunProfile resolve(ModuleConfig config,
                                                                  String bodyId, String barrelId, String magazineId,
                                                                  String materialId) {
        return resolve(config, bodyId, barrelId, magazineId, materialId, 0);
    }

    public static GunProfileResolver.ResolvedGunProfile resolve(ModuleConfig config,
                                                                  String bodyId, String barrelId, String magazineId,
                                                                  String materialId, int honedCount) {
        return resolveInternal(config, bodyId, barrelId, magazineId,
                materialId, materialId, materialId, honedCount, false);
    }

    /**
     * 解析三个主要模块，并分别选择材料。
     * 有意保留五参数旧版重载，以继续支持
     * 将单一材料应用于整把枪械的旧配置。
     */
    public static GunProfileResolver.ResolvedGunProfile resolve(ModuleConfig config,
                                                                  String bodyId, String barrelId, String magazineId,
                                                                  String bodyMaterialId, String barrelMaterialId,
                                                                  String magazineMaterialId, int honedCount) {
        return resolveInternal(config, bodyId, barrelId, magazineId,
                bodyMaterialId, barrelMaterialId, magazineMaterialId, honedCount, true);
    }

    private static GunProfileResolver.ResolvedGunProfile resolveInternal(ModuleConfig config,
                                                                           String bodyId, String barrelId,
                                                                           String magazineId,
                                                                           String bodyMaterialId,
                                                                           String barrelMaterialId,
                                                                           String magazineMaterialId,
                                                                           int honedCount,
                                                                           boolean independentMaterials) {
        if (config == null) config = ModuleConfig.empty();
        ModuleConfig.Material bodyMaterial = findMaterial(config, bodyMaterialId);
        ModuleConfig.Material barrelMaterial = findMaterial(config, barrelMaterialId);
        ModuleConfig.Material magazineMaterial = findMaterial(config, magazineMaterialId);
        double materialCapacity = magazineMaterial == null ? 1 : magazineMaterial.capacityMultiplier();
        double materialWeight = materialWeight(bodyMaterial, barrelMaterial, magazineMaterial,
                independentMaterials);
        Map<String, Double> bodyValues = config.bodies().stream().filter(v -> v.id().equals(bodyId))
                .findFirst().map(ModuleConfig.Body::stats).orElse(Map.of());
        Map<String, Double> magazineValues = config.magazines().stream().filter(v -> v.id().equals(magazineId))
                .findFirst().map(ModuleConfig.Magazine::stats).orElse(Map.of());
        Map<String, Double> barrelValues = config.barrels().stream().filter(v -> v.id().equals(barrelId))
                .findFirst().map(ModuleConfig.Barrel::stats).orElse(Map.of());
        double loadedMultiplier = value(bodyValues, "loaded_capacity_multiplier", 1);
        double resourceBase = value(magazineValues, "resource_base_capacity",
                value(magazineValues, "capacity_multiplier", 1));
        GunProfileResolver.ResolvedGunProfile base = GunProfileResolver.resolveProfiles(
                stats(bodyValues, loadedMultiplier), stats(barrelValues, 1), stats(magazineValues, resourceBase));
        ModuleConfig.Body body = config.bodies().stream().filter(v -> v.id().equals(bodyId)).findFirst().orElse(null);
        base = applyPolish(config, body, base, honedCount);
        base = applyMaterialWeight(base, materialWeight);
        int resourceCapacity = ResourceCapacityCalculator.resourceMax(
                (int) Math.min(Integer.MAX_VALUE, Math.max(0, Math.round(resourceBase))), 0, 0, materialCapacity);
        int loadedCapacity = ResourceCapacityCalculator.loadedAmmoCapacity(
                (int) Math.min(Integer.MAX_VALUE, Math.max(0, Math.round(resourceBase))), loadedMultiplier);
        return new GunProfileResolver.ResolvedGunProfile(base.fireControl(),
                new GunProfileResolver.FeedProfile(loadedCapacity, resourceCapacity), base.projectile(),
                base.handling(), base.thermal(), base.visual());
    }

    private static GunProfileResolver.ResolvedGunProfile applyPolish(ModuleConfig config,
                                                                       ModuleConfig.Body body,
                                                                       GunProfileResolver.ResolvedGunProfile profile,
                                                                       int honedCount) {
        if (body == null || body.basePolish() == null || body.basePolish().isBlank()) return profile;
        Map<String, Double> values = config.polish().getOrDefault(body.basePolish(), Map.of());
        double damageMultiplier = positiveValue(values, "damage_multiplier", 1);
        double rpmMultiplier = positiveValue(values, "rpm_multiplier", 1);
        double reloadTimeMultiplier = positiveValue(values, "reload_time_multiplier", 1);
        double perHone = positiveValue(values, "honing_count_multiplier", 1);
        int safeCount = Math.max(0, Math.min(1000, honedCount));
        if (safeCount == 0) return profile;
        double honingScale = Math.pow(perHone, safeCount);
        if (!Double.isFinite(honingScale) || honingScale <= 0) honingScale = 1;
        damageMultiplier = boundedMultiplier(damageMultiplier * honingScale);
        rpmMultiplier = boundedMultiplier(rpmMultiplier * honingScale);
        int rpm = Math.max(1, (int) Math.round(profile.fireControl().roundsPerMinute() * rpmMultiplier));
        return new GunProfileResolver.ResolvedGunProfile(
                new GunProfileResolver.FireControlProfile(rpm,
                        profile.fireControl().additiveRoundsPerMinute() * rpmMultiplier),
                profile.feed(),
                new GunProfileResolver.ProjectileProfile(profile.projectile().damage() * damageMultiplier,
                        profile.projectile().velocity(), profile.projectile().range(),
                        profile.projectile().armorIgnore(), profile.projectile().pierce(),
                        profile.projectile().knockback()),
                new GunProfileResolver.HandlingProfile(profile.handling().accuracyAdd(),
                        profile.handling().recoilAdd(), profile.handling().weightMultiplier(),
                        profile.handling().reloadTimeMultiplier() * reloadTimeMultiplier),
                profile.thermal(), profile.visual());
    }

    private static ModuleConfig.Material findMaterial(ModuleConfig config, String materialId) {
        return config.materials().stream().filter(v -> v.id().equals(materialId)).findFirst().orElse(null);
    }

    private static double materialWeight(ModuleConfig.Material body, ModuleConfig.Material barrel,
                                          ModuleConfig.Material magazine, boolean independent) {
        if (!independent) return body == null ? 1 : value(body.stats(), "weight", 1);
        return value(body == null ? Map.of() : body.stats(), "weight", 1)
                * value(barrel == null ? Map.of() : barrel.stats(), "weight", 1)
                * value(magazine == null ? Map.of() : magazine.stats(), "weight", 1);
    }

    private static GunProfileResolver.ResolvedGunProfile applyMaterialWeight(
            GunProfileResolver.ResolvedGunProfile profile, double materialWeight) {
        double safeWeight = positiveValue(Map.of("weight", materialWeight), "weight", 1);
        var handling = profile.handling();
        return new GunProfileResolver.ResolvedGunProfile(profile.fireControl(), profile.feed(), profile.projectile(),
                new GunProfileResolver.HandlingProfile(handling.accuracyAdd(), handling.recoilAdd(),
                        handling.weightMultiplier() * safeWeight, handling.reloadTimeMultiplier()),
                profile.thermal(), profile.visual());
    }

    public static GunProfileResolver.ResolvedGunProfile applyAttachments(ModuleConfig config,
                                                                           GunProfileResolver.ResolvedGunProfile base,
                                                                           boolean stock, boolean grip, boolean optic) {
        return applyAttachments(config, base, stock ? "stock" : null,
                grip ? "grip" : null, optic ? "optic" : null);
    }

    /** 为每个已安装的附件模型应用对应配置。 */
    public static GunProfileResolver.ResolvedGunProfile applyAttachments(ModuleConfig config,
                                                                           GunProfileResolver.ResolvedGunProfile base,
                                                                           String stockId, String gripId,
                                                                           String opticId) {
        if (config == null) config = ModuleConfig.empty();
        if (base == null) return GunProfileResolver.resolveProfiles(null, null, null);
        double recoilMultiplier = attachmentValue(config, "stock", "recoil_multiplier", 1, stockId);
        double handlingMultiplier = attachmentValue(config, "grip", "handling_multiplier", 1, gripId);
        double zoom = attachmentValue(config, "optic", "aiming_zoom", 1, opticId);
        double recoil = base.handling().recoilAdd() * (stockId != null ? recoilMultiplier : 1);
        double accuracy = base.handling().accuracyAdd() * (gripId != null ? handlingMultiplier : 1);
        return new GunProfileResolver.ResolvedGunProfile(base.fireControl(), base.feed(), base.projectile(),
                new GunProfileResolver.HandlingProfile(accuracy, recoil,
                        base.handling().weightMultiplier(), base.handling().reloadTimeMultiplier()), base.thermal(),
                new GunProfileResolver.VisualProfile(Math.max(0.01, zoom)));
    }

    /** 仅应用已配置且容量有效的特殊嵌片档案修饰项。 */
    public static GunProfileResolver.ResolvedGunProfile applySpecialInlays(
            ModuleConfig config, GunProfileResolver.ResolvedGunProfile base,
            Collection<String> requestedIds) {
        if (config == null) config = ModuleConfig.empty();
        if (base == null || requestedIds == null || requestedIds.isEmpty()) return base;
        var active = SpecialInlayPolicy.resolve(requestedIds, config).activeIds();
        if (active.isEmpty()) return base;
        double damageMultiplier = 1;
        double rpmMultiplier = 1;
        double velocityMultiplier = 1;
        double rangeMultiplier = 1;
        double knockbackMultiplier = 1;
        double accuracyAdd = 0;
        double recoilAdd = 0;
        double weightMultiplier = 1;
        double reloadTimeMultiplier = 1;
        double armorIgnoreAdd = 0;
        int pierceAdd = 0;
        for (String id : active) {
            var entry = config.specialInlays().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
            if (entry == null) continue;
            Map<String, Double> stats = entry.stats();
            damageMultiplier *= positiveValue(stats, "damage_multiplier", 1);
            rpmMultiplier *= positiveValue(stats, "rpm_multiplier", 1);
            velocityMultiplier *= positiveValue(stats, "velocity_multiplier", 1);
            rangeMultiplier *= positiveValue(stats, "range_multiplier", 1);
            knockbackMultiplier *= positiveValue(stats, "knockback_multiplier", 1);
            weightMultiplier *= positiveValue(stats, "weight_multiplier", 1);
            reloadTimeMultiplier *= positiveValue(stats, "reload_time_multiplier", 1);
            accuracyAdd += value(stats, "accuracy_add", 0);
            recoilAdd += value(stats, "recoil_add", 0);
            armorIgnoreAdd += value(stats, "armor_ignore_add", 0);
            pierceAdd += (int) Math.max(0, Math.round(value(stats, "pierce_add", 0)));
        }
        var projectile = base.projectile();
        var handling = base.handling();
        int rpm = Math.max(1, (int) Math.round(base.fireControl().roundsPerMinute() * boundedMultiplier(rpmMultiplier)));
        return new GunProfileResolver.ResolvedGunProfile(
                new GunProfileResolver.FireControlProfile(rpm,
                        base.fireControl().additiveRoundsPerMinute() * boundedMultiplier(rpmMultiplier)),
                base.feed(),
                new GunProfileResolver.ProjectileProfile(
                        projectile.damage() * boundedMultiplier(damageMultiplier),
                        projectile.velocity() * boundedMultiplier(velocityMultiplier),
                        projectile.range() * boundedMultiplier(rangeMultiplier),
                        Math.max(0, Math.min(1, projectile.armorIgnore() + armorIgnoreAdd)),
                        Math.max(0, projectile.pierce() + pierceAdd),
                        projectile.knockback() * boundedMultiplier(knockbackMultiplier)),
                new GunProfileResolver.HandlingProfile(
                        handling.accuracyAdd() + accuracyAdd,
                        handling.recoilAdd() + recoilAdd,
                        handling.weightMultiplier() * boundedMultiplier(weightMultiplier),
                        handling.reloadTimeMultiplier() * boundedMultiplier(reloadTimeMultiplier)),
                base.thermal(), base.visual());
    }

    private static double attachmentValue(ModuleConfig config, String slot, String key,
                                           double fallback, String selectedId) {
        if (selectedId == null || selectedId.isBlank()) return fallback;
        var selected = config.attachments().stream()
                .filter(v -> slot.equals(v.slot()) && selectedId.equals(v.id()))
                .findFirst();
        if (selected.isEmpty() && selectedId.equals(slot)) {
            selected = config.attachments().stream().filter(v -> slot.equals(v.slot())).findFirst();
        }
        return selected.map(v -> value(v.stats(), key, fallback))
                .filter(v -> Double.isFinite(v) && v > 0).orElse(fallback);
    }

    private static GunProfileResolver.PartStats stats(Map<String, Double> values, double capacityFallback) {
        return new GunProfileResolver.PartStats(
                value(values, "damage_add", value(values, "damage", 0)),
                value(values, "rpm_add", value(values, "rpm", 0)),
                value(values, "velocity_multiplier", value(values, "velocity", 1)),
                value(values, "capacity_multiplier", capacityFallback),
                value(values, "accuracy_add", 0), value(values, "recoil_add", 0),
                value(values, "damage_multiplier", 1), value(values, "rpm_multiplier", 1),
                value(values, "range_multiplier", value(values, "range", 1)),
                value(values, "armor_ignore", 0),
                (int) Math.max(0, Math.round(value(values, "pierce", 0))),
                value(values, "knockback_multiplier", value(values, "knockback", 1)),
                value(values, "weight_multiplier", value(values, "weight", 1)),
                value(values, "reload_time_multiplier", value(values, "reload_time", 1)));
    }

    private static double value(Map<String, Double> values, String key, double fallback) {
        Double value = values.get(key);
        return value != null && Double.isFinite(value) ? value : fallback;
    }

    private static double positiveValue(Map<String, Double> values, String key, double fallback) {
        double result = value(values, key, fallback);
        return result > 0 ? result : fallback;
    }

    private static double boundedMultiplier(double value) {
        return Double.isFinite(value) && value > 0 ? Math.min(1_000_000, value) : 1;
    }
}
