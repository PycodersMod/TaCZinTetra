package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.config.SpecialInlayPolicy;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.registry.ModItems;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.api.AddonAdapterRegistry;
import com.pycoder.taczintetra.api.ExternalCapabilityAdapter;
import com.pycoder.taczintetra.api.ResourceChannelType;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import com.pycoder.taczintetra.runtime.ExternalResourceService;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import org.slf4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

/** 为隔离客户端冒烟测试提供仅限开发使用的服务端观测。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class DevServerAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODULES_READY = "taczintetra_dev_modules_ready";
    private static final String WORKBENCH_OPENED = "taczintetra_dev_workbench_opened_v24";
    private static final String WORKBENCH_OPEN_DELAY = "taczintetra_dev_workbench_open_delay_v1";
    private static final String WORKBENCH_CRAFTED = "taczintetra_dev_workbench_crafted_v24";
    private static final String WORKBENCH_GUI_PENDING = "taczintetra_dev_workbench_gui_pending_v1";
    private static final String WORKBENCH_GUI_OBSERVED = "taczintetra_dev_workbench_gui_observed_v1";
    private static final String HEAT_PROBE_STATE = "taczintetra_dev_heat_probe_v14";
    private static final String HEAT_PROBE_START = "taczintetra_dev_heat_probe_start_v14";
    private static final String ENCHANTMENT_PROBE = "taczintetra_dev_enchantment_probe_v15";
    private static final String BROKEN_PROBE = "taczintetra_dev_broken_probe_v1";
    private static final String STANDARD_CONTAINER_PROBE = "taczintetra_dev_standard_container_probe_v2";
    private static final String PLAYER_INVENTORY_PROBE = "taczintetra_dev_player_inventory_probe_v1";
    private static final String SOPHISTICATED_BACKPACK_PROBE = "taczintetra_dev_sophisticated_backpack_probe_v1";
    private static final String ADDON_CAPABILITY_PROBE = "taczintetra_dev_addon_capability_probe_v1";
    private static final String STARTER_RECIPE_PROBE = "taczintetra_dev_starter_recipe_probe_v2";
    private static final String RAW_MATERIAL_REJECTION_PROBE = "taczintetra_dev_raw_material_rejection_probe_v1";
    private static final String REPAIR_MATERIAL_MATRIX_PROBE = "taczintetra_dev_repair_material_matrix_probe_v1";
    private static final String SPECIAL_INLAY_PROBE = "taczintetra_dev_special_inlay_probe_v2";
    private static final Map<net.minecraft.world.entity.player.Player, Integer> OFFHAND_SYNC_TICKS = new WeakHashMap<>();
    private static final Path DEV_MODULE_CONFIG = FMLPaths.CONFIGDIR.get().resolve("taczintetra.json");
    private static long devModuleConfigTimestamp = Long.MIN_VALUE;
    private static int devModuleConfigHash;
    private static boolean devConfigReloaded;
    private static boolean devPostReloadProfileLogged;
    private static ItemStack devProfileProbeStack = ItemStack.EMPTY;
    private static boolean devModuleConfigReloadPending;
    private static boolean devResetDone;
    private static final Map<net.minecraft.world.entity.player.Player, UUID> DEV_DAMAGE_TARGETS = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingBackpackRestore> PENDING_BACKPACK_RESTORES = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingBackpackClientProbe> PENDING_BACKPACK_CLIENT_PROBES = new WeakHashMap<>();

    private static final class PendingBackpackRestore {
        private final int selectedSlot;
        private final int selectedIndex;
        private final ItemStack selectedSlotStack;
        private final ItemStack mainHandStack;
        private final ItemStack offhandStack;
        private final ItemStack carriedStack;
        private int ticksRemaining;

        private PendingBackpackRestore(int selectedSlot, int selectedIndex, ItemStack selectedSlotStack,
                                       ItemStack mainHandStack, ItemStack offhandStack, ItemStack carriedStack) {
            this.selectedSlot = selectedSlot;
            this.selectedIndex = selectedIndex;
            this.selectedSlotStack = selectedSlotStack;
            this.mainHandStack = mainHandStack;
            this.offhandStack = offhandStack;
            this.carriedStack = carriedStack;
            this.ticksRemaining = 40;
        }
    }

    private static final class PendingBackpackClientProbe {
        private final int selectedIndex;
        private final ItemStack selectedSlotStack;
        private final ItemStack mainHandStack;
        private final ItemStack offhandStack;
        private final ItemStack carriedStack;
        private final ItemStack sourceGun;
        private int ticksRemaining = 100;

        private PendingBackpackClientProbe(int selectedIndex, ItemStack selectedSlotStack,
                                            ItemStack mainHandStack, ItemStack offhandStack,
                                            ItemStack carriedStack, ItemStack sourceGun) {
            this.selectedIndex = selectedIndex;
            this.selectedSlotStack = selectedSlotStack;
            this.mainHandStack = mainHandStack;
            this.offhandStack = offhandStack;
            this.carriedStack = carriedStack;
            this.sourceGun = sourceGun;
        }
    }
    private static final ExternalCapabilityAdapter DEV_ADDON_CAPABILITY = new ExternalCapabilityAdapter() {
        private int amount = 7;

        @Override
        public ResourceChannelType channelType() {
            return ResourceChannelType.ITEM;
        }

        @Override
        public boolean supports(String resourceId) {
            return "tacz:9mm".equals(resourceId);
        }

        @Override
        public int available(net.minecraft.world.entity.LivingEntity holder, String resourceId) {
            return amount;
        }

        @Override
        public boolean canExtract(net.minecraft.world.entity.LivingEntity holder, String resourceId, int requested) {
            return supports(resourceId) && requested > 0 && requested <= amount;
        }

        @Override
        public int extract(net.minecraft.world.entity.LivingEntity holder, String resourceId, int requested) {
            if (!canExtract(holder, resourceId, requested)) return 0;
            amount -= requested;
            return requested;
        }
    };

    private DevServerAutomation() {
    }

    /** 客户端确认已收到副手物品栈后，停止仅用于开发的物品栏镜像。 */
    public static void stopOffhandBroadcast(ServerPlayer player) {
        if (player != null) {
            OFFHAND_SYNC_TICKS.put(player, Integer.MAX_VALUE);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!Boolean.getBoolean("taczintetra.dev_automation")
                || event.getLevel().isClientSide()
                || !(event.getEntity() instanceof EntityKineticBullet bullet)) {
            return;
        }
        LOGGER.info("TaCZinTetra dev automation observed EntityKineticBullet id={} owner={} position={} velocity={}",
                bullet.getId(), bullet.getOwner(), bullet.position(), bullet.getDeltaMovement());
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!Boolean.getBoolean("taczintetra.dev_automation") || event.getEntity().level().isClientSide()) {
            return;
        }
        var target = event.getEntity();
        if (target.getTags().contains("taczintetra_dev_damage_target")) {
            LOGGER.info("TaCZinTetra dev projectile hit target: target={}, healthBefore={}, incomingDamage={}, source={}",
                    target.getUUID(), target.getHealth(), event.getAmount(), event.getSource().getMsgId());
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!Boolean.getBoolean("taczintetra.dev_automation") || event.getEntity().level().isClientSide()) {
            return;
        }
        var target = event.getEntity();
        if (target.getTags().contains("taczintetra_dev_damage_target")) {
            LOGGER.info("TaCZinTetra dev projectile damage event: target={}, healthObserved={}, damageApplied={}, source={}",
                    target.getUUID(), target.getHealth(), event.getAmount(), event.getSource().getMsgId());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !Boolean.getBoolean("taczintetra.dev_automation")
                || event.player.level().isClientSide()) {
            return;
        }
        var player = event.player;
        if (player instanceof ServerPlayer serverPlayer) {
            advancePendingBackpackClientProbe(serverPlayer);
            restorePendingBackpackProbe(serverPlayer);
        }
        observeDevConfigChange(player);
        probeConfiguredStarterRecipe(player);
        resetStaleProbeState(player);
        var stack = player.getMainHandItem();
        if (devConfigReloaded && !devPostReloadProfileLogged
                && !devProfileProbeStack.isEmpty()
                && devProfileProbeStack.getItem() instanceof ModularGunItem probeGun) {
            devPostReloadProfileLogged = true;
            var reloadedProfile = TetraItemStackProfileResolver.tryResolve(probeGun, devProfileProbeStack,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            LOGGER.info("TaCZinTetra dev profile after config reload: {}", reloadedProfile);
        }
        if (!(stack.getItem() instanceof ModularGunItem gun)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int guiPending = player.getPersistentData().getInt(WORKBENCH_GUI_PENDING);
            if (guiPending > 0) {
                if (guiPending == 60
                        && !player.getPersistentData().getBoolean(WORKBENCH_GUI_OBSERVED)) {
                    observeWorkbenchGuiSettlement(serverPlayer, stack);
                    player.getPersistentData().putBoolean(WORKBENCH_GUI_OBSERVED, true);
                }
                if (guiPending == 1) {
                    player.getPersistentData().remove(WORKBENCH_GUI_PENDING);
                    craftBodyThroughWorkbenchForRuntimeVerification(serverPlayer, stack);
                } else {
                    player.getPersistentData().putInt(WORKBENCH_GUI_PENDING, guiPending - 1);
                }
            }
        }
        var existingProfile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (existingProfile != null && devProfileProbeStack.isEmpty()) {
            devProfileProbeStack = stack.copy();
        }
        if (stack.getOrCreateTag().getBoolean(MODULES_READY) && existingProfile != null) {
            probeSpecialInlays(player, stack);
            ensureDamageProbeTarget(player);
            // 客户端命令可能先更新主手，副手的原版物品栏数据包稍后才到达。
            // 为保持开发探针的两组物品栈确定一致，
            // 在模块组装完成后，将已准备好的主手物品栈同步到服务端副手。

            int syncTicks = OFFHAND_SYNC_TICKS.getOrDefault(player, 0);
            if (syncTicks < 100) {
                ItemStack offhand = stack.copy();
                offhand.getOrCreateTag().putFloat("taczintetra_heat", 0.0f);
                offhand.getOrCreateTag().putBoolean("taczintetra_overheat", false);
                offhand.getOrCreateTag().putLong("taczintetra_heat_tick", player.level().getGameTime());
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offhand);
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
                OFFHAND_SYNC_TICKS.put(player, syncTicks + 1);
                if (syncTicks == 0) {
                    LOGGER.info("TaCZinTetra dev automation broadcasting off-hand stack for dual-hand probe: item={}",
                            player.getOffhandItem().getItem());
                }
            }
            if (player instanceof ServerPlayer serverPlayer) {
                // 通用容器流程不依赖 Tetra 工作台探针；
                // 即使工作台探针跳过或变更，也不能影响此契约测试。
                probeStandardContainerInsertion(serverPlayer, stack);
                probePlayerInventoryInsertion(serverPlayer, stack);
                probeSophisticatedBackpackInsertion(serverPlayer, stack);
            }
            if (player.getPersistentData().getBoolean(WORKBENCH_CRAFTED)) {
                probeHeatRuntime(player, gun, stack);
                if (player instanceof ServerPlayer serverPlayer) {
                    probeAddonCapability(serverPlayer);
                }
            }
            openWorkbenchForRuntimeVerification(player);
            return;
        }
        boolean shotgunProbe = player.getTags().contains("taczintetra_dev_shotgun");
        String bodyId = shotgunProbe ? "taczintetra/body/shotgun" : "taczintetra/body/pistol";
        String barrelId = shotgunProbe ? "taczintetra/barrel/12g" : "taczintetra/barrel/9mm";
        String ammoId = shotgunProbe ? "tacz:12g" : "tacz:9mm";
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.BODY, bodyId, "taczintetra/wood/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.BARREL, barrelId, "taczintetra/iron/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/gold/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.STOCK, "taczintetra/stock", "taczintetra/iron/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.OPTIC, "taczintetra/optic", "taczintetra/iron/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.GRIP, "taczintetra/grip", "taczintetra/iron/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                stack, GunModuleSlots.SPECIAL, "taczintetra/special/socket", "taczintetra/special_inlay/");
        // N/M 探针会替换先前的初始物品栈，因此要等最终物品栈组装完成后
        // 再执行数据驱动的弹道探针。
        SpecialInlayPolicy.write(stack.getOrCreateTag(), java.util.List.of("armor_piercer"));
        seedProbeAmmo(stack, ammoId);
        stack.getOrCreateTag().putBoolean(MODULES_READY, true);
        player.removeTag("taczintetra_dev_shotgun");
        var profile = TetraItemStackProfileResolver.tryResolve(gun, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        LOGGER.info("TaCZinTetra dev module structure: {}", describeModules(gun, stack));
        LOGGER.info("TaCZinTetra dev automation installed Tetra modules: body={}, barrel={}, magazine={}, attachments={}, minorModules={}, specialModule={}, maxDamage={}, profile={}",
                stack.getOrCreateTag().contains(GunModuleSlots.BODY),
                stack.getOrCreateTag().contains(GunModuleSlots.BARREL),
                stack.getOrCreateTag().contains(GunModuleSlots.MAGAZINE),
                stack.getOrCreateTag().contains(GunModuleSlots.STOCK),
                gun.getNumMinorModules(stack),
                gun.getModuleFromSlot(stack, GunModuleSlots.SPECIAL) != null,
                gun.getMaxDamage(stack),
                profile);
        if (!player.getPersistentData().getBoolean("taczintetra_dev_offhand_seeded_v2")) {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, stack.copy());
            player.getPersistentData().putBoolean("taczintetra_dev_offhand_seeded_v2", true);
            LOGGER.info("TaCZinTetra dev automation seeded off-hand stack for dual-hand probe: item={}",
                    player.getOffhandItem().getItem());
        }
        openWorkbenchForRuntimeVerification(player);
    }

    /** 为开发伤害冒烟测试创建一个确定性的生物目标。 */
    private static void ensureDamageProbeTarget(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || DEV_DAMAGE_TARGETS.containsKey(player)
                || !(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        // 隔离审计存档可能保留先前 JVM 运行留下的实体。创建当前目标前，
        // 仅移除带有本探针标记的目标，避免过期的火焰/生命值事件
        // 被误认为本次射击产生。
        level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        serverPlayer.getBoundingBox().inflate(64.0),
                        entity -> entity.getTags().contains("taczintetra_dev_damage_target"))
                .forEach(entity -> {
                    LOGGER.info("TaCZinTetra dev removed stale damage target: id={}, type={}",
                            entity.getUUID(), entity.getType());
                    entity.discard();
                });
        // 开发用弹道探针使用较大的生物碰撞箱，避免有效弹丸
        // 仅因擦过小型生物碰撞箱而被判定为未命中。
        var target = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        if (target == null) return;
        var look = serverPlayer.getLookAngle().normalize();
        // 将傀儡身体放在瞄准射线上。旧的“脚部与视线同高”摆放方式，
        // 会让整个碰撞箱高于开发冒烟测试中的向下弹道弧线。
        // 让悬空的开发目标保持在地面上方；否则审计世界会产生与弹丸样本无关的
        // 撞墙伤害事件。
        target.setPos(serverPlayer.getEyePosition().add(look.scale(1.5)));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setSilent(true);
        target.noPhysics = true;
        // 仅供开发使用的护甲对比目标。此前未穿护甲的傀儡基准
        // 记录在 task.md 中；本次为傀儡装备整套钻石盔甲，
        // 以便在真实命中和伤害事件处理环节观察忽略护甲的行为。

        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS));
        target.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0f);
        target.setDropChance(net.minecraft.world.entity.EquipmentSlot.CHEST, 0.0f);
        target.setDropChance(net.minecraft.world.entity.EquipmentSlot.LEGS, 0.0f);
        target.setDropChance(net.minecraft.world.entity.EquipmentSlot.FEET, 0.0f);
        target.addTag("taczintetra_dev_damage_target");
        level.addFreshEntity(target);
        // 部分原版生物生成流程会在加入世界时规范化装备；
        // 因此实体正式加入世界后再次设置确定的护甲套装，
        // 确保记录的护甲值对应实际受击目标。
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS));
        DEV_DAMAGE_TARGETS.put(player, target.getUUID());
        LOGGER.info("TaCZinTetra dev damage target spawned: id={}, health={}, armorValue={}, armorAttribute={}, toughnessAttribute={}, armorItems=[{},{},{},{}], position={}",
                target.getUUID(), target.getHealth(), target.getArmorValue(),
                target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR),
                target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS),
                target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem(),
                target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem(),
                target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).getItem(),
                target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getItem(), target.position());
    }

    /** 通过 Minecraft 配方管理器解析并合成真实的初始配方。 */
    private static void probeConfiguredStarterRecipe(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || player.getPersistentData().getBoolean(STARTER_RECIPE_PROBE)) return;
        var recipeId = ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "starter_pistol");
        var manager = serverPlayer.serverLevel().getRecipeManager();
        var container = new TransientCraftingContainer(serverPlayer.inventoryMenu, 3, 3);
        var recipe = manager.byKey(recipeId).orElse(null);
        if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
            LOGGER.warn("TaCZinTetra dev starter recipe probe missing or not crafting: {}", recipeId);
            player.getPersistentData().putBoolean(STARTER_RECIPE_PROBE, true);
            return;
        }
        var ingredients = craftingRecipe.getIngredients();
        for (int index = 0; index < Math.min(ingredients.size(), container.getContainerSize()); index++) {
            ItemStack[] choices = ingredients.get(index).getItems();
            if (choices.length > 0) container.setItem(index, choices[0].copyWithCount(1));
        }
        boolean matches = craftingRecipe.matches(container, serverPlayer.serverLevel());
        ItemStack assembled = matches ? craftingRecipe.assemble(container, serverPlayer.serverLevel().registryAccess()) : ItemStack.EMPTY;
        LOGGER.info("TaCZinTetra dev starter recipe probe: id={}, matches={}, result={}, tag={}, modules={}",
                recipeId, matches, assembled, assembled.getTag(),
                assembled.getItem() instanceof ModularGunItem gun ? describeModules(gun, assembled) : "not_modular_gun");
        player.getPersistentData().putBoolean(STARTER_RECIPE_PROBE, true);
    }

    /** 将较大的探针 NBT 保留在服务端；客户端命令数据包上限为 256 个字符。 */
    private static void seedProbeAmmo(ItemStack stack, String ammoId) {
        var tag = stack.getOrCreateTag();
        tag.putInt("taczintetra_ammo", 15);
        tag.putInt("taczintetra_max_ammo", 15);
        tag.putInt("AmmoCount", 15);
        var resources = new CompoundTag();
        var itemResources = new CompoundTag();
        itemResources.putInt(ammoId, 15);
        resources.put("taczintetra:item", itemResources);
        tag.put("resources", resources);
    }

    /**
     * 隔离冒烟测试没有可靠的控制台输入通道。仅在开发自动化中，
     * 检测到 taczintetra.json 发生变化时，会执行与 /reload 命令相同的服务端重载流程，
     * 并留下可审计的日志记录。
     */
    private static void observeDevConfigChange(net.minecraft.world.entity.player.Player player) {
        if (devModuleConfigReloadPending || !(player instanceof ServerPlayer serverPlayer)) return;
        try {
            long timestamp = Files.getLastModifiedTime(DEV_MODULE_CONFIG).toMillis();
            int contentHash = Files.readString(DEV_MODULE_CONFIG).hashCode();
            if (devModuleConfigTimestamp == Long.MIN_VALUE) {
                devModuleConfigTimestamp = timestamp;
                devModuleConfigHash = contentHash;
                LOGGER.info("TaCZinTetra dev config watcher armed: path={}, timestamp={}, hash={}",
                        DEV_MODULE_CONFIG, timestamp, contentHash);
                return;
            }
            if (timestamp == devModuleConfigTimestamp && contentHash == devModuleConfigHash) return;
            devModuleConfigTimestamp = timestamp;
            devModuleConfigHash = contentHash;
            devModuleConfigReloadPending = true;
            var server = serverPlayer.getServer();
            if (server == null) return;
            LOGGER.info("TaCZinTetra dev config file changed; starting resource reload: path={}", DEV_MODULE_CONFIG);
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((ignored, error) -> {
                devModuleConfigReloadPending = false;
                if (error != null) {
                    LOGGER.error("TaCZinTetra dev config resource reload failed", error);
                } else {
                    devConfigReloaded = true;
                    LOGGER.info("TaCZinTetra dev config resource reload completed");
                }
            });
        } catch (Exception error) {
            LOGGER.warn("TaCZinTetra dev config watcher could not inspect {}", DEV_MODULE_CONFIG, error);
        }
    }

    /**
     * 开发存档会在多次启动之间保留探针标记。因此重置操作
     * 只作用于当前 JVM 进程，不写入存档：
     * 每次新建隔离窗口时都会真实执行一次工作台生命周期。
     */
    private static void resetStaleProbeState(net.minecraft.world.entity.player.Player player) {
        if (devResetDone) return;
        var data = player.getPersistentData();
        data.remove(WORKBENCH_OPENED);
        data.remove(WORKBENCH_OPEN_DELAY);
        data.remove(WORKBENCH_CRAFTED);
        data.remove(WORKBENCH_GUI_PENDING);
        data.remove(WORKBENCH_GUI_OBSERVED);
        data.remove(HEAT_PROBE_STATE);
        data.remove(HEAT_PROBE_START);
        data.remove(ENCHANTMENT_PROBE);
        data.remove(BROKEN_PROBE);
        data.remove(STANDARD_CONTAINER_PROBE);
        data.remove(SOPHISTICATED_BACKPACK_PROBE);
        data.remove(ADDON_CAPABILITY_PROBE);
        data.remove(STARTER_RECIPE_PROBE);
        data.remove(SPECIAL_INLAY_PROBE);
        OFFHAND_SYNC_TICKS.remove(player);
        devResetDone = true;
        LOGGER.info("TaCZinTetra dev automation reset stale probe markers for a fresh workbench lifecycle");
    }

    /** 确认真实枪械 NBT 中已过滤配置的特殊嵌片 ID。 */
    private static void probeSpecialInlays(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        var data = player.getPersistentData();
        if (data.getBoolean(SPECIAL_INLAY_PROBE)) return;
        SpecialInlayPolicy.write(stack.getOrCreateTag(), java.util.List.of("armor_piercer", "taczintetra:unknown"));
        // 初始模块数据包发出后，探针会修改逻辑服务端物品栈。
        // 重新发布完整物品栈，确保客户端/TaCZ 射击流程
        // 收到相同的特殊嵌片 NBT，而不是旧副本。
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        var result = SpecialInlayPolicy.resolve(stack.getOrCreateTag(), TaCZinTetra.MODULE_CONFIG);
        LOGGER.info("TaCZinTetra dev special inlay probe: active={}, rejected={}, capacity={}, usedCapacity={}",
                result.activeIds(), result.rejectedIds(), result.capacity(), result.usedCapacity());
        UpgradeSchematic schematic = SchematicRegistry.getSchematic("taczintetra/special_socket");
        if (schematic != null) {
            ItemStack craftTarget = new ItemStack((ModularGunItem) stack.getItem());
            se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                    craftTarget, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/wood/");
            se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                    craftTarget, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
            se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                    craftTarget, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/gold/");
            ItemStack material = new ItemStack(ModItems.SPECIAL_INLAY.get());
            ItemStack[] materials = {material.copy()};
            String slot = GunModuleSlots.SPECIAL;
            boolean accepted = schematic.acceptsMaterial(craftTarget, slot, 0, material);
            boolean canApply = schematic.canApplyUpgrade(player, craftTarget, materials, slot, java.util.Map.of());
            ItemStack applied = canApply
                    ? schematic.applyUpgrade(craftTarget, materials, true, slot, player)
                    : craftTarget;
            boolean installed = ((ModularGunItem) applied.getItem())
                    .getModuleFromSlot(applied, slot) != null;
            LOGGER.info("TaCZinTetra dev special schematic settlement: accepted={}, canApply={}, installed={}, materialBefore={}, materialAfter={}, targetMinorModules={}",
                    accepted, canApply, installed, material.getCount(), materials[0].getCount(),
                    ((ModularGunItem) applied.getItem()).getNumMinorModules(applied));
            var installedModule = ((ModularGunItem) applied.getItem()).getModuleFromSlot(applied, slot);
            ItemStack[] extracted = installedModule == null ? new ItemStack[0] : installedModule.removeModule(applied);
            boolean removed = ((ModularGunItem) applied.getItem()).getModuleFromSlot(applied, slot) == null;
            LOGGER.info("TaCZinTetra dev special extraction settlement: removed={}, extractedStacks={}, extractedItems={}",
                    removed, extracted.length,
                    java.util.Arrays.stream(extracted).filter(value -> value != null && !value.isEmpty()).count());
        } else {
            LOGGER.warn("TaCZinTetra dev special schematic settlement skipped: schematic missing");
        }
        data.putBoolean(SPECIAL_INLAY_PROBE, true);
    }

    private static void probeHeatRuntime(net.minecraft.world.entity.player.Player player,
                                         ModularGunItem gun, ItemStack stack) {
        var data = player.getPersistentData();
        int state = data.getInt(HEAT_PROBE_STATE);
        long now = player.level().getGameTime();
        if (state == 0) {
            gun.setHeatAmount(stack, 98);
            gun.setOverheatLocked(stack, false);
            gun.addShotHeat(stack, now, 1);
            data.putInt(HEAT_PROBE_STATE, 1);
            data.putLong(HEAT_PROBE_START, now);
            LOGGER.info("TaCZinTetra dev heat probe: after shot heat={}, locked={}, hotRpm={}, inaccuracy={}",
                    gun.getHeatAmount(stack), gun.isOverheatLocked(stack), gun.lerpRPM(stack),
                    gun.lerpInaccuracy(stack));
            return;
        }
        if (state == 1 && now - data.getLong(HEAT_PROBE_START) >= 5) {
            data.putInt(HEAT_PROBE_STATE, 2);
            LOGGER.info("TaCZinTetra dev heat probe: cooling observed heat={}, locked={}, elapsedTicks={}",
                    gun.getHeatAmount(stack), gun.isOverheatLocked(stack),
                    now - data.getLong(HEAT_PROBE_START));
            return;
        }
        if (state == 2 && !gun.isOverheatLocked(stack)) {
            data.putInt(HEAT_PROBE_STATE, 3);
            LOGGER.info("TaCZinTetra dev heat probe: heat cooled and unlocked heat={}, locked={}, elapsedTicks={}",
                    gun.getHeatAmount(stack), gun.isOverheatLocked(stack),
                    now - data.getLong(HEAT_PROBE_START));
        }
    }

    /**
     * 测试原版箱子使用的常规服务端菜单流程。此流程
     * 仅供开发使用，并调用真实的 AbstractContainerMenu.clicked
     * 入口，以便观察 mixin 的携带资源插入逻辑。
     */
    private static void probeStandardContainerInsertion(ServerPlayer player, ItemStack sourceGun) {
        var data = player.getPersistentData();
        if (data.getBoolean(STANDARD_CONTAINER_PROBE)
                || !(sourceGun.getItem() instanceof ModularGunItem gun)) return;
        ResourceLocation ammoId = ResourceLocation.fromNamespaceAndPath("tacz", "9mm");
        ResourceLocation ammoItemId = ResourceLocation.fromNamespaceAndPath("tacz", "ammo");
        ItemStack ammo = new ItemStack(BuiltInRegistries.ITEM.get(ammoItemId), 3);
        if (ammo.isEmpty()) {
            LOGGER.warn("TaCZinTetra dev standard container insertion skipped: missing {}", ammoItemId);
            data.putBoolean(STANDARD_CONTAINER_PROBE, true);
            return;
        }
        ammo.getOrCreateTag().putString("AmmoId", ammoId.toString());
        ItemStack targetGun = sourceGun.copy();
        ResourceNbtAdapter.write(targetGun.getOrCreateTag(), ResourceInsertionService.ITEM_CHANNEL,
                ammoId.toString(), 0);
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, targetGun);
        MenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, ignored) -> ChestMenu.threeRows(containerId, inventory, chest),
                Component.literal("TaCZinTetra standard container probe"));
        NetworkHooks.openScreen(player, provider);
        if (player.containerMenu instanceof ChestMenu) {
            player.containerMenu.setCarried(ammo);
            player.containerMenu.clicked(0, 1, ClickType.PICKUP, player);
            int inserted = ResourceNbtAdapter.read(chest.getItem(0).getOrCreateTag(),
                    ResourceInsertionService.ITEM_CHANNEL, ammoId.toString());
            LOGGER.info("TaCZinTetra dev standard container insertion: menu={}, inserted={}, chestAmmo={}, carriedRemainder={}, changedSlot={}",
                    player.containerMenu.getClass().getSimpleName(), inserted,
                    inserted, player.containerMenu.getCarried().getCount(), chest.getItem(0));
        } else {
            LOGGER.warn("TaCZinTetra dev standard container insertion skipped: menu={}",
                    player.containerMenu.getClass().getSimpleName());
        }
        // 此探针会打开真实的客户端菜单。运行可选探针前应先关闭该菜单，
        // 否则同一服务端 tick 内会排入多个
        // NetworkHooks.openScreen 数据包，
        // 后续背包上下文可能会基于过期物品栏进行校验。
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        data.putBoolean(STANDARD_CONTAINER_PROBE, true);
    }

    /**
     * 测试真实玩家 InventoryMenu 流程，并在返回前恢复快捷栏与
     * 携带物品栈。此测试不针对附属模组容器。
     */
    private static void probePlayerInventoryInsertion(ServerPlayer player, ItemStack sourceGun) {
        var data = player.getPersistentData();
        if (data.getBoolean(PLAYER_INVENTORY_PROBE)
                || !(sourceGun.getItem() instanceof ModularGunItem)) return;
        ResourceLocation ammoId = ResourceLocation.fromNamespaceAndPath("tacz", "9mm");
        ResourceLocation ammoItemId = ResourceLocation.fromNamespaceAndPath("tacz", "ammo");
        ItemStack ammo = new ItemStack(BuiltInRegistries.ITEM.get(ammoItemId), 3);
        if (ammo.isEmpty()) {
            LOGGER.warn("TaCZinTetra dev player inventory insertion skipped: missing {}", ammoItemId);
            data.putBoolean(PLAYER_INVENTORY_PROBE, true);
            return;
        }
        ammo.getOrCreateTag().putString("AmmoId", ammoId.toString());
        var inventory = player.getInventory();
        ItemStack originalHotbar = inventory.getItem(0).copy();
        ItemStack originalCarried = player.inventoryMenu.getCarried().copy();
        ItemStack targetGun = sourceGun.copy();
        ResourceNbtAdapter.write(targetGun.getOrCreateTag(), ResourceInsertionService.ITEM_CHANNEL,
                ammoId.toString(), 0);
        try {
            inventory.setItem(0, targetGun);
            player.inventoryMenu.broadcastChanges();
            player.inventoryMenu.setCarried(ammo);
            player.inventoryMenu.clicked(36, 1, ClickType.PICKUP, player);
            int inserted = ResourceNbtAdapter.read(inventory.getItem(0).getOrCreateTag(),
                    ResourceInsertionService.ITEM_CHANNEL, ammoId.toString());
            LOGGER.info("TaCZinTetra dev player inventory insertion: menu={}, inserted={}, carriedRemainder={}, slotStack={}",
                    player.inventoryMenu.getClass().getSimpleName(), inserted,
                    player.inventoryMenu.getCarried().getCount(), inventory.getItem(0));
        } finally {
            inventory.setItem(0, originalHotbar);
            player.inventoryMenu.setCarried(originalCarried);
            player.inventoryMenu.broadcastChanges();
        }
        data.putBoolean(PLAYER_INVENTORY_PROBE, true);
    }

    /**
     * 若安装了可选模组 Sophisticated Backpacks，则打开真实的背包物品菜单。
     * 通过注册表查询避免核心制品对附属模组产生硬依赖；
     * 此探针仅供开发使用，并使用一次性物品栈。
     */
    private static void probeSophisticatedBackpackInsertion(ServerPlayer player, ItemStack sourceGun) {
        var data = player.getPersistentData();
        if (data.getBoolean(SOPHISTICATED_BACKPACK_PROBE)
                || !(sourceGun.getItem() instanceof ModularGunItem)) return;
        ResourceLocation backpackId = ResourceLocation.fromNamespaceAndPath(
                "sophisticatedbackpacks", "backpack");
        net.minecraft.world.item.Item backpack = BuiltInRegistries.ITEM.get(backpackId);
        if (backpack == net.minecraft.world.item.Items.AIR) {
            LOGGER.info("TaCZinTetra dev Sophisticated Backpacks insertion skipped: mod item missing {}", backpackId);
            data.putBoolean(SOPHISTICATED_BACKPACK_PROBE, true);
            return;
        }
        ResourceLocation ammoId = ResourceLocation.fromNamespaceAndPath("tacz", "9mm");
        ResourceLocation ammoItemId = ResourceLocation.fromNamespaceAndPath("tacz", "ammo");
        ItemStack ammo = new ItemStack(BuiltInRegistries.ITEM.get(ammoItemId), 3);
        if (ammo.isEmpty()) {
            LOGGER.warn("TaCZinTetra dev Sophisticated Backpacks insertion skipped: missing {}", ammoItemId);
            data.putBoolean(SOPHISTICATED_BACKPACK_PROBE, true);
            return;
        }
        ItemStack originalMain = player.getMainHandItem().copy();
        ItemStack originalCarried = player.inventoryMenu.getCarried().copy();
        int selectedIndex = player.getInventory().selected;
        int selectedSlot = selectedIndex;
        ItemStack originalSelectedSlot = player.getInventory().getItem(selectedSlot).copy();
        ItemStack originalOffhand = player.getOffhandItem().copy();
        ItemStack backpackStack = new ItemStack(backpack);
        try {
            // 让正常的客户端交互数据包打开菜单。直接调用服务端 Item#use
            // 虽然会打开服务端菜单，但客户端会在同步物品栈
            // 获得能力数据之前重建 BackpackContext，
            // 从而产生错误的槽位包装器报错。
            player.getInventory().setItem(selectedIndex, backpackStack);
            player.getInventory().selected = selectedIndex;
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, backpackStack);
            player.inventoryMenu.broadcastChanges();
            player.inventoryMenu.broadcastFullState();
            PENDING_BACKPACK_CLIENT_PROBES.put(player, new PendingBackpackClientProbe(
                    selectedIndex, originalSelectedSlot, originalMain, originalOffhand,
                    originalCarried, sourceGun.copy()));
            LOGGER.info("TaCZinTetra dev Sophisticated Backpacks client probe armed: selectedSlot={}, item={}",
                    selectedIndex, player.getMainHandItem().getItem());
        } catch (RuntimeException error) {
            LOGGER.warn("TaCZinTetra dev Sophisticated Backpacks client probe setup failed", error);
            data.putBoolean(SOPHISTICATED_BACKPACK_PROBE, true);
            restoreBackpackProbe(player, selectedSlot, selectedIndex, originalSelectedSlot,
                    originalMain, originalOffhand, originalCarried);
        }
    }

    private static void restorePendingBackpackProbe(ServerPlayer player) {
        PendingBackpackRestore pending = PENDING_BACKPACK_RESTORES.get(player);
        if (pending == null) return;
        if (--pending.ticksRemaining > 0) return;
        PENDING_BACKPACK_RESTORES.remove(player);
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        restoreBackpackProbe(player, pending.selectedSlot, pending.selectedIndex,
                pending.selectedSlotStack, pending.mainHandStack, pending.offhandStack, pending.carriedStack);
    }

    private static void advancePendingBackpackClientProbe(ServerPlayer player) {
        PendingBackpackClientProbe pending = PENDING_BACKPACK_CLIENT_PROBES.get(player);
        if (pending == null) return;
        if (--pending.ticksRemaining <= 0) {
            PENDING_BACKPACK_CLIENT_PROBES.remove(player);
            player.getPersistentData().putBoolean(SOPHISTICATED_BACKPACK_PROBE, true);
            LOGGER.warn("TaCZinTetra dev Sophisticated Backpacks client probe timed out: menu={}",
                    player.containerMenu.getClass().getName());
            restoreBackpackProbe(player, pending.selectedIndex, pending.selectedIndex,
                    pending.selectedSlotStack, pending.mainHandStack, pending.offhandStack,
                    pending.carriedStack);
            return;
        }
        if (player.containerMenu == player.inventoryMenu
                || !player.containerMenu.getClass().getName()
                .contains("sophisticatedbackpacks.common.gui.BackpackContainer")) {
            return;
        }
        ResourceLocation ammoId = ResourceLocation.fromNamespaceAndPath("tacz", "9mm");
        ResourceLocation ammoItemId = ResourceLocation.fromNamespaceAndPath("tacz", "ammo");
        ItemStack ammo = new ItemStack(BuiltInRegistries.ITEM.get(ammoItemId), 3);
        if (ammo.isEmpty()) {
            LOGGER.warn("TaCZinTetra dev Sophisticated Backpacks insertion skipped: missing {}", ammoItemId);
            PENDING_BACKPACK_CLIENT_PROBES.remove(player);
            player.getPersistentData().putBoolean(SOPHISTICATED_BACKPACK_PROBE, true);
            restoreBackpackProbe(player, pending.selectedIndex, pending.selectedIndex,
                    pending.selectedSlotStack, pending.mainHandStack, pending.offhandStack,
                    pending.carriedStack);
            return;
        }
        ammo.getOrCreateTag().putString("AmmoId", ammoId.toString());
        ItemStack targetGun = pending.sourceGun.copy();
        ResourceNbtAdapter.write(targetGun.getOrCreateTag(), ResourceInsertionService.ITEM_CHANNEL,
                ammoId.toString(), 0);
        int targetSlot = -1;
        for (int index = 0; index < player.containerMenu.slots.size(); index++) {
            var slot = player.containerMenu.getSlot(index);
            if (slot.container != player.getInventory() && slot.mayPlace(targetGun)) {
                targetSlot = index;
                break;
            }
        }
        if (targetSlot < 0) {
            LOGGER.warn("TaCZinTetra dev Sophisticated Backpacks insertion skipped: no storage slot");
            return;
        }
        var slot = player.containerMenu.getSlot(targetSlot);
        slot.set(targetGun);
        player.containerMenu.broadcastChanges();
        player.containerMenu.setCarried(ammo);
        player.containerMenu.clicked(targetSlot, 1, ClickType.PICKUP, player);
        int inserted = ResourceNbtAdapter.read(slot.getItem().getOrCreateTag(),
                ResourceInsertionService.ITEM_CHANNEL, ammoId.toString());
        LOGGER.info("TaCZinTetra dev Sophisticated Backpacks insertion: menu={}, slot={}, inserted={}, carriedRemainder={}, slotStack={}",
                player.containerMenu.getClass().getName(), targetSlot, inserted,
                player.containerMenu.getCarried().getCount(), slot.getItem());
        PENDING_BACKPACK_CLIENT_PROBES.remove(player);
        player.getPersistentData().putBoolean(SOPHISTICATED_BACKPACK_PROBE, true);
        PENDING_BACKPACK_RESTORES.put(player, new PendingBackpackRestore(
                pending.selectedIndex, pending.selectedIndex, pending.selectedSlotStack,
                pending.mainHandStack, pending.offhandStack, pending.carriedStack));
    }

    private static void restoreBackpackProbe(ServerPlayer player, int selectedSlot,
                                             int selectedIndex, ItemStack selectedSlotStack, ItemStack mainHandStack,
                                             ItemStack offhandStack, ItemStack carriedStack) {
        player.getInventory().selected = selectedIndex;
        if (selectedSlot >= 0) player.getInventory().setItem(selectedSlot, selectedSlotStack);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mainHandStack);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offhandStack);
        player.inventoryMenu.setCarried(carriedStack);
        player.inventoryMenu.broadcastChanges();
    }

    /** 测试可选附属能力桥接，不加载附属模组类。 */
    private static void probeAddonCapability(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.getBoolean(ADDON_CAPABILITY_PROBE)) return;
        AddonAdapterRegistry.Registration registration =
                AddonAdapterRegistry.registerScoped(DEV_ADDON_CAPABILITY);
        try {
            String resourceId = "tacz:9mm";
            int before = ExternalResourceService.available(resourceId, player);
            boolean extracted = ExternalResourceService.extract(DEV_ADDON_CAPABILITY, player,
                    resourceId, 2);
            int after = ExternalResourceService.available(resourceId, player);
            LOGGER.info("TaCZinTetra dev addon capability simulation: resource={}, before={}, extracted={}, after={}, adapter={}",
                    resourceId, before, extracted, after, DEV_ADDON_CAPABILITY.getClass().getName());
            data.putBoolean(ADDON_CAPABILITY_PROBE, true);
        } finally {
            registration.close();
        }
    }

    private static void openWorkbenchForRuntimeVerification(net.minecraft.world.entity.player.Player player) {
        if (player.getPersistentData().getBoolean(WORKBENCH_OPENED)) return;
        int openDelay = player.getPersistentData().getInt(WORKBENCH_OPEN_DELAY);
        if (openDelay == 0) {
            // 等待前面的全息球探针完成并关闭客户端界面，
            // 再发送服务端打开菜单的数据包。否则
            // WorkbenchTile 可能已在服务端打开，而客户端仍显示 HoloGui，
            // 因而不会创建 WorkbenchScreen。
            player.getPersistentData().putInt(WORKBENCH_OPEN_DELAY, 40);
            return;
        }
        if (openDelay > 1) {
            player.getPersistentData().putInt(WORKBENCH_OPEN_DELAY, openDelay - 1);
            return;
        }
        player.getPersistentData().remove(WORKBENCH_OPEN_DELAY);
        LOGGER.info("TaCZinTetra dev workbench entry: openedMarker=false, mainHand={}, positionBase={}",
                player.getMainHandItem().getItem(), player.blockPosition());
        Block workbench = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tetra", "basic_workbench"));
        LOGGER.info("TaCZinTetra dev workbench block lookup: id=tetra:basic_workbench, block={}, air={}",
                workbench, workbench.defaultBlockState().isAir());
        if (workbench.defaultBlockState().isAir()) {
            LOGGER.warn("TaCZinTetra dev workbench verification skipped: tetra:basic_workbench is not registered");
            return;
        }
        BlockPos position = player.blockPosition().relative(player.getDirection()).above();
        player.level().setBlockAndUpdate(position, workbench.defaultBlockState());
        LOGGER.info("TaCZinTetra dev workbench block placed: position={}, blockEntity={}",
                position, player.level().getBlockEntity(position));
        if (player.level().getBlockEntity(position) instanceof MenuProvider provider
                && player instanceof ServerPlayer serverPlayer) {
            prepareWorkbenchGuiProbe((WorkbenchTile) provider, serverPlayer, player.getMainHandItem());
            // WorkbenchContainer 的 MenuType 工厂会从额外数据缓冲区读取方块位置；
            // 通用的 openMenu(provider) 流程会发送 null。
            NetworkHooks.openScreen(serverPlayer, provider, position);
            player.getPersistentData().putBoolean(WORKBENCH_OPENED, true);
            player.getPersistentData().putInt(WORKBENCH_GUI_PENDING, 80);
            LOGGER.info("TaCZinTetra dev opened Tetra workbench menu at {}", position);
        } else {
            LOGGER.warn("TaCZinTetra dev workbench block entity was not created at {}", position);
        }
    }

    private static void prepareWorkbenchGuiProbe(WorkbenchTile workbench, ServerPlayer player,
                                                  ItemStack heldGun) {
        if (!(heldGun.getItem() instanceof ModularGunItem)) return;
        workbench.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(capability -> {
            if (!(capability instanceof ItemStackHandler handler)) return;
            ItemStack target = heldGun.copy();
            ItemStack material = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "iron_body")));
            var schematic = SchematicRegistry.getSchematic("taczintetra/body_rifle");
            if (schematic == null) {
                LOGGER.warn("TaCZinTetra dev workbench GUI probe skipped: body_rifle schematic missing");
                return;
            }
            handler.setStackInSlot(0, target);
            handler.setStackInSlot(1, material);
            workbench.setCurrentSchematic(schematic, GunModuleSlots.BODY);
            workbench.setChanged();
            boolean canApply = schematic.canApplyUpgrade(player, target,
                    new ItemStack[]{material.copy()}, GunModuleSlots.BODY, java.util.Map.of());
            LOGGER.info("TaCZinTetra dev workbench GUI probe prepared: schematic={}, target={}, material={}, canApply={}",
                    schematic.getKey(), target, material, canApply);
        });
    }

    private static void craftBodyThroughWorkbenchForRuntimeVerification(ServerPlayer player, ItemStack heldGun) {
        if (!(heldGun.getItem() instanceof ModularGunItem)) return;
        BlockPos position = player.blockPosition().relative(player.getDirection()).above();
        if (player.level().getBlockEntity(position) instanceof WorkbenchTile workbench) {
            LOGGER.info("TaCZinTetra dev workbench GUI probe window elapsed; continuing full workbench regression");
            craftBodyThroughWorkbench(workbench, player, heldGun);
        }
    }

    private static void observeWorkbenchGuiSettlement(ServerPlayer player, ItemStack heldGun) {
        BlockPos position = player.blockPosition().relative(player.getDirection()).above();
        if (!(player.level().getBlockEntity(position) instanceof WorkbenchTile workbench)
                || !(heldGun.getItem() instanceof ModularGunItem gun)) return;
        workbench.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(capability -> {
            if (!(capability instanceof ItemStackHandler handler)) return;
            ItemStack target = handler.getStackInSlot(0).copy();
            ItemStack remainingMaterial = handler.getStackInSlot(1).copy();
            LOGGER.info("TaCZinTetra dev workbench GUI settlement observation: target={}, remainingMaterial={}, modules={}",
                    target, remainingMaterial, describeModules(gun, target));
        });
    }

    /**
     * 使用干净的枪械副本测试 Tetra 的真实处理器、蓝图和制作流程。
     * 此流程仅供开发使用；正式游戏不会绕过
     * 常规工作台或全息工作台界面。
     */
    private static void craftBodyThroughWorkbench(WorkbenchTile workbench, ServerPlayer player, ItemStack heldGun) {
        if (player.getPersistentData().getBoolean(WORKBENCH_CRAFTED)
                || !(heldGun.getItem() instanceof ModularGunItem gun)) return;
        workbench.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(capability -> {
            if (!(capability instanceof ItemStackHandler handler)) {
                LOGGER.warn("TaCZinTetra dev workbench handler is not mutable: {}", capability.getClass().getName());
                return;
            }
            ItemStack result = new ItemStack(gun);
            result = craftMainModule(workbench, handler, player, gun, result,
                    GunModuleSlots.BODY, "iron_body", "body");
            result = craftMainModule(workbench, handler, player, gun, result,
                    GunModuleSlots.BODY, "iron_body", "body replacement", "taczintetra/body_rifle");
            result = craftMainModule(workbench, handler, player, gun, result,
                    GunModuleSlots.BARREL, "iron_barrel", "barrel");
            result = craftMainModule(workbench, handler, player, gun, result,
                    GunModuleSlots.MAGAZINE, "iron_magazine", "magazine");
            if (result.isEmpty()) return;
            var profile = TetraItemStackProfileResolver.tryResolve(gun, result,
                    GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
            LOGGER.info("TaCZinTetra dev workbench crafted all major modules: result={}, profile={}", result, profile);
            ItemStack damaged = result.copy();
            damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));
            ItemStack repairMaterial = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "iron_repair")));
            var repairDefinitions = gun.getRepairDefinitions(damaged);
            handler.setStackInSlot(0, damaged);
            handler.setStackInSlot(1, repairMaterial.copy());
            String repairSlot = gun.getRepairSlot(damaged);
            UpgradeSchematic repairSchematic = SchematicRegistry.getSchematic("taczintetra_repair");
            if (!(repairSchematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic)) {
                repairSchematic = java.util.Arrays.stream(
                            SchematicRegistry.getSchematics(damaged, repairSlot))
                    .filter(schematic -> schematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic)
                    .findFirst().orElse(null);
            }
            var actionKeys = java.util.Arrays.stream(workbench.getAvailableActions(player))
                    .map(action -> action.getKey())
                    .toList();
            boolean repairActionAvailable = java.util.Arrays.stream(workbench.getAvailableActions(player))
                    .anyMatch(action -> "repair_action".equals(action.getKey()));
            int damageBeforeRepair = damaged.getDamageValue();
            boolean repairCanApply = repairSchematic != null && repairSchematic.canApplyUpgrade(player, damaged,
                    new ItemStack[]{repairMaterial.copy()}, repairSlot, java.util.Map.of());
            if (repairActionAvailable && repairSchematic != null) {
                workbench.setCurrentSchematic(repairSchematic, repairSlot);
                workbench.performAction(player, "repair_action");
                workbench.craft(player);
            }
            ItemStack repaired = handler.getStackInSlot(0).copy();
            LOGGER.info("TaCZinTetra dev repair settlement: candidates={}, configuredCount={}, repairSlot={}, schematic={}, actions={}, actionAvailable={}, canApply={}, beforeDamage={}, afterDamage={}, remainingMaterial={}",
                    repairDefinitions.size(), gun.getRepairMaterialCount(damaged, repairMaterial),
                    repairSlot, repairSchematic == null ? "<missing>" : repairSchematic.getKey(),
                    actionKeys, repairActionAvailable, repairCanApply, damageBeforeRepair, repaired.getDamageValue(),
                    handler.getStackInSlot(1).getCount());
            probeRepairMaterialMatrix(workbench, player, handler, gun, repaired, repairSchematic);
            probeTetraEnchantments(workbench, player, handler, gun, repaired);
            UpgradeSchematic finalRepair = SchematicRegistry.getSchematic("repair/taczintetra");
            if (finalRepair instanceof se.mickelus.tetra.module.schematic.RepairSchematic) {
                workbench.setCurrentSchematic(finalRepair, gun.getRepairSlot(repaired));
                LOGGER.info("TaCZinTetra dev repair GUI probe selected schematic={}, slot={}",
                        finalRepair.getKey(), gun.getRepairSlot(repaired));
            }
            probeRawMaterialRejection(workbench, player, handler, gun, repaired);
            probeBrokenState(player, gun, repaired);
            player.getPersistentData().putBoolean(WORKBENCH_CRAFTED, true);
        });
    }

    /** 在 Tetra 蓝图处理边界处，使用受损副本逐一检查已配置的修复代理。 */
    private static void probeRepairMaterialMatrix(WorkbenchTile workbench, ServerPlayer player,
                                                  ItemStackHandler handler, ModularGunItem gun,
                                                  ItemStack repaired, UpgradeSchematic repairSchematic) {
        var data = player.getPersistentData();
        if (data.getBoolean(REPAIR_MATERIAL_MATRIX_PROBE) || repaired.isEmpty()
                || !(repairSchematic instanceof se.mickelus.tetra.module.schematic.RepairSchematic)) return;
        String[] materials = {"wood_repair", "stone_repair", "iron_repair", "gold_repair", "netherite_repair"};
        for (String materialId : materials) {
            String materialPath = "taczintetra/" + materialId.replace("_repair", "") + "/";
            ItemStack target = new ItemStack(gun);
            se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                    target, GunModuleSlots.BODY, "taczintetra/body/pistol", materialPath);
            se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                    target, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", materialPath);
            se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                    target, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", materialPath);
            target.setDamageValue(Math.max(1, target.getMaxDamage() / 4));
            ItemStack material = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, materialId)));
            String slot = gun.getRepairSlot(target);
            handler.setStackInSlot(0, target.copy());
            handler.setStackInSlot(1, material.copy());
            workbench.setCurrentSchematic(repairSchematic, slot);
            boolean accepted = repairSchematic.acceptsMaterial(target, slot, 0, material);
            boolean canApply = repairSchematic.canApplyUpgrade(player, target,
                    new ItemStack[]{material.copy()}, slot, java.util.Map.of());
            LOGGER.info("TaCZinTetra dev repair material matrix: material={}, accepted={}, canApply={}, configuredCount={}, slot={}",
                    materialId, accepted, canApply, gun.getRepairMaterialCount(target, material), slot);
        }
        handler.setStackInSlot(0, repaired.copy());
        handler.setStackInSlot(1, ItemStack.EMPTY);
        data.putBoolean(REPAIR_MATERIAL_MATRIX_PROBE, true);
    }

    /** 在真实工作台边界验证“仅半成品”配置规则。 */
    private static void probeRawMaterialRejection(WorkbenchTile workbench, ServerPlayer player,
                                                   ItemStackHandler handler, ModularGunItem gun,
                                                   ItemStack repaired) {
        var data = player.getPersistentData();
        if (data.getBoolean(RAW_MATERIAL_REJECTION_PROBE) || repaired.isEmpty()) return;
        UpgradeSchematic body = SchematicRegistry.getSchematic("taczintetra/body");
        if (body == null) {
            LOGGER.warn("TaCZinTetra dev raw material rejection probe skipped: body schematic missing");
            data.putBoolean(RAW_MATERIAL_REJECTION_PROBE, true);
            return;
        }
        ItemStack target = repaired.copy();
        ItemStack raw = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot")));
        handler.setStackInSlot(0, target.copy());
        handler.setStackInSlot(1, raw.copy());
        workbench.setCurrentSchematic(body, GunModuleSlots.BODY);
        int materialBefore = handler.getStackInSlot(1).getCount();
        boolean accepted = body.acceptsMaterial(target, GunModuleSlots.BODY, 0, raw);
        boolean canApply = body.canApplyUpgrade(player, target, new ItemStack[]{raw.copy()},
                GunModuleSlots.BODY, java.util.Map.of());
        if (canApply) workbench.craft(player);
        int materialAfter = handler.getStackInSlot(1).getCount();
        boolean targetUnchanged = ItemStack.isSameItemSameTags(target, handler.getStackInSlot(0));
        LOGGER.info("TaCZinTetra dev raw material rejection: material=minecraft:iron_ingot, accepted={}, canApply={}, materialBefore={}, materialAfter={}, targetUnchanged={}",
                accepted, canApply, materialBefore, materialAfter, targetUnchanged);
        handler.setStackInSlot(0, repaired.copy());
        handler.setStackInSlot(1, ItemStack.EMPTY);
        data.putBoolean(RAW_MATERIAL_REJECTION_PROBE, true);
    }

    /** 确认可以观察到 Tetra 的损坏状态，同时不删除物品栈。 */
    private static void probeBrokenState(ServerPlayer player, ModularGunItem gun, ItemStack repaired) {
        var data = player.getPersistentData();
        if (data.getBoolean(BROKEN_PROBE) || repaired.isEmpty()) return;
        ItemStack broken = repaired.copy();
        broken.setDamageValue(Math.max(0, broken.getMaxDamage()));
        boolean isBroken = gun.isBroken(broken);
        LOGGER.info("TaCZinTetra dev broken-state probe: maxDamage={}, damage={}, isBroken={}, stackPresent={}",
                broken.getMaxDamage(), broken.getDamageValue(), isBroken, !broken.isEmpty());
        data.putBoolean(BROKEN_PROBE, true);
    }

    /** 使用真实枪械物品栈测试 Tetra 原生附魔书蓝图。 */
    private static void probeTetraEnchantments(WorkbenchTile workbench, ServerPlayer player,
                                                ItemStackHandler handler, ModularGunItem gun,
                                                ItemStack repaired) {
        var data = player.getPersistentData();
        if (data.getBoolean(ENCHANTMENT_PROBE) || repaired.isEmpty()) return;
        UpgradeSchematic schematic = SchematicRegistry.getSchematic("book_enchant");
        if (schematic == null) {
            LOGGER.warn("TaCZinTetra dev enchantment probe skipped: Tetra book_enchant schematic missing");
            data.putBoolean(ENCHANTMENT_PROBE, true);
            return;
        }
        // 默认目录中铁的 Tetra 魔力容量刻意设为零；
        // 此处改用下界合金副本，使探针独立测试附魔策略
        // 和 Tetra 原生结算逻辑，不受材料容量影响。
        ItemStack enchantTarget = new ItemStack(gun);
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                enchantTarget, GunModuleSlots.BODY, "taczintetra/body/rifle", "taczintetra/netherite/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                enchantTarget, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/netherite/");
        se.mickelus.tetra.items.modular.IModularItem.putModuleInSlot(
                enchantTarget, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/netherite/");
        var enchantBody = gun.getModuleFromSlot(enchantTarget, GunModuleSlots.BODY);
        LOGGER.info("TaCZinTetra dev enchantment target: tag={}, bodyMagicCapacity={}",
                enchantTarget.getTag(), enchantBody == null ? -1 : enchantBody.getMagicCapacityGain(enchantTarget));
        LOGGER.info("TaCZinTetra dev enchantment policy: directUnbreaking={}, bookEnchantments={}",
                gun.acceptsEnchantment(enchantTarget, Enchantments.UNBREAKING, false),
                java.util.List.of(Enchantments.UNBREAKING, Enchantments.MENDING,
                        Enchantments.VANISHING_CURSE));
        var supported = java.util.List.of(Enchantments.UNBREAKING, Enchantments.MENDING,
                Enchantments.VANISHING_CURSE);
        ItemStack current = enchantTarget.copy();
        for (var enchantment : supported) {
            ItemStack book = EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(enchantment, 1));
            boolean accepted = schematic.acceptsMaterial(current, GunModuleSlots.BODY, 0, book);
            handler.setStackInSlot(0, current.copy());
            handler.setStackInSlot(1, book.copy());
            workbench.setCurrentSchematic(schematic, GunModuleSlots.BODY);
            boolean canApply = accepted && schematic.canApplyUpgrade(player, current,
                    new ItemStack[]{book}, GunModuleSlots.BODY, java.util.Map.of());
            if (canApply) {
                try {
                    workbench.craft(player);
                } catch (RuntimeException exception) {
                    // Tetra 6.17 可能在工作台奖励流程中
                    // 使用空值 ExplosionInteraction 执行不稳定爆炸。
                    // 为保持服务端运行，并继续使用同一原生蓝图的
                    // 结算方法，此处不经过可选奖励流程。
                    LOGGER.warn("TaCZinTetra dev enchantment workbench bonus failed; applying native schematic directly: {}",
                            exception.toString());
                    ItemStack fallback = schematic.applyUpgrade(current, new ItemStack[]{book}, true,
                            GunModuleSlots.BODY, player);
                    handler.setStackInSlot(0, fallback);
                    handler.setStackInSlot(1, ItemStack.EMPTY);
                }
            }
            current = handler.getStackInSlot(0).copy();
            LOGGER.info("TaCZinTetra dev enchantment settlement: enchantment={}, accepted={}, canApply={}, resultEnchantments={}, remainingBook={}",
                    BuiltInRegistries.ENCHANTMENT.getKey(enchantment), accepted, canApply,
                    current.getEnchantmentTags(), handler.getStackInSlot(1).getCount());
        }
        ItemStack binding = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(Enchantments.BINDING_CURSE, 1));
        LOGGER.info("TaCZinTetra dev enchantment rejection: enchantment={}, accepted={}",
                BuiltInRegistries.ENCHANTMENT.getKey(Enchantments.BINDING_CURSE),
                schematic.acceptsMaterial(current, GunModuleSlots.BODY, 0, binding));
        data.putBoolean(ENCHANTMENT_PROBE, true);
    }

    private static ItemStack craftMainModule(WorkbenchTile workbench, ItemStackHandler handler,
                                             ServerPlayer player, ModularGunItem gun, ItemStack target,
                                             String slot, String materialId, String label) {
        return craftMainModule(workbench, handler, player, gun, target, slot, materialId, label, null);
    }

    private static ItemStack craftMainModule(WorkbenchTile workbench, ItemStackHandler handler,
                                             ServerPlayer player, ModularGunItem gun, ItemStack target,
                                             String slot, String materialId, String label, String schematicKey) {
        if (target.isEmpty()) {
            LOGGER.warn("TaCZinTetra dev workbench {} skipped because target stack is empty", label);
            return ItemStack.EMPTY;
        }
        handler.setStackInSlot(0, target);
        ItemStack material = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, materialId)));
        handler.setStackInSlot(1, material);
        var schematics = SchematicRegistry.getSchematics(target, slot);
        LOGGER.info("TaCZinTetra dev workbench {} schematics={}", label,
                java.util.Arrays.stream(schematics).map(schematic -> schematic.getKey()).toList());
        if (schematics.length == 0) {
            LOGGER.warn("TaCZinTetra dev workbench has no {} schematic", label);
            return ItemStack.EMPTY;
        }
        var selected = java.util.Arrays.stream(schematics)
                .filter(schematic -> schematicKey == null || schematicKey.equals(schematic.getKey()))
                .findFirst().orElse(null);
        if (selected == null) {
            LOGGER.warn("TaCZinTetra dev workbench {} has no requested schematic {}", label, schematicKey);
            return ItemStack.EMPTY;
        }
        workbench.setCurrentSchematic(selected, slot);
        LOGGER.info("TaCZinTetra dev workbench {} selected key={}, currentSlot={}, target={}",
                label, selected.getKey(), workbench.getCurrentSlot(), target);
        workbench.craft(player);
        ItemStack result = handler.getStackInSlot(0).copy();
        LOGGER.info("TaCZinTetra dev workbench crafted {}: result={}, tag={}, modules={}",
                label, result, result.getTag(), describeModules(gun, result));
        return result;
    }

    private static String describeModules(ModularGunItem gun, net.minecraft.world.item.ItemStack stack) {
        StringBuilder result = new StringBuilder();
        for (String slot : new String[]{GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE}) {
            try {
                var module = gun.getModuleFromSlot(stack, slot);
                var variantKey = module == null ? null
                        : module.getKey().substring(module.getKey().lastIndexOf('/') + 1) + "/";
                result.append(slot).append("=")
                        .append(module == null ? "null" : module.getKey())
                        .append("/")
                        .append(variantKey == null ? "null" : variantKey)
                        .append("/material=")
                        .append(module == null ? "" : stack.getOrCreateTag().getString(module.getKey() + "_material")).append("; ");
            } catch (RuntimeException exception) {
                result.append(slot).append("=ERROR:").append(exception.getClass().getSimpleName()).append("; ");
            }
        }
        return result.toString();
    }
}
