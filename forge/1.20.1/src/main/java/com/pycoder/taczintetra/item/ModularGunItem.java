package com.pycoder.taczintetra.item;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.logic.AmmoStatePolicy;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.pycoder.taczintetra.logic.HeatModel;
import com.pycoder.taczintetra.logic.HeatCurve;
import com.pycoder.taczintetra.logic.ShotCountCalculator;
import com.pycoder.taczintetra.logic.GunStateRevalidator;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.logic.GunBodyDisplayPolicy;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.modifier.ParameterizedCachePair;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import se.mickelus.tetra.items.modular.ItemModularHandheld;
import se.mickelus.tetra.gui.GuiModuleOffsets;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.function.Consumer;

/**
 * 由 Tetra 驱动的枪械主体。TaCZ 运行行为刻意放在服务逻辑中，
 * 避免模块数据和逐物品栈状态泄漏到 Item 类中。
 */
public class ModularGunItem extends ItemModularHandheld implements GunItemDataAccessor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean NATIVE_AMMO_WRITE_TRACE = Boolean.getBoolean("taczintetra.dev_trace_ammo_writes");
    private static final String AMMO = "taczintetra_ammo";
    private static final String FIRE_MODE = "taczintetra_fire_mode";
    private static final String BULLET = "taczintetra_bullet";
    private static final String HEAT = "taczintetra_heat";
    private static final String OVERHEAT = "taczintetra_overheat";
    private static final String HEAT_TICK = "taczintetra_heat_tick";
    private static final ResourceLocation GUN_ID = ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "modular_gun");
    /** 冷却状态不得在每个服务端 tick 修改手持物品栈。 */
    private static final Map<ItemStack, Float> RUNTIME_HEAT = Collections.synchronizedMap(new WeakHashMap<>());
    /** 独立的延迟冷却锚点；持久化 NBT 仍只在开火时写入。 */
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

    /** Match TaCZ AbstractGunItem: keep vanilla attack swings from lowering the gun pose. */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GunItemRendererWrapper renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GunItemRendererWrapper();
                }
                return renderer;
            }
        });
    }

    /**
     * Tetra 6.17 仅为零至三个槽位提供默认次级布局。
     * 第四个配置槽位使用显式且互不重叠的布局保持可见，
     * 不要越界访问 Tetra 的三槽位默认数组。
     */
    @Override
    public GuiModuleOffsets getMinorGuiOffsets(ItemStack stack) {
        if (getNumMinorModules(stack) == 4) {
            return new GuiModuleOffsets(-12, -1, -21, 12, -12, 25, 4, 12);
        }
        return super.getMinorGuiOffsets(stack);
    }

    /** 可编辑目录重载后，刷新继承自 Tetra 的基础数值。 */
    public void refreshConfiguredDurability() {
        this.baseDurability = configuredInt("base_durability", 100);
        this.baseIntegrity = configuredInt("base_integrity", 1);
    }

    @Override public float getAimingZoom(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        return profile == null ? 1.0f : (float) Math.max(0.01, profile.visual().aimingZoom());
    }
    // Reload reserves come from Tetra's configured material accounts, not
    // TaCZ's single integer dummy-ammo pool.
    @Override public boolean useDummyAmmo(ItemStack stack) { return false; }
    @Override public int getDummyAmmoAmount(ItemStack stack) { return GunItemDataAccessor.super.getDummyAmmoAmount(stack); }
    @Override public void setDummyAmmoAmount(ItemStack stack, int amount) { GunItemDataAccessor.super.setDummyAmmoAmount(stack, amount); }
    @Override public void addDummyAmmoAmount(ItemStack stack, int amount) { GunItemDataAccessor.super.addDummyAmmoAmount(stack, amount); }
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
    @Override public ResourceLocation getGunId(ItemStack stack) {
        ResourceLocation id = GunItemDataAccessor.super.getGunId(stack);
        if (id == null || DefaultAssets.EMPTY_GUN_ID.equals(id)) {
            GunItemDataAccessor.super.setGunId(stack, GUN_ID);
            return GUN_ID;
        }
        return id;
    }
    @Override public void setGunId(ItemStack stack, ResourceLocation id) {
        GunItemDataAccessor.super.setGunId(stack, id);
    }
    @Override public ResourceLocation getGunDisplayId(ItemStack stack) {
        ResourceLocation id = GunItemDataAccessor.super.getGunDisplayId(stack);
        if (id == null || DefaultAssets.DEFAULT_GUN_DISPLAY_ID.equals(id)
                || GunBodyDisplayPolicy.isManagedDisplayId(id)) {
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            String bodyVariant = modules == null ? null : modules.body().variantId();
            ResourceLocation selectedDisplay = GunBodyDisplayPolicy.displayIdForBody(bodyVariant);
            GunItemDataAccessor.super.setGunDisplayId(stack, selectedDisplay);
            return selectedDisplay;
        }
        return id;
    }
    @Override public void setGunDisplayId(ItemStack stack, ResourceLocation id) {
        GunItemDataAccessor.super.setGunDisplayId(stack, id);
    }
    @Override public int getLevel(int exp) { return 0; }
    @Override public int getExp(int level) { return 0; }
    @Override public int getMaxLevel() { return 0; }
    @Override public int getLevel(ItemStack stack) { return 0; }
    @Override public int getExp(ItemStack stack) { return 0; }
    @Override public int getExpToNextLevel(ItemStack stack) { return 0; }
    @Override public int getExpCurrentLevel(ItemStack stack) { return 0; }
    @Override public FireMode getFireMode(ItemStack stack) {
        migrateNativeGunState(stack);
        FireMode mode = GunItemDataAccessor.super.getFireMode(stack);
        return mode == null || mode == FireMode.UNKNOWN ? FireMode.SEMI : mode;
    }
    @Override public void setFireMode(ItemStack stack, FireMode mode) {
        GunItemDataAccessor.super.setFireMode(stack, mode == null ? FireMode.SEMI : mode);
    }
    @Override public int getCurrentAmmoCount(ItemStack stack) {
        migrateNativeGunState(stack);
        revalidateRuntimeState(stack);
        return Math.min(getMaxDummyAmmoAmount(stack),
                Math.max(0, GunItemDataAccessor.super.getCurrentAmmoCount(stack)));
    }
    @Override public void setCurrentAmmoCount(ItemStack stack, int count) {
        migrateNativeGunState(stack);
        int before = stack.getOrCreateTag().getInt("GunCurrentAmmoCount");
        int after = Math.min(getMaxDummyAmmoAmount(stack), Math.max(0, count));
        GunItemDataAccessor.super.setCurrentAmmoCount(stack, after);
        var tag = stack.getOrCreateTag();
        if (NATIVE_AMMO_WRITE_TRACE && before != after) {
            String stackUuid = tag.hasUUID("taczintetra_stack_uuid")
                    ? tag.getUUID("taczintetra_stack_uuid").toString() : "missing";
            String caller = StackWalker.getInstance().walk(frames -> frames.skip(1).limit(8)
                    .map(Object::toString).collect(Collectors.joining(" <- ")));
            LOGGER.info("NATIVE_AMMO_WRITE_TRACE source=setCurrentAmmoCount thread={} stackUuid={} before={} requested={} after={} caller={}",
                    Thread.currentThread().getName(), stackUuid, before, count, after, caller);
        }
    }

    /** Move old TaCZinTetra state into TaCZ's standard per-stack fields once. */
    private void migrateNativeGunState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        var tag = stack.getOrCreateTag();
        if (!tag.contains("GunCurrentAmmoCount", net.minecraft.nbt.Tag.TAG_INT)) {
            int legacyAmmo = tag.contains(AMMO, net.minecraft.nbt.Tag.TAG_INT)
                    ? tag.getInt(AMMO) : tag.getInt("AmmoCount");
            int migratedAmmo = Math.max(0, legacyAmmo);
            GunItemDataAccessor.super.setCurrentAmmoCount(stack, migratedAmmo);
            if (NATIVE_AMMO_WRITE_TRACE && migratedAmmo != 0) {
                String stackUuid = tag.hasUUID("taczintetra_stack_uuid")
                        ? tag.getUUID("taczintetra_stack_uuid").toString() : "missing";
                String caller = StackWalker.getInstance().walk(frames -> frames.skip(1).limit(8)
                        .map(Object::toString).collect(Collectors.joining(" <- ")));
                LOGGER.info("NATIVE_AMMO_WRITE_TRACE source=migrateNativeGunState thread={} stackUuid={} before=0 requested={} after={} caller={}",
                        Thread.currentThread().getName(), stackUuid, legacyAmmo, migratedAmmo, caller);
            }
        }
        if (!tag.contains("GunFireMode", net.minecraft.nbt.Tag.TAG_STRING)
                && tag.contains(FIRE_MODE, net.minecraft.nbt.Tag.TAG_STRING)) {
            GunItemDataAccessor.super.setFireMode(stack,
                    parseLegacyFireMode(tag.getString(FIRE_MODE)));
        }
        if (!tag.contains("HasBulletInBarrel", net.minecraft.nbt.Tag.TAG_BYTE)
                && tag.contains(BULLET, net.minecraft.nbt.Tag.TAG_BYTE)) {
            GunItemDataAccessor.super.setBulletInBarrel(stack, tag.getBoolean(BULLET));
        }
        if (!tag.contains("HeatAmount", net.minecraft.nbt.Tag.TAG_FLOAT)
                && tag.contains(HEAT, net.minecraft.nbt.Tag.TAG_FLOAT)) {
            GunItemDataAccessor.super.setHeatAmount(stack, tag.getFloat(HEAT));
        }
        if (!tag.contains("OverHeated", net.minecraft.nbt.Tag.TAG_BYTE)
                && tag.contains(OVERHEAT, net.minecraft.nbt.Tag.TAG_BYTE)) {
            GunItemDataAccessor.super.setOverheatLocked(stack, tag.getBoolean(OVERHEAT));
        }
        tag.remove(AMMO);
        tag.remove("AmmoCount");
        tag.remove(FIRE_MODE);
        tag.remove(BULLET);
        tag.remove(HEAT);
        tag.remove(OVERHEAT);
    }

    private static FireMode parseLegacyFireMode(String value) {
        try {
            FireMode mode = FireMode.valueOf(value);
            return mode == FireMode.UNKNOWN ? FireMode.SEMI : mode;
        } catch (IllegalArgumentException ignored) {
            return FireMode.SEMI;
        }
    }

    /** 根据当前模块档案重新验证已保存的弹药/资源状态。 */
    public void revalidateRuntimeState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        int maxAmmo = getMaxDummyAmmoAmount(stack);
        int maxResource = getResourceCapacity(stack);
        var recipe = ConfiguredAmmoRecipeResolver.select(TaCZinTetra.MODULE_CONFIG, this, stack);
        int loadedAmmo = Math.max(0, GunItemDataAccessor.super.getCurrentAmmoCount(stack));
        var state = GunStateRevalidator.revalidate(new GunStateRevalidator.State(
                maxAmmo, loadedAmmo, maxResource, 0, java.util.List.of("semi"), "semi"));
        if (state.currentAmmo() != loadedAmmo) {
            GunItemDataAccessor.super.setCurrentAmmoCount(stack, state.currentAmmo());
            if (NATIVE_AMMO_WRITE_TRACE) {
                var tag = stack.getOrCreateTag();
                String stackUuid = tag.hasUUID("taczintetra_stack_uuid")
                        ? tag.getUUID("taczintetra_stack_uuid").toString() : "missing";
                String caller = StackWalker.getInstance().walk(frames -> frames.skip(1).limit(8)
                        .map(Object::toString).collect(Collectors.joining(" <- ")));
                LOGGER.info("NATIVE_AMMO_REVALIDATE_WRITE_TRACE thread={} stackUuid={} before={} maxAmmo={} after={} caller={}",
                        Thread.currentThread().getName(), stackUuid, loadedAmmo, maxAmmo,
                        state.currentAmmo(), caller);
            }
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
    /** 精确消耗一次射击批次所代表的独立弹药数量。 */
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
            // 模块状态无效或不完整时，不得开放修理。
        }
        return 0;
    }
    @Override public ItemStack getAttachment(ItemStack stack, AttachmentType type) {
        ResourceLocation attachmentId = nativeAttachmentId(stack, type);
        if (DefaultAssets.EMPTY_ATTACHMENT_ID.equals(attachmentId)) return ItemStack.EMPTY;
        return AttachmentItemBuilder.create().setId(attachmentId).build();
    }
    @Override public ItemStack getBuiltinAttachment(ItemStack stack, AttachmentType type) { return ItemStack.EMPTY; }
    /** Tetra owns gameplay properties; these TaCZ stacks are render-only native model views. */
    @Override public CompoundTag getAttachmentTag(ItemStack stack, AttachmentType type) { return new CompoundTag(); }
    @Override public ResourceLocation getBuiltInAttachmentId(ItemStack stack, AttachmentType type) { return DefaultAssets.EMPTY_ATTACHMENT_ID; }
    @Override public ResourceLocation getAttachmentId(ItemStack stack, AttachmentType type) {
        return DefaultAssets.EMPTY_ATTACHMENT_ID;
    }
    @Override public void installAttachment(ItemStack stack, ItemStack attachment) { }
    @Override public void unloadAttachment(ItemStack stack, AttachmentType type) { }
    @Override public boolean allowAttachment(ItemStack stack, ItemStack attachment) { return false; }
    @Override public boolean allowAttachmentType(ItemStack stack, AttachmentType type) { return false; }

    private ResourceLocation nativeAttachmentId(ItemStack stack, AttachmentType type) {
        if (stack == null || stack.isEmpty() || type == null) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        try {
            String moduleSlot = switch (type) {
                case SCOPE -> GunModuleSlots.OPTIC;
                case MUZZLE -> GunModuleSlots.BARREL;
                case EXTENDED_MAG -> GunModuleSlots.MAGAZINE;
                case GRIP -> GunModuleSlots.GRIP;
                case STOCK -> GunModuleSlots.STOCK;
                default -> null;
            };
            if (moduleSlot == null || getModuleFromSlot(stack, moduleSlot) == null) {
                return DefaultAssets.EMPTY_ATTACHMENT_ID;
            }
            var modules = TetraItemStackProfileResolver.selectedModules(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            if (modules == null) return DefaultAssets.EMPTY_ATTACHMENT_ID;
            return GunBodyDisplayPolicy.nativeAttachmentIdForBody(modules.body().variantId(), type)
                    .orElse(DefaultAssets.EMPTY_ATTACHMENT_ID);
        } catch (RuntimeException ignored) {
            // Tetra module data can be incomplete during load; never invent an attachment in that state.
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
    }
    @Override public boolean hasBulletInBarrel(ItemStack stack) {
        migrateNativeGunState(stack);
        return GunItemDataAccessor.super.hasBulletInBarrel(stack);
    }
    @Override public void setBulletInBarrel(ItemStack stack, boolean value) {
        GunItemDataAccessor.super.setBulletInBarrel(stack, value);
    }
    @Override public boolean useInventoryAmmo(ItemStack stack) { return false; }
    @Override public boolean hasInventoryAmmo(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, boolean check) { return false; }
    @Override
    public <T> T modifyProperty(ShooterDataHolder holder, ItemStack stack, LivingEntity shooter,
                                String propertyName, Class<T> type, T defaultValue) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null) return defaultValue;
        var projectile = profile.projectile();
        if (GunProperties.AMMO_SPEED.name().equals(propertyName)
                && defaultValue instanceof Float base
                && Double.isFinite(projectile.velocity()) && projectile.velocity() > 0) {
            return type.cast(base * (float) projectile.velocity());
        }
        if (GunProperties.EFFECTIVE_RANGE.name().equals(propertyName)
                && defaultValue instanceof Float base
                && Double.isFinite(projectile.range()) && projectile.range() > 0) {
            return type.cast(base * (float) projectile.range());
        }
        if (GunProperties.ARMOR_IGNORE.name().equals(propertyName)
                && defaultValue instanceof Float base && Double.isFinite(projectile.armorIgnore())) {
            return type.cast(Math.max(0, Math.min(1, base + (float) projectile.armorIgnore())));
        }
        if (GunProperties.PIERCE.name().equals(propertyName)
                && defaultValue instanceof Integer && projectile.pierce() > 0) {
            return type.cast(Math.max(1, projectile.pierce()));
        }
        if (GunProperties.KNOCKBACK.name().equals(propertyName)
                && defaultValue instanceof Float base
                && Double.isFinite(projectile.knockback()) && projectile.knockback() > 0) {
            return type.cast(base * (float) projectile.knockback());
        }
        return defaultValue;
    }
    @Override
    @SuppressWarnings("unchecked")
    public <T> T modifyProperty(ShooterDataHolder holder, ItemStack stack, LivingEntity shooter,
                                String function, String propertyName, Class<T> type, T defaultValue) {
        if ("modify_cached_property".equals(function)
                && GunProperties.RECOIL.name().equals(propertyName)
                && defaultValue instanceof ParameterizedCachePair<?, ?> pair
                && type.isInstance(defaultValue)) {
            var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            if (profile != null && Double.isFinite(profile.handling().recoilAdd())
                    && profile.handling().recoilAdd() >= 0) {
                var recoil = (ParameterizedCachePair<Float, Float>) pair;
                return type.cast(com.pycoder.taczintetra.logic.TaczNativeRecoilCacheAdapter.scale(
                        recoil, profile.handling().recoilAdd()));
            }
        }
        return modifyProperty(holder, stack, shooter, propertyName, type, defaultValue);
    }
    @Override public int getRPM(ItemStack stack) {
        var profile = TetraItemStackProfileResolver.tryResolve(this, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        return profile == null ? configuredInt("rpm", 400) : profile.fireControl().roundsPerMinute();
    }
    @Override public boolean isCanCrawl(ItemStack stack) { return true; }
    /** This firearm is not a Tetra blocking tool; suppress its block-progress reticle. */
    @Override public float getBlockProgress(ItemStack stack, LivingEntity entity) { return 0.0f; }
    @Override public boolean hasCustomLaserColor(ItemStack stack) { return false; }
    @Override public int getLaserColor(ItemStack stack) { return 0; }
    @Override public void setLaserColor(ItemStack stack, int color) { }
    @Override public boolean hasHeatData(ItemStack stack) { return true; }
    @Override public boolean isOverheatLocked(ItemStack stack) {
        migrateNativeGunState(stack);
        return GunItemDataAccessor.super.isOverheatLocked(stack);
    }
    @Override public void setOverheatLocked(ItemStack stack, boolean locked) {
        GunItemDataAccessor.super.setOverheatLocked(stack, locked);
    }
    @Override public void setHeatAmount(ItemStack stack, float amount) {
        float bounded = (float) Math.min(heatStat(stack, "max_heat", 100), Float.isFinite(amount) ? Math.max(0, amount) : 0);
        RUNTIME_HEAT.put(stack, bounded);
        migrateNativeGunState(stack);
        GunItemDataAccessor.super.setHeatAmount(stack, bounded);
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
        if (runtime != null) return runtime;
        migrateNativeGunState(stack);
        return GunItemDataAccessor.super.getHeatAmount(stack);
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

    /** 单次扣动扳机产生的原生弹丸总数，并受统一安全上限约束。 */
    public int projectileCount(ItemStack stack) {
        return projectileCount(stack, independentShots(stack));
    }

    /** 根据已验证的独立弹药数量计算弹丸总数。 */
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
        // 仅推进运行时锚点。持久化 NBT 仍在射击时写入，
        // 同一时间间隔不能重复用于冷却。
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

    /** 冷却值由枪管类型倍率乘以所选材料系数计算。 */
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
