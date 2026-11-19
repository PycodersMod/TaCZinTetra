package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import se.mickelus.tetra.blocks.workbench.gui.WorkbenchScreen;

import java.util.List;

/** Opt-in real-client probe for the ordinary survival progression into a Tetra workbench. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevSurvivalTetraWorkbenchAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_survival_tetra_workbench_probe");
    private static final int MAX_STAGE_TICKS = 400;
    private static volatile Phase phase = Phase.IDLE;
    private static int phaseTicks;
    private static BlockPos tablePosition;
    private static BlockPos supportPosition;
    private static BlockPos logPosition;
    private static int hammerInventoryIndex = -1;
    private static int lastPickupAssistTick = -MAX_STAGE_TICKS;
    private static boolean deadPlayerRecoveryRequested;

    private enum Phase {
        IDLE,
        WAIT_LOGS,
        WAIT_PLANKS,
        WAIT_STICKS,
        WAIT_CRAFTING_TABLE,
        WAIT_TABLE_PLACED,
        WAIT_CRAFTING_MENU,
        WAIT_HAMMER,
        WAIT_UPGRADED_WORKBENCH,
        WAIT_WORKBENCH_GUI,
        PASSED,
        FAILED
    }

    private DevSurvivalTetraWorkbenchAutomation() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        phase = Phase.IDLE;
        phaseTicks = 0;
        tablePosition = null;
        supportPosition = null;
        logPosition = null;
        hammerInventoryIndex = -1;
        lastPickupAssistTick = -MAX_STAGE_TICKS;
        deadPlayerRecoveryRequested = false;
        LOGGER.info("TETRA_WORKBENCH_SURVIVAL probe armed for a real local client session");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == Phase.PASSED || phase == Phase.FAILED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) return;
        var server = minecraft.getSingleplayerServer();
        ServerPlayer serverPlayer = server == null ? null : server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (server == null || serverPlayer == null) return;
        if (deadPlayerRecoveryRequested) {
            if (SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(
                    minecraft.player.isAlive(), serverPlayer.isAlive(), true)) {
                deadPlayerRecoveryRequested = false;
                phase = Phase.IDLE;
                phaseTicks = 0;
                lastPickupAssistTick = -MAX_STAGE_TICKS;
                LOGGER.info("TETRA_WORKBENCH_SURVIVAL stale-player recovery complete; restarting with a fresh survival fixture");
            } else if (++phaseTicks > MAX_STAGE_TICKS) {
                fail("integrated-server respawn did not restore client/server player identity within the recovery limit");
            }
            return;
        }
        if (!minecraft.player.isAlive() || !serverPlayer.isAlive()) {
            if (SurvivalProbeRecoveryPolicy.shouldRecover(serverPlayer.isAlive(), deadPlayerRecoveryRequested)) {
                recoverDeadProbePlayer(minecraft, serverPlayer);
                return;
            }
            fail("controlled player remained dead after the bounded respawn recovery");
            return;
        }
        if (phase == Phase.IDLE) {
            prepareFixture(minecraft, serverPlayer);
            return;
        }

        switch (phase) {
            case WAIT_LOGS -> waitForLogs(minecraft, serverPlayer);
            case WAIT_PLANKS -> waitForPlanks(minecraft, serverPlayer);
            case WAIT_STICKS -> waitForSticks(minecraft, serverPlayer);
            case WAIT_CRAFTING_TABLE -> waitForCraftingTable(minecraft, serverPlayer);
            case WAIT_TABLE_PLACED -> waitForTablePlacement(minecraft, serverPlayer);
            case WAIT_CRAFTING_MENU -> waitForCraftingMenu(minecraft, serverPlayer);
            case WAIT_HAMMER -> waitForHammer(minecraft, serverPlayer);
            case WAIT_UPGRADED_WORKBENCH -> waitForUpgradedWorkbench(minecraft, serverPlayer);
            case WAIT_WORKBENCH_GUI -> waitForWorkbenchGui(minecraft, serverPlayer);
            default -> { }
        }
    }

    private static void prepareFixture(Minecraft minecraft, ServerPlayer serverPlayer) {
        BlockPos origin = serverPlayer.blockPosition();
        Direction facing = serverPlayer.getDirection();
        tablePosition = origin.relative(facing.getClockWise());
        supportPosition = tablePosition.below();
        logPosition = origin.relative(facing, 2);
        var serverLevel = serverPlayer.serverLevel();
        minecraft.setScreen(null);
        serverLevel.getServer().execute(() -> {
            if (serverPlayer.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                serverPlayer.setGameMode(GameType.SURVIVAL);
            }
            serverPlayer.closeContainer();
            serverLevel.getEntitiesOfClass(ItemEntity.class, serverPlayer.getBoundingBox().inflate(6.0D))
                    .forEach(ItemEntity::discard);
            serverPlayer.getInventory().clearContent();
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
            serverPlayer.fallDistance = 0.0F;
            serverLevel.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(tablePosition.below(), Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(logPosition.below(), Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(logPosition, Blocks.OAK_LOG.defaultBlockState(), 3);
            serverLevel.setBlock(logPosition.above(), Blocks.OAK_LOG.defaultBlockState(), 3);
            boolean firstBroken = serverPlayer.gameMode.destroyBlock(logPosition);
            boolean secondBroken = serverPlayer.gameMode.destroyBlock(logPosition.above());
            List<String> dropsAfterHarvest = serverLevel.getEntitiesOfClass(ItemEntity.class,
                            new net.minecraft.world.phys.AABB(logPosition).inflate(6.0D)).stream()
                    .map(entity -> BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()) + "x"
                            + entity.getItem().getCount() + "@" + entity.position()
                            + ":age=" + entity.tickCount + ":alive=" + entity.isAlive())
                    .toList();
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL harvest diagnostics: firstAccepted={}, secondAccepted={}, lowerBlock={}, upperBlock={}, drops={}, serverPlayerPos={}, clientPlayerPos={}, mode={}",
                    firstBroken, secondBroken,
                    BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(logPosition).getBlock()),
                    BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(logPosition.above()).getBlock()),
                    dropsAfterHarvest, serverPlayer.position(), minecraft.player.position(),
                    serverPlayer.gameMode.getGameModeForPlayer());
            if (!firstBroken || !secondBroken) {
                fail("survival destroyBlock did not break both staged oak logs; first=" + firstBroken + ", second=" + secondBroken);
                return;
            }
            serverPlayer.teleportTo(logPosition.getX() + 0.5D,
                    logPosition.getY(), logPosition.getZ() + 0.5D);
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL fixture prepared: player={}, mode={}, stagedLogBlocks=2, rawMaterialsGranted=false, tablePosition={}, supportPosition={}, logPosition={}",
                    serverPlayer.getGameProfile().getName(), serverPlayer.gameMode.getGameModeForPlayer(),
                    tablePosition, supportPosition, logPosition);
        });
        phase = Phase.WAIT_LOGS;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void recoverDeadProbePlayer(Minecraft minecraft, ServerPlayer serverPlayer) {
        var server = minecraft.getSingleplayerServer();
        var serverLevel = serverPlayer.serverLevel();
        var clientPlayer = minecraft.player;
        BlockPos safeSpawn = clientPlayer.blockPosition();
        float yaw = clientPlayer.getYRot();
        var connection = serverPlayer.connection;
        deadPlayerRecoveryRequested = true;
        phaseTicks = 0;
        LOGGER.warn("TETRA_WORKBENCH_SURVIVAL stale-player recovery requested: player={}, deathTime={}, pos={}, dimension={}, safeSpawn={}",
                clientPlayer.getGameProfile().getName(), clientPlayer.deathTime, clientPlayer.blockPosition(),
                serverLevel.dimension().location(), safeSpawn);
        server.execute(() -> {
            serverLevel.setBlock(safeSpawn.below(), Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(safeSpawn, Blocks.AIR.defaultBlockState(), 3);
            serverLevel.setBlock(safeSpawn.above(), Blocks.AIR.defaultBlockState(), 3);
            serverPlayer.setRespawnPosition(serverLevel.dimension(), safeSpawn, yaw, true, false);
            ServerPlayer respawned = server.getPlayerList().respawn(serverPlayer, false);
            connection.player = respawned;
            LOGGER.warn("TETRA_WORKBENCH_SURVIVAL stale-player recovery respawned: player={}, alive={}, pos={}, returnedPlayerPresent={}, connectionPlayerMatchesServerPlayer={}",
                    respawned.getGameProfile().getName(), respawned.isAlive(), respawned.position(),
                    server.getPlayerList().getPlayer(respawned.getUUID()) == respawned,
                    connection.player == respawned);
        });
    }

    private static void waitForLogs(Minecraft minecraft, ServerPlayer serverPlayer) {
        int logs = minecraft.player.getInventory().countItem(Items.OAK_LOG);
        if (logs < 2) {
            pickupDroppedLogs(serverPlayer);
            if (timedOut("two harvested oak logs did not enter the survival inventory; count=" + logs)) return;
            return;
        }
        minecraft.setScreen(new InventoryScreen(minecraft.player));
        ItemStack logStack = minecraft.player.getInventory().items.stream()
                .filter(stack -> stack.is(Items.OAK_LOG) && stack.getCount() >= 2).findFirst().orElse(ItemStack.EMPTY);
        int inventoryIndex = minecraft.player.getInventory().items.indexOf(logStack);
        if (inventoryIndex < 0) {
            fail("harvested oak logs were not found in player inventory slots");
            return;
        }
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(inventoryIndex);
        runMenuAction(serverPlayer, "craft eight planks from two harvested logs", () -> {
            click(serverPlayer, source, 0, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0), 0, ClickType.PICKUP);
            click(serverPlayer, 0, 0, ClickType.QUICK_MOVE);
        });
        phase = Phase.WAIT_PLANKS;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForPlanks(Minecraft minecraft, ServerPlayer serverPlayer) {
        int planks = minecraft.player.getInventory().countItem(Items.OAK_PLANKS);
        if (planks < 8) {
            if (timedOut("vanilla inventory crafting did not produce eight oak planks; count=" + planks)) return;
            return;
        }
        int inventoryIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 8);
        if (inventoryIndex < 0) {
            fail("eight crafted planks were not present in a single inventory stack");
            return;
        }
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(inventoryIndex);
        runMenuAction(serverPlayer, "craft four sticks from two planks", () -> {
            click(serverPlayer, source, 0, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0), 1, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 0), 1, ClickType.PICKUP);
            click(serverPlayer, source, 0, ClickType.PICKUP);
            click(serverPlayer, 0, 0, ClickType.PICKUP);
            moveCarriedToEmptyInventory(serverPlayer);
        });
        phase = Phase.WAIT_STICKS;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForSticks(Minecraft minecraft, ServerPlayer serverPlayer) {
        int sticks = minecraft.player.getInventory().countItem(Items.STICK);
        int planks = minecraft.player.getInventory().countItem(Items.OAK_PLANKS);
        if (sticks < 4 || planks < 6) {
            if (timedOut("stick crafting did not settle with four sticks and six remaining planks; sticks=" + sticks + ", planks=" + planks)) return;
            return;
        }
        int inventoryIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 6);
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(inventoryIndex);
        runMenuAction(serverPlayer, "craft one vanilla crafting table", () -> {
            click(serverPlayer, source, 0, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0), 1, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 1), 1, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 0), 1, ClickType.PICKUP);
            click(serverPlayer, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 1), 1, ClickType.PICKUP);
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL crafting-table recipe staged: result={}, craftSlots={}, cursor={}, inventoryPlanks={}",
                    serverPlayer.containerMenu.getSlot(0).getItem(), craftSlots(serverPlayer),
                    serverPlayer.containerMenu.getCarried(), serverPlayer.getInventory().countItem(Items.OAK_PLANKS));
            click(serverPlayer, source, 0, ClickType.PICKUP);
            click(serverPlayer, 0, 0, ClickType.QUICK_MOVE);
        });
        phase = Phase.WAIT_CRAFTING_TABLE;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForCraftingTable(Minecraft minecraft, ServerPlayer serverPlayer) {
        int tableInventoryIndex = findInventorySlot(minecraft, Items.CRAFTING_TABLE, 1);
        if (tableInventoryIndex < 0) {
            if (timedOut("vanilla crafting-table recipe output did not enter inventory; planks="
                    + minecraft.player.getInventory().countItem(Items.OAK_PLANKS)
                    + ", cursor=" + serverPlayer.containerMenu.getCarried()
                    + ", craftSlots=" + craftSlots(serverPlayer))) return;
            return;
        }
        runServerAction(serverPlayer, "place the crafted vanilla table", () -> {
            int hotbarIndex = ensureHotbarSlot(serverPlayer, tableInventoryIndex, 0);
            serverPlayer.getInventory().selected = hotbarIndex;
            serverPlayer.containerMenu.broadcastChanges();
            BlockHitResult hit = hit(supportPosition, Direction.UP);
            InteractionResult result = serverPlayer.gameMode.useItemOn(serverPlayer, serverPlayer.serverLevel(),
                    serverPlayer.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL vanilla table placement: result={}, support={}, target={}, held={}",
                    result, supportPosition, tablePosition, serverPlayer.getMainHandItem());
        });
        phase = Phase.WAIT_TABLE_PLACED;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForTablePlacement(Minecraft minecraft, ServerPlayer serverPlayer) {
        if (!minecraft.level.getBlockState(tablePosition).is(Blocks.CRAFTING_TABLE)) {
            if (timedOut("crafted vanilla crafting table was not placed through Item.useOn; block="
                    + BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(tablePosition).getBlock()))) return;
            return;
        }
        if (minecraft.screen == null) minecraft.setScreen(new InventoryScreen(minecraft.player));
        runServerAction(serverPlayer, "open placed crafting table through its block interaction", () -> {
            serverPlayer.getInventory().selected = firstEmptyHotbarSlot(serverPlayer);
            serverPlayer.gameMode.useItemOn(serverPlayer, serverPlayer.serverLevel(), ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, hit(tablePosition, Direction.UP));
        });
        phase = Phase.WAIT_CRAFTING_MENU;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForCraftingMenu(Minecraft minecraft, ServerPlayer serverPlayer) {
        if (!(serverPlayer.containerMenu instanceof CraftingMenu menu)
                || menu.getGridWidth() != 3 || !(minecraft.screen instanceof CraftingScreen)) {
            if (timedOut("placed crafting table did not open the real three-by-three CraftingMenu; serverMenu="
                    + serverPlayer.containerMenu.getClass().getName() + ", clientScreen="
                    + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName()))) return;
            return;
        }
        int planksIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 2);
        int sticksIndex = findInventorySlot(minecraft, Items.STICK, 2);
        if (planksIndex < 0 || sticksIndex < 0) {
            fail("survival inventory lacks two planks or two sticks for Tetra's oak hammer recipe; planks="
                    + minecraft.player.getInventory().countItem(Items.OAK_PLANKS) + ", sticks="
                    + minecraft.player.getInventory().countItem(Items.STICK));
            return;
        }
        var recipeId = ResourceLocation.parse(TetraWorkbenchSurvivalProgressionPlan.OAK_HAMMER_RECIPE_ID);
        var recipe = minecraft.level.getRecipeManager().byKey(recipeId).orElse(null);
        if (recipe == null || !BuiltInRegistries.ITEM.getKey(recipe.getResultItem(minecraft.level.registryAccess()).getItem())
                .equals(ResourceLocation.parse("tetra:modular_double"))) {
            fail("pinned Tetra oak hammer recipe did not resolve to tetra:modular_double");
            return;
        }
        int plankMenuSlot = SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(planksIndex);
        int stickMenuSlot = SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(sticksIndex);
        List<CraftingMenuRecipePlacement.Click> clicks = TetraWorkbenchSurvivalProgressionPlan
                .oakHammerClicks(plankMenuSlot, stickMenuSlot);
        runMenuAction(serverPlayer, "place Tetra oak hammer recipe ingredients in the real crafting-table menu", () -> {
            for (var click : clicks) click(serverPlayer, click.slotId(), click.button(), ClickType.PICKUP);
        });
        phase = Phase.WAIT_HAMMER;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForHammer(Minecraft minecraft, ServerPlayer serverPlayer) {
        var menu = serverPlayer.containerMenu;
        ItemStack result = menu.getSlot(0).getItem();
        if (result.isEmpty()) {
            if (timedOut("Tetra oak hammer recipe result did not appear in the real CraftingMenu")) return;
            return;
        }
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (!resultId.equals(ResourceLocation.parse("tetra:modular_double"))
                || result.getTag() == null
                || !result.getTag().getString("double/basic_hammer_left_material").equals("basic_hammer/oak")) {
            fail("Tetra oak hammer output differs from the verified recipe; item=" + resultId + ", tag=" + result.getTag());
            return;
        }
        var hammerItem = result.getItem();
        runServerAction(serverPlayer, "collect and use the real oak mallet on the vanilla table", () -> {
            click(serverPlayer, 0, 0, ClickType.QUICK_MOVE);
            hammerInventoryIndex = findInventorySlot(serverPlayer, hammerItem, 1);
            if (hammerInventoryIndex < 0) {
                fail("crafted Tetra oak hammer could not be collected into survival inventory");
                return;
            }
            int hotbarIndex = ensureHotbarSlot(serverPlayer, hammerInventoryIndex, 0);
            serverPlayer.getInventory().selected = hotbarIndex;
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.closeContainer();
            ItemStack hammer = serverPlayer.getMainHandItem();
            InteractionResult interaction = serverPlayer.gameMode.useItemOn(serverPlayer,
                    serverPlayer.serverLevel(), hammer, InteractionHand.MAIN_HAND, hit(tablePosition, Direction.UP));
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL actual mallet interaction: recipe={}, item={}, result={}, beforeBlock=minecraft:crafting_table, position={}, handStack={}",
                    TetraWorkbenchSurvivalProgressionPlan.OAK_HAMMER_RECIPE_ID, resultId, interaction,
                    tablePosition, hammer);
        });
        phase = Phase.WAIT_UPGRADED_WORKBENCH;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForUpgradedWorkbench(Minecraft minecraft, ServerPlayer serverPlayer) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(tablePosition).getBlock());
        boolean isSurvival = serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
        if (!blockId.equals(ResourceLocation.parse("tetra:basic_workbench")) || !isSurvival) {
            if (timedOut("real mallet use did not upgrade the vanilla table while in Survival; block="
                    + blockId + ", mode=" + serverPlayer.gameMode.getGameModeForPlayer())) return;
            return;
        }
        LOGGER.info("TETRA_WORKBENCH_ACQUISITION_ASSERTION PASS pid={}, player={}, gameMode=SURVIVAL, harvestedLogs=2, rawMaterialsGranted=false, oakHammerRecipe={}, vanillaTablePlacedByItemUse=true, tetraWorkbenchCreatedByMalletUse=true, beforeBlock=minecraft:crafting_table, afterBlock={}, hammerStillInInventory={}",
                ProcessHandle.current().pid(), serverPlayer.getGameProfile().getName(),
                TetraWorkbenchSurvivalProgressionPlan.OAK_HAMMER_RECIPE_ID, blockId,
                serverPlayer.getInventory().countItem(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tetra:modular_double"))) > 0);
        runServerAction(serverPlayer, "open the acquired Tetra workbench GUI", () ->
                {
                    serverPlayer.getInventory().selected = firstEmptyHotbarSlot(serverPlayer);
                    serverPlayer.gameMode.useItemOn(serverPlayer, serverPlayer.serverLevel(), ItemStack.EMPTY,
                            InteractionHand.MAIN_HAND, hit(tablePosition, Direction.UP));
                });
        phase = Phase.WAIT_WORKBENCH_GUI;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForWorkbenchGui(Minecraft minecraft, ServerPlayer serverPlayer) {
        if (!(minecraft.screen instanceof WorkbenchScreen)) {
            if (timedOut("actual Tetra workbench did not open WorkbenchScreen; clientScreen="
                    + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName())
                    + ", serverMenu=" + serverPlayer.containerMenu.getClass().getName())) return;
            return;
        }
        phase = Phase.PASSED;
        LOGGER.info("TETRA_WORKBENCH_GUI_ASSERTION PASS pid={}, player={}, screen={}, serverMenu={}, workbench={}, mode=SURVIVAL",
                ProcessHandle.current().pid(), serverPlayer.getGameProfile().getName(),
                minecraft.screen.getClass().getName(), serverPlayer.containerMenu.getClass().getName(), tablePosition);
        minecraft.execute(minecraft::stop);
    }

    private static void pickupDroppedLogs(ServerPlayer player) {
        int tick = MAX_STAGE_TICKS - phaseTicks;
        if (tick - lastPickupAssistTick < 25) return;
        var level = player.serverLevel();
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(logPosition).inflate(6.0D)).stream()
                .filter(entity -> entity.isAlive() && entity.getItem().is(Items.OAK_LOG))
                .toList();
        if (tick % 20 == 0) {
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL pickup diagnostics: tick={}, gameTime={}, drops={}, serverPlayerPos={}, clientPlayerPos={}, mode={}",
                    tick, level.getGameTime(), drops.stream()
                            .map(entity -> entity.getItem() + "@" + entity.position()
                                    + ":age=" + entity.tickCount
                                    + ":distanceSqr=" + player.distanceToSqr(entity))
                            .toList(), player.position(), Minecraft.getInstance().player == null ? "missing"
                            : Minecraft.getInstance().player.position(), player.gameMode.getGameModeForPlayer());
        }
        ItemEntity nearest = drops.stream()
                .filter(entity -> entity.isAlive() && entity.getItem().is(Items.OAK_LOG)
                        && entity.tickCount >= 20)
                .findFirst().orElse(null);
        if (nearest == null) return;
        lastPickupAssistTick = tick;
        player.server.execute(() -> {
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL pickup callback: gameTime={}, playerAlive={}, dropAlive={}, drop={}, playerPos={}, dropPos={}",
                    player.serverLevel().getGameTime(), player.isAlive(), nearest.isAlive(), nearest.getItem(),
                    player.position(), nearest.position());
            if (!player.isAlive() || !nearest.isAlive()) return;
            player.connection.teleport(nearest.getX(), nearest.getY(), nearest.getZ(),
                    player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL pickup assist: drop={}, age={}, playerPos={}, distanceSqr={}",
                    nearest.getItem(), nearest.tickCount, player.position(), player.distanceToSqr(nearest));
        });
    }

    private static void runMenuAction(ServerPlayer player, String action, Runnable operation) {
        player.server.execute(() -> {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                fail("player left Survival before menu action: " + action);
                return;
            }
            operation.run();
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL menu action complete: action={}, menu={}, cursor={}, craftSlots={}, planks={}, sticks={}, tables={}, hammers={}",
                    action, player.containerMenu.getClass().getSimpleName(), player.containerMenu.getCarried(),
                    craftSlots(player),
                    player.getInventory().countItem(Items.OAK_PLANKS), player.getInventory().countItem(Items.STICK),
                    player.getInventory().countItem(Items.CRAFTING_TABLE),
                    player.getInventory().countItem(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tetra:modular_double"))));
        });
    }

    private static void runServerAction(ServerPlayer player, String action, Runnable operation) {
        player.server.execute(() -> {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                fail("player left Survival before world interaction: " + action);
                return;
            }
            operation.run();
            LOGGER.info("TETRA_WORKBENCH_SURVIVAL world action sent: action={}, position={}, mode={}",
                    action, tablePosition, player.gameMode.getGameModeForPlayer());
        });
    }

    private static void click(ServerPlayer player, int slot, int button, ClickType type) {
        player.containerMenu.clicked(slot, button, type, player);
    }

    private static String craftSlots(ServerPlayer player) {
        int slotCount = Math.min(5, player.containerMenu.slots.size());
        return java.util.stream.IntStream.range(0, slotCount)
                .mapToObj(index -> index + "=" + player.containerMenu.getSlot(index).getItem())
                .toList().toString();
    }

    private static void moveCarriedToEmptyInventory(ServerPlayer player) {
        if (player.containerMenu.getCarried().isEmpty()) return;
        int empty = findEmptyInventorySlot(player);
        if (empty < 0) throw new IllegalStateException("No player inventory slot for crafted sticks");
        click(player, SurvivalCraftingSlotLayout.playerInventoryMenuSlot(empty), 0, ClickType.PICKUP);
    }

    private static int findInventorySlot(Minecraft minecraft, net.minecraft.world.item.Item item, int count) {
        for (int index = 0; index < 36; index++) {
            ItemStack stack = minecraft.player.getInventory().getItem(index);
            if (stack.is(item) && stack.getCount() >= count) return index;
        }
        return -1;
    }

    private static int findInventorySlot(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        for (int index = 0; index < 36; index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (stack.is(item) && stack.getCount() >= count) return index;
        }
        return -1;
    }

    private static int findEmptyInventorySlot(ServerPlayer player) {
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            if (player.getInventory().getItem(index).isEmpty()) return index;
        }
        return -1;
    }

    private static int ensureHotbarSlot(ServerPlayer player, int inventoryIndex, int preferredHotbar) {
        if (inventoryIndex < 9) return inventoryIndex;
        int menuSlot = player.containerMenu instanceof CraftingMenu craftingMenu && craftingMenu.getGridWidth() == 3
                ? SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(inventoryIndex)
                : SurvivalCraftingSlotLayout.playerInventoryMenuSlot(inventoryIndex);
        click(player, menuSlot, preferredHotbar,
                ClickType.SWAP);
        return preferredHotbar;
    }

    private static int firstEmptyHotbarSlot(ServerPlayer player) {
        for (int index = 0; index < 9; index++) {
            if (player.getInventory().getItem(index).isEmpty()) return index;
        }
        return 8;
    }

    private static BlockHitResult hit(BlockPos position, Direction face) {
        Vec3 location = Vec3.atCenterOf(position);
        if (face == Direction.UP) location = location.add(0.0D, 0.5D, 0.0D);
        return new BlockHitResult(location, face, position, false);
    }

    private static boolean timedOut(String reason) {
        if (phaseTicks-- > 0) return false;
        fail(reason);
        return true;
    }

    private static void fail(String reason) {
        Phase failedPhase = phase;
        phase = Phase.FAILED;
        LOGGER.error("TETRA_WORKBENCH_ACQUISITION_ASSERTION FAIL pid={}, phase={}, reason={}",
                ProcessHandle.current().pid(), failedPhase, reason);
        Minecraft.getInstance().execute(Minecraft.getInstance()::stop);
    }
}
