package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.blocks.workbench.gui.WorkbenchScreen;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

/** Opt-in real GUI/survival probe; all recipe inputs and the Tetra action use ordinary menus. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevSurvivalWorkbenchModuleUpgradeAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean(
            "taczintetra.dev_survival_workbench_module_upgrade_probe");
    private static final int MAX_STAGE_TICKS = 600;
    private static final int CRAFT_BUTTON_MAX_ATTEMPTS = 120;
    private static volatile Phase phase = Phase.IDLE;
    private static BlockPos tablePosition;
    private static BlockPos[] logPositions;
    private static int phaseTicks;
    private static int lastPickupTick = -MAX_STAGE_TICKS;
    private static boolean recoveryRequested;
    private static boolean craftButtonClicked;
    private static int craftButtonAttempts;
    private static boolean transactionPreparationRequested;
    private static boolean observationRequested;
    private static String beforeBody = "";
    private static int materialBefore;
    private static SurvivalModuleProfileChangeAssertion.Snapshot profileBefore;

    private enum Phase {
        IDLE, WAIT_LOGS, WAIT_PLANKS, WAIT_STICKS, WAIT_TABLE, WAIT_TABLE_PLACED,
        WAIT_CRAFTING_MENU, WAIT_SLABS, WAIT_PISTOL, WAIT_PISTOL_RESULT,
        WAIT_WOOD_BODY, WAIT_WOOD_BODY_RESULT, WAIT_HAMMER_INGREDIENTS, WAIT_HAMMER, WAIT_HAMMER_RESULT,
        WAIT_TETRA_TABLE, WAIT_WORKBENCH_GUI, WAIT_MODULE_UPGRADE, PASSED, FAILED
    }

    private DevSurvivalWorkbenchModuleUpgradeAutomation() { }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        phase = Phase.IDLE;
        phaseTicks = 0;
        lastPickupTick = -MAX_STAGE_TICKS;
        recoveryRequested = false;
        craftButtonClicked = false;
        craftButtonAttempts = 0;
        transactionPreparationRequested = false;
        observationRequested = false;
        beforeBody = "";
        materialBefore = 0;
        profileBefore = null;
        LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE probe armed; opt-in=true, items={}, {}, {}",
                BuiltInRegistries.ITEM.getKey(starterPistol()), BuiltInRegistries.ITEM.getKey(woodBody()),
                BuiltInRegistries.ITEM.getKey(modularDouble()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END
                || phase == Phase.PASSED || phase == Phase.FAILED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) return;
        var server = minecraft.getSingleplayerServer();
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (server == null || player == null) return;
        if (recoveryRequested) {
            if (minecraft.player.isAlive() && player.isAlive() && minecraft.getConnection().getPlayerInfo(player.getUUID()) != null) {
                recoveryRequested = false;
                phase = Phase.IDLE;
                phaseTicks = 0;
                LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE stale-player recovery complete");
            } else if (++phaseTicks > MAX_STAGE_TICKS) fail("bounded respawn recovery failed");
            return;
        }
        if (!player.isAlive() || !minecraft.player.isAlive()) {
            recoverDeadPlayer(minecraft, player);
            return;
        }
        if (phase == Phase.IDLE) {
            prepareSurvivalFixture(minecraft, player);
            return;
        }
        switch (phase) {
            case WAIT_LOGS -> waitForLogs(minecraft, player);
            case WAIT_PLANKS -> waitForPlanks(minecraft, player);
            case WAIT_STICKS -> waitForSticks(minecraft, player);
            case WAIT_TABLE -> waitForTable(minecraft, player);
            case WAIT_TABLE_PLACED -> waitForTablePlaced(minecraft, player);
            case WAIT_CRAFTING_MENU -> waitForCraftingMenu(minecraft, player);
            case WAIT_SLABS -> waitForSlabs(minecraft, player);
            case WAIT_PISTOL -> waitForPistolIngredients(minecraft, player);
            case WAIT_PISTOL_RESULT -> collectRecipeOutput(player, starterPistol(),
                    "survival starter pistol", Phase.WAIT_WOOD_BODY);
            case WAIT_WOOD_BODY -> waitForBodyIngredients(minecraft, player);
            case WAIT_WOOD_BODY_RESULT -> collectRecipeOutput(player, woodBody(),
                    "survival wood body", Phase.WAIT_HAMMER_INGREDIENTS);
            case WAIT_HAMMER_INGREDIENTS -> waitForHammerIngredients(minecraft, player);
            case WAIT_HAMMER -> waitForHammer(minecraft, player);
            case WAIT_HAMMER_RESULT -> collectHammerRecipe(minecraft, player);
            case WAIT_TETRA_TABLE -> waitForTetraTable(minecraft, player);
            case WAIT_WORKBENCH_GUI -> prepareWorkbenchTransaction(minecraft, player);
            case WAIT_MODULE_UPGRADE -> verifyWorkbenchTransaction(minecraft, player);
            default -> { }
        }
        if (phase != Phase.PASSED && phase != Phase.FAILED && ++phaseTicks > MAX_STAGE_TICKS) {
            fail("phase timeout=" + phase + ", inventory=" + inventorySummary(player));
        }
    }

    @SubscribeEvent
    public static void onWorkbenchRender(ScreenEvent.Render.Post event) {
        if (!ENABLED || craftButtonClicked || phase != Phase.WAIT_MODULE_UPGRADE
                || !(event.getScreen() instanceof WorkbenchScreen screen)) return;
        boolean handled = DevClientAutomation.clickWorkbenchCraftButton(screen);
        craftButtonClicked = handled;
        if (handled) {
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE visible CraftButton click accepted after {} attempt(s)",
                    craftButtonAttempts + 1);
            return;
        }
        craftButtonAttempts++;
        if (craftButtonAttempts == 1 || craftButtonAttempts % 30 == 0) {
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE CraftButton not ready/handled yet; attempt={}/{}",
                    craftButtonAttempts, CRAFT_BUTTON_MAX_ATTEMPTS);
        }
        if (craftButtonAttempts >= CRAFT_BUTTON_MAX_ATTEMPTS) {
            fail("Tetra CraftButton did not become actionable after " + craftButtonAttempts + " GUI attempts");
        }
    }

    private static void prepareSurvivalFixture(Minecraft minecraft, ServerPlayer player) {
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) player.setGameMode(GameType.SURVIVAL);
        BlockPos origin = player.blockPosition();
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();
        tablePosition = origin.relative(side);
        BlockPos support = tablePosition.below();
        BlockPos logOrigin = origin.relative(forward, 2);
        logPositions = new BlockPos[6];
        for (int i = 0; i < logPositions.length; i++) {
            logPositions[i] = logOrigin.relative(side, i % 3).relative(forward, i / 3);
        }
        var level = player.serverLevel();
        var server = level.getServer();
        minecraft.setScreen(null);
        server.execute(() -> {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || !player.isAlive()) return;
            player.closeContainer();
            player.getInventory().clearContent();
            level.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(12.0D)).forEach(ItemEntity::discard);
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            for (BlockPos pos : logPositions) {
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
            }
            int harvested = 0;
            for (BlockPos pos : logPositions) if (player.gameMode.destroyBlock(pos)) harvested++;
            player.teleportTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE fixture: player={}, gameMode=SURVIVAL, stagedLogBlocks=6, harvested={}, rawMaterialsGranted=false, tablePosition={}, logPositions={}",
                    player.getGameProfile().getName(), harvested, tablePosition, List.of(logPositions));
            if (harvested != 6) fail("survival harvest accepted " + harvested + " of 6 staged oak logs");
        });
        phase = Phase.WAIT_LOGS;
        phaseTicks = 0;
    }

    private static void waitForLogs(Minecraft minecraft, ServerPlayer player) {
        if (player.getInventory().countItem(Items.OAK_LOG) < 6) {
            pickupDrop(player);
            return;
        }
        openInventory(minecraft, player);
        int index = findInventorySlot(player, Items.OAK_LOG, 6);
        if (index < 0) { fail("six harvested logs are not in one inventory stack"); return; }
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(index);
        runMenuAction(player, "craft 24 planks from six harvested logs", () -> {
            click(player, source, 0, ClickType.PICKUP);
            click(player, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0), 0, ClickType.PICKUP);
            click(player, 0, 0, ClickType.QUICK_MOVE);
        });
        phase = Phase.WAIT_PLANKS;
        phaseTicks = 0;
    }

    private static void waitForPlanks(Minecraft minecraft, ServerPlayer player) {
        if (player.getInventory().countItem(Items.OAK_PLANKS) < 24) return;
        int index = findInventorySlot(player, Items.OAK_PLANKS, 20);
        if (index < 0) { fail("crafted planks are not available in one stack"); return; }
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(index);
        runMenuAction(player, "craft eight sticks from four survival-crafted planks", () -> {
            for (int batch = 0; batch < 2; batch++) {
                click(player, source, 0, ClickType.PICKUP);
                click(player, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0), 1, ClickType.PICKUP);
                click(player, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 0), 1, ClickType.PICKUP);
                click(player, source, 0, ClickType.PICKUP);
                click(player, 0, 0, ClickType.QUICK_MOVE);
            }
        });
        phase = Phase.WAIT_STICKS;
        phaseTicks = 0;
    }

    private static void waitForSticks(Minecraft minecraft, ServerPlayer player) {
        if (player.getInventory().countItem(Items.STICK) < 8
                || player.getInventory().countItem(Items.OAK_PLANKS) < 20) return;
        openInventory(minecraft, player);
        int index = findInventorySlot(player, Items.OAK_PLANKS, 20);
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(index);
        runMenuAction(player, "craft vanilla crafting table from four survival-crafted planks", () -> {
            click(player, source, 0, ClickType.PICKUP);
            click(player, 1, 1, ClickType.PICKUP);
            click(player, 2, 1, ClickType.PICKUP);
            click(player, 3, 1, ClickType.PICKUP);
            click(player, 4, 1, ClickType.PICKUP);
            click(player, source, 0, ClickType.PICKUP);
            click(player, 0, 0, ClickType.QUICK_MOVE);
        });
        phase = Phase.WAIT_TABLE;
        phaseTicks = 0;
    }

    private static void waitForTable(Minecraft minecraft, ServerPlayer player) {
        int tableIndex = findInventorySlot(player, Items.CRAFTING_TABLE, 1);
        if (tableIndex < 0) return;
        runServerAction(player, "place survival-crafted crafting table", () -> {
            int hotbar = ensureHotbarSlot(player, tableIndex, 0, false);
            player.getInventory().selected = hotbar;
            InteractionResult result = player.gameMode.useItemOn(player, player.serverLevel(),
                    player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(tablePosition.below(), Direction.UP));
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE crafting-table placement={}", result);
        });
        phase = Phase.WAIT_TABLE_PLACED;
        phaseTicks = 0;
    }

    private static void waitForTablePlaced(Minecraft minecraft, ServerPlayer player) {
        if (!minecraft.level.getBlockState(tablePosition).is(Blocks.CRAFTING_TABLE)) return;
        minecraft.setScreen(null);
        runServerAction(player, "open survival-crafted 3x3 crafting menu", () -> {
            player.getInventory().selected = firstEmptyHotbar(player);
            player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, hit(tablePosition, Direction.UP));
        });
        phase = Phase.WAIT_CRAFTING_MENU;
        phaseTicks = 0;
    }

    private static void waitForCraftingMenu(Minecraft minecraft, ServerPlayer player) {
        if (!(player.containerMenu instanceof CraftingMenu menu) || menu.getGridWidth() != 3
                || !(minecraft.screen instanceof CraftingScreen)) return;
        beginRecipe(player, new String[]{"PPP", "   ", "   "}, Map.of('P', source(player, Items.OAK_PLANKS, 3)),
                "craft six oak slabs", Phase.WAIT_SLABS);
    }

    private static void waitForSlabs(Minecraft minecraft, ServerPlayer player) {
        ItemStack result = player.containerMenu.getSlot(0).getItem();
        if (result.isEmpty() || !result.is(Items.OAK_SLAB)) return;
        runMenuAction(player, "collect six slabs from real crafting result slot", () -> click(player, 0, 0, ClickType.QUICK_MOVE));
        phase = Phase.WAIT_PISTOL;
        phaseTicks = 0;
    }

    private static void waitForPistolIngredients(Minecraft minecraft, ServerPlayer player) {
        if (player.getInventory().countItem(Items.OAK_SLAB) < 6) return;
        beginRecipe(player, new String[]{"PPP", "PS ", "L  "}, Map.of(
                        'P', source(player, Items.OAK_PLANKS, 4),
                        'S', source(player, Items.STICK, 1),
                        'L', source(player, Items.OAK_SLAB, 1)),
                "craft starter pistol through its registered recipe", Phase.WAIT_PISTOL_RESULT);
    }

    private static void waitForBodyIngredients(Minecraft minecraft, ServerPlayer player) {
        if (player.getInventory().countItem(starterPistol()) < 1) return;
        beginRecipe(player, new String[]{"PPP", "P P", "S S"}, Map.of(
                        'P', source(player, Items.OAK_PLANKS, 5),
                        'S', source(player, Items.STICK, 2)),
                "craft taczintetra:wood_body through its registered recipe", Phase.WAIT_WOOD_BODY_RESULT);
    }

    private static void waitForHammerIngredients(Minecraft minecraft, ServerPlayer player) {
        if (player.getInventory().countItem(woodBody()) < 1) return;
        beginRecipe(player, TetraWorkbenchSurvivalProgressionPlan.OAK_HAMMER_PATTERN.toArray(String[]::new),
                Map.of('#', source(player, Items.OAK_PLANKS, 2), '/', source(player, Items.STICK, 2)),
                "craft Tetra oak mallet through its registered recipe", Phase.WAIT_HAMMER_RESULT);
    }

    private static void collectRecipeOutput(ServerPlayer player, Item expected, String label, Phase next) {
        ItemStack result = player.containerMenu.getSlot(0).getItem();
        if (result.isEmpty() || !result.is(expected)) return;
        runMenuAction(player, "collect " + label + " from the real result slot", () -> click(player, 0, 0, ClickType.QUICK_MOVE));
        phase = next;
        phaseTicks = 0;
    }

    private static void collectHammerRecipe(Minecraft minecraft, ServerPlayer player) {
        ItemStack result = player.containerMenu.getSlot(0).getItem();
        if (!result.is(modularDouble()) || result.getTag() == null
                || !result.getTag().getString("double/basic_hammer_left_material").equals("basic_hammer/oak")) return;
        runMenuAction(player, "collect Tetra oak hammer from the real result slot", () ->
                click(player, 0, 0, ClickType.QUICK_MOVE));
        phase = Phase.WAIT_HAMMER;
        phaseTicks = 0;
    }

    private static void beginRecipe(ServerPlayer player, String[] pattern, Map<Character, Integer> sources,
                                    String label, Phase waitingForOutput) {
        List<CraftingMenuRecipePlacement.Click> clicks = CraftingMenuRecipePlacement.plan(pattern, sources);
        runMenuAction(player, label, () -> {
            if (!(player.containerMenu instanceof CraftingMenu menu) || menu.getGridWidth() != 3) {
                fail("recipe staging requires the actual 3x3 CraftingMenu: " + label);
                return;
            }
            for (var click : clicks) click(player, click.slotId(), click.button(), ClickType.PICKUP);
        });
        phase = waitingForOutput;
        phaseTicks = 0;
    }

    private static void waitForHammer(Minecraft minecraft, ServerPlayer player) {
        int slot = findInventorySlot(player, modularDouble(), 1);
        if (slot < 0) return;
        runServerAction(player, "collect and use oak mallet to upgrade table", () -> {
            ItemStack hammer = player.getInventory().getItem(slot);
            if (hammer.getTag() == null || !hammer.getTag().getString("double/basic_hammer_left_material")
                    .equals("basic_hammer/oak")) { fail("survival-crafted oak hammer has unexpected recipe NBT"); return; }
            int hotbar = ensureHotbarSlot(player, slot, 0, true);
            player.getInventory().selected = hotbar;
            player.closeContainer();
            InteractionResult result = player.gameMode.useItemOn(player, player.serverLevel(),
                    player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(tablePosition, Direction.UP));
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE real mallet use: result={}, held={}", result,
                    player.getMainHandItem());
        });
        phase = Phase.WAIT_TETRA_TABLE;
        phaseTicks = 0;
    }

    private static void waitForTetraTable(Minecraft minecraft, ServerPlayer player) {
        if (!BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(tablePosition).getBlock())
                .equals(ResourceLocation.parse("tetra:basic_workbench"))) return;
        minecraft.setScreen(null);
        runServerAction(player, "open acquired Tetra workbench", () -> {
            player.getInventory().selected = firstEmptyHotbar(player);
            player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, hit(tablePosition, Direction.UP));
        });
        phase = Phase.WAIT_WORKBENCH_GUI;
        phaseTicks = 0;
    }

    private static void prepareWorkbenchTransaction(Minecraft minecraft, ServerPlayer player) {
        if (!(minecraft.screen instanceof WorkbenchScreen)
                || player.containerMenu == player.inventoryMenu || transactionPreparationRequested) return;
        transactionPreparationRequested = true;
        minecraft.getSingleplayerServer().execute(() -> prepareWorkbenchTransactionOnServer(minecraft, player));
    }

    private static void prepareWorkbenchTransactionOnServer(Minecraft minecraft, ServerPlayer player) {
        if (phase != Phase.WAIT_WORKBENCH_GUI || player.containerMenu == player.inventoryMenu) return;
        var server = minecraft.getSingleplayerServer();
        var tile = server.getLevel(minecraft.level.dimension()).getBlockEntity(tablePosition);
        if (!(tile instanceof WorkbenchTile workbench)) { fail("Tetra workbench block entity is missing"); return; }
        var handler = workbench.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (!(handler instanceof ItemStackHandler stacks)) { fail("Tetra workbench inventory capability is unavailable"); return; }
        int gunIndex = findInventorySlot(player, starterPistol(), 1);
        int bodyIndex = findInventorySlot(player, woodBody(), 1);
        if (gunIndex < 0 || bodyIndex < 0) { fail("survival-crafted gun/body missing from inventory"); return; }
        clearWorkbenchSlotsThroughMenu(player, stacks);
        if (!moveToWorkbench(player, gunIndex, 0) || !moveToWorkbench(player, bodyIndex, 1)) {
            fail("survival-crafted gun or wood body could not be moved through the Tetra container");
            return;
        }
        UpgradeSchematic schematic = SchematicRegistry.getSchematic("taczintetra/body_rifle");
        if (schematic == null) { fail("Tetra body_rifle schematic is unavailable"); return; }
        workbench.setCurrentSchematic(schematic, GunModuleSlots.BODY);
        ItemStack target = stacks.getStackInSlot(0).copy();
        ItemStack material = stacks.getStackInSlot(1).copy();
        ModularGunItem gun = (ModularGunItem) target.getItem();
        profileBefore = SurvivalModuleProfileChangeAssertion.snapshot(gun, target);
        var module = gun.getModuleFromSlot(target, GunModuleSlots.BODY);
        beforeBody = module == null ? "" : module.getKey();
        materialBefore = material.getCount();
        boolean canApply = schematic.canApplyUpgrade(player, target, new ItemStack[]{material.copy()},
                GunModuleSlots.BODY, Map.of());
        player.containerMenu.broadcastChanges();
        LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE prepared: schematic={}, beforeBody={}, target={}, material={}, materialCount={}, canApply={}, gameMode={}",
                schematic.getKey(), beforeBody, target, material, materialBefore, canApply,
                player.gameMode.getGameModeForPlayer());
        if (!canApply || !beforeBody.equals("taczintetra/body/pistol") || materialBefore != 1
                || profileBefore == null) {
            fail("prepared module transaction did not match the survival body upgrade preconditions");
            return;
        }
        phase = Phase.WAIT_MODULE_UPGRADE;
        phaseTicks = 0;
    }

    private static void verifyWorkbenchTransaction(Minecraft minecraft, ServerPlayer player) {
        if (!craftButtonClicked || observationRequested) return;
        observationRequested = true;
        minecraft.getSingleplayerServer().execute(() -> verifyWorkbenchTransactionOnServer(minecraft, player));
    }

    private static void verifyWorkbenchTransactionOnServer(Minecraft minecraft, ServerPlayer player) {
        if (phase != Phase.WAIT_MODULE_UPGRADE) return;
        var tile = player.serverLevel().getBlockEntity(tablePosition);
        if (!(tile instanceof WorkbenchTile workbench)) { fail("Tetra workbench disappeared during crafting"); return; }
        var handler = workbench.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (!(handler instanceof ItemStackHandler stacks)) { fail("Tetra workbench inventory disappeared"); return; }
        ItemStack result = stacks.getStackInSlot(0);
        if (!(result.getItem() instanceof ModularGunItem gun)) return;
        var module = gun.getModuleFromSlot(result, GunModuleSlots.BODY);
        String after = module == null ? "" : module.getKey();
        var assertion = WorkbenchGuiCraftAssertion.evaluate(beforeBody, "taczintetra/body/rifle", after,
                materialBefore, stacks.getStackInSlot(1).getCount());
        if (!assertion.passed()) {
            observationRequested = false;
            return;
        }
        var profileAfter = SurvivalModuleProfileChangeAssertion.snapshot(gun, result);
        var profileAssertion = SurvivalModuleProfileChangeAssertion.evaluate(profileBefore, profileAfter);
        if (!profileAssertion.passed()) {
            fail("runtime gun profile did not change after settled body upgrade: " + profileAssertion.reason()
                    + ", before=" + profileBefore + ", after=" + profileAfter);
            return;
        }
        LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE_ASSERTION PASS pid={}, player={}, mode=SURVIVAL, gunCraftedByRecipe=true, physicalPartCraftedByRecipe=true, workbenchAcquiredByMallet=true, beforeBody={}, afterBody={}, rpm={}->{}, damage={}->{}, recoil={}->{}, twoHanded={}->{}, materialBefore={}, materialAfter={}, materialPath={}, gui={}, normalMenuTransaction=true, rawMaterialsGranted=false",
                ProcessHandle.current().pid(), player.getGameProfile().getName(), beforeBody, after,
                profileBefore.roundsPerMinute(), profileAfter.roundsPerMinute(),
                profileBefore.damage(), profileAfter.damage(), profileBefore.recoil(), profileAfter.recoil(),
                profileBefore.twoHanded(), profileAfter.twoHanded(),
                materialBefore, stacks.getStackInSlot(1).getCount(),
                result.getOrCreateTag().getString(after + "_material"),
                minecraft.screen.getClass().getSimpleName());
        phase = Phase.PASSED;
        minecraft.execute(minecraft::stop);
    }

    private static void clearWorkbenchSlotsThroughMenu(ServerPlayer player, ItemStackHandler handler) {
        for (int slot = 0; slot < Math.min(4, handler.getSlots()); slot++) {
            ItemStack old = handler.getStackInSlot(slot);
            if (old.isEmpty()) continue;
            int empty = findEmptySlot(player);
            if (empty < 0) throw new IllegalStateException("No inventory slot for existing workbench item");
            click(player, slot, 0, ClickType.PICKUP);
            click(player, TetraWorkbenchSlotLayout.playerInventoryMenuSlot(empty), 0, ClickType.PICKUP);
        }
    }

    private static boolean moveToWorkbench(ServerPlayer player, int inventoryIndex, int workbenchSlot) {
        int source = TetraWorkbenchSlotLayout.playerInventoryMenuSlot(inventoryIndex);
        ItemStack expected = player.getInventory().getItem(inventoryIndex).copy();
        click(player, source, 0, ClickType.PICKUP);
        click(player, workbenchSlot, 0, ClickType.PICKUP);
        click(player, source, 0, ClickType.PICKUP);
        return ItemStack.isSameItemSameTags(expected, player.containerMenu.getSlot(workbenchSlot).getItem())
                && player.containerMenu.getCarried().isEmpty();
    }

    private static void recoverDeadPlayer(Minecraft minecraft, ServerPlayer player) {
        if (recoveryRequested) return;
        recoveryRequested = true;
        phaseTicks = 0;
        var connection = player.connection;
        var level = player.serverLevel();
        BlockPos spawn = minecraft.player.blockPosition();
        float yaw = minecraft.player.getYRot();
        minecraft.getSingleplayerServer().execute(() -> {
            level.setBlock(spawn.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(spawn, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(spawn.above(), Blocks.AIR.defaultBlockState(), 3);
            player.setRespawnPosition(level.dimension(), spawn, yaw, true, false);
            ServerPlayer replacement = minecraft.getSingleplayerServer().getPlayerList().respawn(player, false);
            connection.player = replacement;
            LOGGER.warn("TETRA_SURVIVAL_MODULE_UPGRADE stale player respawned: alive={}, connectionRebound={}",
                    replacement.isAlive(), connection.player == replacement);
        });
    }

    private static void pickupDrop(ServerPlayer player) {
        int tick = phaseTicks;
        if (!SurvivalProbePickupCadencePolicy.shouldAssist(tick, lastPickupTick, 25)) return;
        ItemEntity drop = player.serverLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(player.blockPosition()).inflate(12.0D)).stream()
                .filter(entity -> entity.isAlive() && entity.getItem().is(Items.OAK_LOG) && entity.tickCount >= 20)
                .findFirst().orElse(null);
        if (drop == null) return;
        lastPickupTick = tick;
        LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE pickup assist scheduled: tick={}, heldLogs={}, drop={}, age={}, playerPos={}, dropPos={}",
                tick, player.getInventory().countItem(Items.OAK_LOG), drop.getItem(), drop.tickCount,
                player.position(), drop.position());
        player.server.execute(() -> player.connection.teleport(drop.getX(), drop.getY(), drop.getZ(),
                player.getYRot(), player.getXRot()));
    }

    private static void openInventory(Minecraft minecraft, ServerPlayer player) {
        if (minecraft.screen == null) minecraft.setScreen(new InventoryScreen(minecraft.player));
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
    }

    private static int source(ServerPlayer player, Item item, int count) {
        int index = findInventorySlot(player, item, count);
        if (index < 0) throw new IllegalStateException("Missing recipe ingredient " + item + " x" + count);
        return SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(index);
    }

    private static int findInventorySlot(ServerPlayer player, Item item, int count) {
        for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).is(item)
                && player.getInventory().getItem(i).getCount() >= count) return i;
        return -1;
    }

    private static int findEmptySlot(ServerPlayer player) {
        for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) return i;
        return -1;
    }

    private static int ensureHotbarSlot(ServerPlayer player, int inventoryIndex, int preferred, boolean craftingTable) {
        if (inventoryIndex < 9) return inventoryIndex;
        int menuSlot = craftingTable ? SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(inventoryIndex)
                : SurvivalCraftingSlotLayout.playerInventoryMenuSlot(inventoryIndex);
        click(player, menuSlot, preferred, ClickType.SWAP);
        return preferred;
    }

    private static int firstEmptyHotbar(ServerPlayer player) {
        for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).isEmpty()) return i;
        return 8;
    }

    private static void runMenuAction(ServerPlayer player, String action, Runnable operation) {
        player.server.execute(() -> {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                fail("player left Survival before menu action: " + action); return;
            }
            operation.run();
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE menu action: {}, menu={}, inventory={}", action,
                    player.containerMenu.getClass().getSimpleName(), inventorySummary(player));
        });
    }

    private static void runServerAction(ServerPlayer player, String action, Runnable operation) {
        player.server.execute(() -> {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                fail("player left Survival before world action: " + action); return;
            }
            operation.run();
            LOGGER.info("TETRA_SURVIVAL_MODULE_UPGRADE world action: {}", action);
        });
    }

    private static void click(ServerPlayer player, int slot, int button, ClickType type) {
        player.containerMenu.clicked(slot, button, type, player);
    }

    private static int firstEmptyInventorySlot(ServerPlayer player) { return findEmptySlot(player); }

    private static String inventorySummary(ServerPlayer player) {
        return "logs=" + player.getInventory().countItem(Items.OAK_LOG)
                + ",planks=" + player.getInventory().countItem(Items.OAK_PLANKS)
                + ",sticks=" + player.getInventory().countItem(Items.STICK)
                + ",pistols=" + player.getInventory().countItem(starterPistol())
                + ",woodBodies=" + player.getInventory().countItem(woodBody());
    }

    private static Item starterPistol() { return ModItems.STARTER_PISTOL.get(); }

    private static Item woodBody() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse("taczintetra:wood_body"));
    }

    private static Item modularDouble() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse("tetra:modular_double"));
    }

    private static BlockHitResult hit(BlockPos position, Direction face) {
        Vec3 location = Vec3.atCenterOf(position);
        if (face == Direction.UP) location = location.add(0, 0.5D, 0);
        return new BlockHitResult(location, face, position, false);
    }

    private static boolean timedOut(String stage) {
        if (phaseTicks-- > 0) return false;
        fail("timeout phase=" + phase + ", stage=" + stage);
        return true;
    }

    private static void fail(String reason) {
        Phase failed = phase;
        phase = Phase.FAILED;
        LOGGER.error("TETRA_SURVIVAL_MODULE_UPGRADE_ASSERTION FAIL pid={}, phase={}, reason={}",
                ProcessHandle.current().pid(), failed, reason);
        Minecraft.getInstance().execute(Minecraft.getInstance()::stop);
    }
}
