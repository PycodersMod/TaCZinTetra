package com.pycoder.taczintetra.logic;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 确定性的静态档案计算。运行时热量/弹药状态
 * 保留在此解析器之外，避免频繁 tick 使静态缓存失效。
 */
public final class GunProfileResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private GunProfileResolver() {
    }

    public static Result resolve(PartStats body, PartStats barrel, PartStats magazine) {
        PartStats b = safe(body);
        PartStats r = safe(barrel);
        PartStats m = safe(magazine);
        double damage = (b.damageAdd + r.damageAdd + m.damageAdd)
                * b.damageMultiplier * r.damageMultiplier * m.damageMultiplier;
        double rpm = (b.rpmAdd + r.rpmAdd + m.rpmAdd)
                * b.rpmMultiplier * r.rpmMultiplier * m.rpmMultiplier;
        double velocity = b.velocityMultiplier * r.velocityMultiplier * m.velocityMultiplier;
        double capacity = b.capacityMultiplier * r.capacityMultiplier * m.capacityMultiplier;
        return new Result(roundMin(damage), Math.max(1, (int) Math.round(rpm)),
                roundMin(velocity), Math.max(1, (int) Math.round(capacity)));
    }

    public static ResolvedGunProfile resolveProfiles(PartStats body, PartStats barrel, PartStats magazine) {
        Result result = resolve(body, barrel, magazine);
        PartStats b = safe(body), r = safe(barrel), m = safe(magazine);
        return new ResolvedGunProfile(
                new FireControlProfile(result.roundsPerMinute(), b.rpmAdd() + r.rpmAdd() + m.rpmAdd()),
                new FeedProfile(result.loadedCapacity()),
                new ProjectileProfile(result.damage(), result.velocity(),
                        b.rangeMultiplier() * r.rangeMultiplier() * m.rangeMultiplier(),
                        b.armorIgnore() + r.armorIgnore() + m.armorIgnore(),
                        b.pierce() + r.pierce() + m.pierce(),
                        b.knockbackMultiplier() * r.knockbackMultiplier() * m.knockbackMultiplier()),
                new HandlingProfile(b.accuracyAdd() + r.accuracyAdd() + m.accuracyAdd(),
                        b.recoilAdd() + r.recoilAdd() + m.recoilAdd(),
                        b.weightMultiplier() * r.weightMultiplier() * m.weightMultiplier(),
                        b.reloadTimeMultiplier() * r.reloadTimeMultiplier() * m.reloadTimeMultiplier()),
                new ThermalProfile(0, false), new VisualProfile(1));
    }

    private static PartStats safe(PartStats value) {
        if (value == null) {
            return new PartStats(0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1);
        }
        if (!isValid(value)) {
            LOGGER.warn("Invalid TaCZ in Tetra gun part configuration; applying safe defaults");
        }
        return new PartStats(Math.max(0, finite(value.damageAdd())),
                Math.max(0, finite(value.rpmAdd())),
                positiveOrDefault(value.velocityMultiplier(), 1),
                positiveOrDefault(value.capacityMultiplier(), 1),
                finite(value.accuracyAdd()), finite(value.recoilAdd()),
                positiveOrDefault(value.damageMultiplier(), 1),
                positiveOrDefault(value.rpmMultiplier(), 1),
                positiveOrDefault(value.rangeMultiplier(), 1),
                Math.max(0, Math.min(1, finite(value.armorIgnore()))),
                Math.max(0, value.pierce()),
                positiveOrDefault(value.knockbackMultiplier(), 1),
                positiveOrDefault(value.weightMultiplier(), 1),
                positiveOrDefault(value.reloadTimeMultiplier(), 1));
    }

    private static boolean isValid(PartStats value) {
        return Double.isFinite(value.damageAdd()) && value.damageAdd() >= 0
                && Double.isFinite(value.rpmAdd()) && value.rpmAdd() >= 0
                && Double.isFinite(value.velocityMultiplier()) && value.velocityMultiplier() > 0
                && Double.isFinite(value.capacityMultiplier()) && value.capacityMultiplier() > 0
                && Double.isFinite(value.accuracyAdd()) && Double.isFinite(value.recoilAdd())
                && Double.isFinite(value.damageMultiplier()) && value.damageMultiplier() > 0
                && Double.isFinite(value.rpmMultiplier()) && value.rpmMultiplier() > 0
                && Double.isFinite(value.rangeMultiplier()) && value.rangeMultiplier() > 0
                && Double.isFinite(value.armorIgnore()) && value.armorIgnore() >= 0 && value.armorIgnore() <= 1
                && value.pierce() >= 0
                && Double.isFinite(value.knockbackMultiplier()) && value.knockbackMultiplier() > 0
                && Double.isFinite(value.weightMultiplier()) && value.weightMultiplier() > 0
                && Double.isFinite(value.reloadTimeMultiplier()) && value.reloadTimeMultiplier() > 0;
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0;
    }

    private static double positiveOrDefault(double value, double fallback) {
        return Double.isFinite(value) && value > 0 ? value : fallback;
    }

    private static double roundMin(double value) {
        if (!Double.isFinite(value)) {
            return 0.01;
        }
        return Math.max(0.01, Math.round(value * 100.0) / 100.0);
    }

    public record PartStats(double damageAdd, double rpmAdd, double velocityMultiplier,
                            double capacityMultiplier, double accuracyAdd, double recoilAdd,
                            double damageMultiplier, double rpmMultiplier, double rangeMultiplier,
                            double armorIgnore, int pierce, double knockbackMultiplier,
                            double weightMultiplier, double reloadTimeMultiplier) {
        public PartStats(double damageAdd, double rpmAdd, double velocityMultiplier,
                         double capacityMultiplier, double accuracyAdd, double recoilAdd,
                         double damageMultiplier, double rpmMultiplier, double rangeMultiplier,
                         double armorIgnore, int pierce, double knockbackMultiplier) {
            this(damageAdd, rpmAdd, velocityMultiplier, capacityMultiplier, accuracyAdd, recoilAdd,
                    damageMultiplier, rpmMultiplier, rangeMultiplier, armorIgnore, pierce,
                    knockbackMultiplier, 1, 1);
        }

        public PartStats(double damageAdd, double rpmAdd, double velocityMultiplier,
                         double capacityMultiplier, double accuracyAdd, double recoilAdd,
                         double damageMultiplier, double rpmMultiplier) {
            this(damageAdd, rpmAdd, velocityMultiplier, capacityMultiplier, accuracyAdd, recoilAdd,
                    damageMultiplier, rpmMultiplier, 1, 0, 0, 1, 1, 1);
        }
    }

    public record Result(double damage, int roundsPerMinute, double velocity, int loadedCapacity) {
    }

    public record ResolvedGunProfile(FireControlProfile fireControl, FeedProfile feed,
                                     ProjectileProfile projectile, HandlingProfile handling,
                                     ThermalProfile thermal, VisualProfile visual) { }
    public record FireControlProfile(int roundsPerMinute, double additiveRoundsPerMinute) { }
    public record FeedProfile(int loadedCapacity, int resourceCapacity) {
        public FeedProfile(int loadedCapacity) {
            this(loadedCapacity, loadedCapacity);
        }
    }
    public record ProjectileProfile(double damage, double velocity, double range,
                                    double armorIgnore, int pierce, double knockback) { }
    public record HandlingProfile(double accuracyAdd, double recoilAdd,
                                  double weightMultiplier, double reloadTimeMultiplier) {
        public HandlingProfile(double accuracyAdd, double recoilAdd) {
            this(accuracyAdd, recoilAdd, 1, 1);
        }
    }
    public record ThermalProfile(double heat, boolean overheatLocked) { }
    public record VisualProfile(double aimingZoom) { }
}
