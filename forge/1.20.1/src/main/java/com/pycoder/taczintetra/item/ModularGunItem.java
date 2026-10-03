package com.pycoder.taczintetra.item;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.logic.AmmoStatePolicy;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.pycoder.taczintetra.logic.HeatModel;
import com.pycoder.taczintetra.logic.HeatCurve;
import com.pycoder.taczintetra.logic.ShotCountCalculator;
import com.pycoder.taczintetra.logic.GunStateRevalidator;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import se.mickelus.tetra.items.modular.ItemModularHandheld;
import se.mickelus.tetra.gui.GuiModuleOffsets;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tetra-backed firearm root. Runtime TaCZ behaviour is deliberately kept in
 * services so module data and per-stack state do not leak into the Item class.
 */
public class ModularGunItem extends ItemModularHandheld implements IGun {
    private static final String AMMO = "taczintetra_ammo";
    private static final String FIRE_MODE = "taczintetra_fire_mode";
    private static final String BULLET = "taczintetra_bullet";
    private static final String HEAT = "taczintetra_heat";
    private static final String OVERHEAT = "taczintetra_overheat";
    private static final String HEAT_TICK = "taczintetra_heat_tick";
    private static final ResourceLocation GUN_ID = ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "modular_gun");
    private static final ResourceLocation DISPLAY_ID = ResourceLocation.fromNamespaceAndPath("tacz", "modular_gun_display");
    /** Cooling state must not mutate the held stack every server tick. */
    private static final Map<ItemStack, Float> RUNTIME_HEAT = Collections.synchronizedMap(new WeakHashMap<>());
    /** Separate lazy-cooling anchor; persisted NBT remains write-on-shot. */
    private static final Map<ItemStack, Long> RUNTIME_HEAT_TICK = Collections.synchronizedMap(new WeakHashMap<>());

    public ModularGunItem(Properties properties) {
        super(properties);
        this.majorModuleKeys = new String[] {
                GunModuleSlots.BODY, GunModuleSlots.MAGAZINE, GunModuleSlots.BARREL
        };
        this.minorModuleKeys = new String[] {
                GunModuleSlots.STOCK, GunModuleSlots.OPTIC, GunModuleSlots.GRIP, GunModuleSlots.SPECIAL
        };
        this.requiredModules = new String[] {
                GunModuleSlots.BODY, GunModuleSlots.MAGAZINE, GunModuleSlots.BARREL
        };
        refreshConfiguredDurability();
        this.canHone = true;
    }

    /**
     * Tetra 6.17 only ships default minor layouts for zero through three slots.
     * Keep the fourth configured slot visible with an explicit, non-overlapping
     * layout instead of indexing Tetra's three-slot default array.
     */
    @Override
    public GuiModuleOffsets getMinorGuiOffsets(ItemStack stack) {
        if (getNumMinorModules(stack) == 4) {
            return new GuiModuleOffsets(-12, -1, -21, 12, -12, 25, 4, 12);
        }
        return super.getMinorGuiOffsets(stack);
    }

    /** Refreshes Tetra's inherited base values after the editable catalog reloads. */
    public void refreshConfiguredDurability() {
        this.baseDurability = configuredInt("base_durability", 100);
        this.baseIntegrity = configuredInt("base_integrity", 1);
    }

    @Override public float getAimingZoom(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        return profile == null ? 1.0f : (float) Math.max(0.01, profile.visual().aimingZoom());
    }
    @Override public boolean useDummyAmmo(ItemStack stack) { return true; }
    @Override public int getDummyAmmoAmount(ItemStack stack) { return Math.max(0, stack.getOrCreateTag().getInt(AMMO)); }
    @Override public void setDummyAmmoAmount(ItemStack stack, int amount) { stack.getOrCreateTag().putInt(AMMO, Math.max(0, amount)); }
    @Override public void addDummyAmmoAmount(ItemStack stack, int amount) { setDummyAmmoAmount(stack, AmmoStatePolicy.addSaturated(getDummyAmmoAmount(stack), amount)); }
    @Override public boolean hasMaxDummyAmmo(ItemStack stack) { return true; }
    @Override public int getMaxDummyAmmoAmount(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile != null && profile.feed().loadedCapacity() > 0) return profile.feed().loadedCapacity();
        int configured = configuredInt("resource_base_capacity", 15);
        return AmmoStatePolicy.sanitizeMax(stack.getOrCreateTag().getInt("taczintetra_max_ammo"), configured);
    }
    public int getResourceCapacity(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        return profile == null ? getMaxDummyAmmoAmount(stack) : profile.feed().resourceCapacity();
    }
    @Override public void setMaxDummyAmmoAmount(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("taczintetra_max_ammo", AmmoStatePolicy.sanitizeMax(amount, 0));
    }
    @Override public boolean hasAttachmentLock(ItemStack stack) { return false; }

    @Override public boolean acceptsEnchantment(ItemStack stack, Enchantment enchantment, boolean primary) {
        if (enchantment == null) return false;
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        return id != null && com.pycoder.taczintetra.logic.EnchantmentPolicy.isAllowed(id.toString(), TaCZinTetra.MODULE_CONFIG);
    }

    @Override public void setAttachmentLock(ItemStack stack, boolean locked) { }
    @Override public ResourceLocation getGunId(ItemStack stack) { return GUN_ID; }
    @Override public void setGunId(ItemStack stack, ResourceLocation id) { }
    @Override public ResourceLocation getGunDisplayId(ItemStack stack) { return DISPLAY_ID; }
    @Override public void setGunDisplayId(ItemStack stack, ResourceLocation id) { }
    @Override public int getLevel(int exp) { return 0; }
    @Override public int getExp(int level) { return 0; }
    @Override public int getMaxLevel() { return 0; }
    @Override public int getLevel(ItemStack stack) { return 0; }
    @Override public int getExp(ItemStack stack) { return 0; }
    @Override public int getExpToNextLevel(ItemStack stack) { return 0; }
    @Override public int getExpCurrentLevel(ItemStack stack) { return 0; }
    @Override public FireMode getFireMode(ItemStack stack) {
        try { return FireMode.valueOf(stack.getOrCreateTag().getString(FIRE_MODE)); }
        catch (IllegalArgumentException ignored) { return FireMode.SEMI; }
    }
    @Override public void setFireMode(ItemStack stack, FireMode mode) { stack.getOrCreateTag().putString(FIRE_MODE, mode == null ? FireMode.SEMI.name() : mode.name()); }
    @Override public int getCurrentAmmoCount(ItemStack stack) {
        revalidateRuntimeState(stack);
        return getDummyAmmoAmount(stack);
    }
    @Override public void setCurrentAmmoCount(ItemStack stack, int count) { setDummyAmmoAmount(stack, count); }

    /** Revalidates persisted ammo/resource state against the current module profile. */
    public void revalidateRuntimeState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        int maxAmmo = getMaxDummyAmmoAmount(stack);
        int maxResource = getResourceCapacity(stack);
        var recipe = ConfiguredAmmoRecipeResolver.select(TaCZinTetra.MODULE_CONFIG, this, stack);
        var state = GunStateRevalidator.revalidate(new GunStateRevalidator.State(
                maxAmmo, getDummyAmmoAmount(stack), maxResource, 0, java.util.List.of("semi"), "semi"));
        if (state.currentAmmo() != getDummyAmmoAmount(stack)) {
            setDummyAmmoAmount(stack, state.currentAmmo());
        }
        if (recipe != null) {
            for (String resourceId : recipe.cost().keySet()) {
                int stored = ResourceNbtAdapter.read(stack.getOrCreateTag(),
                        com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, resourceId);
                int clamped = Math.min(stored, state.maxCapacity());
                if (stored != clamped) {
                    ResourceNbtAdapter.write(stack.getOrCreateTag(),
                            com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL,
                            resourceId, clamped);
                }
            }
        }
    }
    @Override public void reduceCurrentAmmoCount(ItemStack stack) { setCurrentAmmoCount(stack, getCurrentAmmoCount(stack) - 1); }
    /** Consumes the exact number of independent rounds represented by a shot batch. */
    public void reduceCurrentAmmoCount(ItemStack stack, int rounds) {
        int safeRounds = Math.max(0, rounds);
        setCurrentAmmoCount(stack, Math.max(0, getCurrentAmmoCount(stack) - safeRounds));
    }
    @Override public void dropAllAmmo(Player player, ItemStack stack) { setCurrentAmmoCount(stack, 0); setBulletInBarrel(stack, false); }
    @Override public int getRepairMaterialCount(ItemStack stack, ItemStack material) {
        if (stack == null || stack.isEmpty() || material == null || material.isEmpty()) return 0;
        try {
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            String slot = getRepairSlot(stack);
            String partId = switch (slot) {
                case GunModuleSlots.BODY -> modules == null ? "" : modules.body().variantId();
                case GunModuleSlots.BARREL -> modules == null ? "" : modules.barrel().variantId();
                case GunModuleSlots.MAGAZINE -> modules == null ? "" : modules.magazine().variantId();
                default -> "";
            };
            if (!partId.isBlank() && modules != null) {
                String selectedMaterialId = switch (slot) {
                    case GunModuleSlots.BODY -> modules.body().materialId();
                    case GunModuleSlots.BARREL -> modules.barrel().materialId();
                    case GunModuleSlots.MAGAZINE -> modules.magazine().materialId();
                    default -> "";
                };
                var configured = com.pycoder.taczintetra.logic.RepairCostResolver.resolve(
                        TaCZinTetra.MODULE_CONFIG, slot, partId, selectedMaterialId);
                String repairItemId = BuiltInRegistries.ITEM.getKey(material.getItem()).toString();
                if (!configured.repairItem().isBlank()
                        && repairItemId.equals(configured.repairItem())) {
                    return configured.count();
                }
            }
            if (com.pycoder.taczintetra.config.TaczInTetraForgeConfig.ONLY_REPAIR_AGENTS.get()) {
                return 0;
            }
            var repairModule = getRepairModule(stack);
            if (repairModule.isPresent()) {
                var definition = repairModule.get().getRepairDefinition(stack, material);
                return definition == null || definition.material == null ? 0 : definition.material.count;
            }
        } catch (RuntimeException ignored) {
            // Invalid or incomplete module state must make the repair unavailable.
        }
        return 0;
    }
    @Override public ItemStack getAttachment(ItemStack stack, AttachmentType type) { return ItemStack.EMPTY; }
    @Override public ItemStack getBuiltinAttachment(ItemStack stack, AttachmentType type) { return ItemStack.EMPTY; }
    @Override public CompoundTag getAttachmentTag(ItemStack stack, AttachmentType type) { return new CompoundTag(); }
    @Override public ResourceLocation getBuiltInAttachmentId(ItemStack stack, AttachmentType type) { return DefaultAssets.EMPTY_ATTACHMENT_ID; }
    @Override public ResourceLocation getAttachmentId(ItemStack stack, AttachmentType type) { return DefaultAssets.EMPTY_ATTACHMENT_ID; }
    @Override public void installAttachment(ItemStack stack, ItemStack attachment) { }
    @Override public void unloadAttachment(ItemStack stack, AttachmentType type) { }
    @Override public boolean allowAttachment(ItemStack stack, ItemStack attachment) { return false; }
    @Override public boolean allowAttachmentType(ItemStack stack, AttachmentType type) { return false; }
    @Override public boolean hasBulletInBarrel(ItemStack stack) { return stack.getOrCreateTag().getBoolean(BULLET); }
    @Override public void setBulletInBarrel(ItemStack stack, boolean value) { stack.getOrCreateTag().putBoolean(BULLET, value); }
    @Override public boolean useInventoryAmmo(ItemStack stack) { return false; }
    @Override public boolean hasInventoryAmmo(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, boolean check) { return false; }
    @Override public int getRPM(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        return profile == null ? configuredInt("rpm", 400) : profile.fireControl().roundsPerMinute();
    }
    @Override public boolean isCanCrawl(ItemStack stack) { return true; }
    @Override public boolean hasCustomLaserColor(ItemStack stack) { return false; }
    @Override public int getLaserColor(ItemStack stack) { return 0; }
    @Override public void setLaserColor(ItemStack stack, int color) { }
    @Override public boolean hasHeatData(ItemStack stack) { return true; }
    @Override public boolean isOverheatLocked(ItemStack stack) { return stack.getOrCreateTag().getBoolean(OVERHEAT); }
    @Override public void setOverheatLocked(ItemStack stack, boolean locked) { stack.getOrCreateTag().putBoolean(OVERHEAT, locked); }
    @Override public void setHeatAmount(ItemStack stack, float amount) {
        float bounded = (float) Math.min(heatStat(stack, "max_heat", 100), Float.isFinite(amount) ? Math.max(0, amount) : 0);
        RUNTIME_HEAT.put(stack, bounded);
        stack.getOrCreateTag().putFloat(HEAT, bounded);
    }
    @Override public float lerpRPM(ItemStack stack) {
        double threshold = heatStat(stack, "overheat_threshold", heatStat(stack, "max_heat", 100));
        double multiplier = com.pycoder.taczintetra.logic.HeatEffectPolicy.curveMultiplier(
                getHeatAmount(stack), threshold,
                configuredHeatCurve("rpm_multiplier"),
                heatStat(stack, "min_rpm_multiplier", 1),
                heatStat(stack, "max_rpm_multiplier", 0.85));
        return (float) Math.max(1, getRPM(stack) * multiplier);
    }
    @Override public float lerpInaccuracy(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null) return 0;
        double value = profile.handling().accuracyAdd();
        double threshold = heatStat(stack, "overheat_threshold", heatStat(stack, "max_heat", 100));
        double multiplier = com.pycoder.taczintetra.logic.HeatEffectPolicy.curveMultiplier(
                getHeatAmount(stack), threshold,
                configuredHeatCurve("inaccuracy_multiplier"),
                heatStat(stack, "min_inaccuracy_multiplier", 1),
                heatStat(stack, "max_inaccuracy_multiplier", 1.2));
        return (float) (Double.isFinite(value) ? value * multiplier : 0);
    }
    @Override public float getHeatAmount(ItemStack stack) {
        Float runtime = RUNTIME_HEAT.get(stack);
        return runtime == null ? stack.getOrCreateTag().getFloat(HEAT) : runtime;
    }

    public int independentShots(ItemStack stack) {
        try {
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            if (modules != null) {
                return Math.max(1, Math.min(64, (int) Math.round(TaCZinTetra.MODULE_CONFIG.bodies().stream()
                        .filter(body -> body.id().equals(modules.body().variantId()))
                        .findFirst().map(body -> body.stats().getOrDefault("shots_per_trigger", 1.0)).orElse(1.0))));
            }
        } catch (RuntimeException ignored) { }
        return 1;
    }

    /** Total native projectiles for one trigger, capped by the shared safety limit. */
    public int projectileCount(ItemStack stack) {
        return projectileCount(stack, independentShots(stack));
    }

    /** Total projectiles for an already validated independent-round count. */
    public int projectileCount(ItemStack stack, int independentShots) {
        int pellets = (int) Math.round(heatStat(stack, "pellets_per_round", 1));
        return ShotCountCalculator.totalProjectiles(Math.max(0, independentShots), pellets, 64);
    }

    public void addShotHeat(ItemStack stack, long gameTime, int independentShots) {
        int shots = Math.max(1, Math.min(64, independentShots));
        setHeatAmount(stack, getHeatAmount(stack) + (float) (heatStat(stack, "heat_per_shot", 2) * shots));
        RUNTIME_HEAT_TICK.put(stack, gameTime);
        stack.getOrCreateTag().putLong(HEAT_TICK, gameTime);
        updateOverheat(stack);
    }

    public void addShotHeat(ItemStack stack, long gameTime) {
        addShotHeat(stack, gameTime, independentShots(stack));
    }

    public void tickHeat(ItemStack stack, long gameTime) {
        var tag = stack.getOrCreateTag();
        long previous = RUNTIME_HEAT_TICK.getOrDefault(stack,
                tag.contains(HEAT_TICK) ? tag.getLong(HEAT_TICK) : gameTime);
        long ticks = Math.max(0, Math.min(1200, gameTime - previous));
        double cooled = HeatModel.cool(getHeatAmount(stack), 0,
                coolingCoefficient(stack), ticks / 20.0,
                heatStat(stack, "heat_epsilon", 0.01));
        RUNTIME_HEAT.put(stack, (float) cooled);
        // Advance only the runtime anchor. Persisted NBT stays write-on-shot,
        // while the same elapsed interval cannot be cooled twice.
        RUNTIME_HEAT_TICK.put(stack, gameTime);
        updateOverheat(stack);
    }

    private void updateOverheat(ItemStack stack) {
        double threshold = heatStat(stack, "overheat_threshold", heatStat(stack, "max_heat", 100));
        double epsilon = heatStat(stack, "heat_epsilon", 0.01);
        if (isOverheatLocked(stack)) {
            if (HeatModel.canUnlock(getHeatAmount(stack), 0, epsilon)) setOverheatLocked(stack, false);
        } else if (HeatModel.isOverheated(getHeatAmount(stack), threshold, epsilon)) {
            setOverheatLocked(stack, true);
        }
    }

    private double heatStat(ItemStack stack, String key, double fallback) {
        try {
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            if (modules != null) {
                return TaCZinTetra.MODULE_CONFIG.barrels().stream()
                        .filter(barrel -> barrel.id().equals(modules.barrel().variantId()))
                        .findFirst().map(barrel -> barrel.stats().getOrDefault(key, fallback))
                        .filter(value -> Double.isFinite(value) && value >= 0).orElse(fallback);
            }
        } catch (RuntimeException ignored) { }
        return fallback;
    }

    private HeatCurve configuredHeatCurve(String key) {
        return TaCZinTetra.MODULE_CONFIG.heatCurves().get(key);
    }

    /** Cooling is the barrel type multiplier scaled by the selected material. */
    private double coolingCoefficient(ItemStack stack) {
        double barrelMultiplier = heatStat(stack, "cooling_coefficient", 4);
        try {
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            if (modules == null) return barrelMultiplier;
            double conductivity = TaCZinTetra.MODULE_CONFIG.materials().stream()
                    .filter(material -> material.id().equals(modules.barrel().materialId()))
                    .findFirst()
                    .map(ModuleConfig.Material::thermalConductivity)
                    .filter(value -> Double.isFinite(value) && value >= 0)
                    .orElse(1.0);
            return com.pycoder.taczintetra.logic.HeatEffectPolicy.coolingCoefficient(
                    barrelMultiplier, conductivity, coolingHoning(stack));
        } catch (RuntimeException ignored) {
            return barrelMultiplier;
        }
    }

    private double coolingHoning(ItemStack stack) {
        try {
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            if (modules == null) return 0;
            var body = TaCZinTetra.MODULE_CONFIG.bodies().stream()
                    .filter(value -> value.id().equals(modules.body().variantId()))
                    .findFirst().orElse(null);
            if (body == null) return 0;
            double perLevel = TaCZinTetra.MODULE_CONFIG.polish()
                    .getOrDefault(body.basePolish(), Map.of())
                    .getOrDefault("cooling_honing_per_level", 0.0);
            if (!Double.isFinite(perLevel) || perLevel < 0) return 0;
            return perLevel * Math.max(0, Math.min(1000, getHonedCount(stack)));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int configuredInt(String key, int fallback) {
        try {
            return TaCZinTetra.MODULE_CONFIG.bodies().stream().findFirst()
                    .map(body -> body.stats().get(key)).filter(value -> value != null && Double.isFinite(value) && value > 0)
                    .map(value -> (int) Math.min(Integer.MAX_VALUE, Math.round(value))).orElse(fallback);
        } catch (RuntimeException ignored) { return fallback; }
    }
}
