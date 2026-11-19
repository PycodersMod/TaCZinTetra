package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/** Opt-in real-client survival probe. It never grants recipe ingredients or output items. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevSurvivalStarterPistolAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean(
            "taczintetra.dev_survival_starter_pistol_recipe_craft");
    private static final int MAX_STAGE_TICKS = 400;

    private static Phase phase = Phase.IDLE;
    private static BlockPos tablePos;
    private static Vec3 tableApproachPosition;
    private static float tableApproachYaw;
    private static float tableApproachPitch;
    private static List<BlockPos> logPositions = List.of();
    private static int phaseTicks;
    private static int lastPickupAssistTick = -MAX_STAGE_TICKS;
    private static int tableInteractionAttempts;
    private static boolean deadPlayerRecoveryRequested;

    private enum Phase {
        IDLE,
        WAIT_LOGS,
        WAIT_PLANK_RESULT,
        WAIT_PLANKS,
        WAIT_STICK_RESULT,
        WAIT_STICKS,
        WAIT_TABLE,
        WAIT_SLAB_RESULT,
        WAIT_SLABS,
        WAIT_PISTOL_RESULT,
        WAIT_COMPLETE,
        PASSED,
        FAILED
    }

    private DevSurvivalStarterPistolAutomation() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        phase = Phase.IDLE;
        phaseTicks = 0;
        lastPickupAssistTick = -MAX_STAGE_TICKS;
        deadPlayerRecoveryRequested = false;
        LOGGER.info("SURVIVAL_STARTER_PISTOL probe armed; all ingredients must be gathered and crafted in survival");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END
                || phase == Phase.PASSED || phase == Phase.FAILED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) return;
        var server = minecraft.getSingleplayerServer();
        var serverPlayer = server == null ? null : server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (serverPlayer == null) return;

        if (deadPlayerRecoveryRequested) {
            if (SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(minecraft.player.isAlive(),
                    serverPlayer.isAlive(), true)) {
                deadPlayerRecoveryRequested = false;
                phase = Phase.IDLE;
                phaseTicks = 0;
                LOGGER.info("SURVIVAL_STARTER_PISTOL stale-player recovery complete; restarting with a fresh survival fixture");
            } else if (++phaseTicks > MAX_STAGE_TICKS) {
                fail("integrated-server respawn did not restore client/server player identity in the bounded recovery window");
            }
            return;
        }
        if (!minecraft.player.isAlive() || !serverPlayer.isAlive()) {
            if (SurvivalProbeRecoveryPolicy.shouldRecover(serverPlayer.isAlive(), deadPlayerRecoveryRequested)) {
                recoverDeadProbePlayer(minecraft, serverPlayer);
                return;
            }
            fail("controlled world player is not alive after recovery; refusing to grant or craft items through a dead session");
            return;
        }
        if (phase == Phase.IDLE) {
            prepareFixture(minecraft);
            return;
        }

        switch (phase) {
            case WAIT_LOGS -> waitForLogs(minecraft, serverPlayer);
            case WAIT_PLANK_RESULT -> waitForPlankResult(minecraft);
            case WAIT_PLANKS -> waitForPlanks(minecraft);
            case WAIT_STICK_RESULT -> waitForStickResult(minecraft);
            case WAIT_STICKS -> waitForSticks(minecraft);
            case WAIT_TABLE -> waitForCraftingTable(minecraft);
            case WAIT_SLAB_RESULT -> waitForSlabResult(minecraft);
            case WAIT_SLABS -> waitForSlabs(minecraft);
            case WAIT_PISTOL_RESULT -> waitForPistolResult(minecraft);
            case WAIT_COMPLETE -> assertComplete(minecraft);
            default -> { }
        }
        if (phase != Phase.PASSED && phase != Phase.FAILED && ++phaseTicks > MAX_STAGE_TICKS) {
            fail("stage timeout: " + phase + ", inventory=" + inventorySummary(minecraft));
        }
    }

    private static void prepareFixture(Minecraft minecraft) {
        var server = minecraft.getSingleplayerServer();
        var dimension = minecraft.level.dimension();
        BlockPos playerPos = minecraft.player.blockPosition();
        Direction forward = minecraft.player.getDirection();
        Direction side = forward.getClockWise();
        BlockPos support = playerPos.below();
        tablePos = playerPos.relative(forward, 2).below();
        BlockPos middleLog = playerPos.relative(forward, 1);
        logPositions = List.of(middleLog.relative(side), middleLog, middleLog.relative(side.getOpposite()));
        Vec3 towardLogs = Vec3.atLowerCornerOf(forward.getNormal()).scale(0.15D);
        tableApproachPosition = new Vec3(support.getX() + 0.5D + towardLogs.x,
                support.getY() + 1.0D, support.getZ() + 0.5D + towardLogs.z);
        tableApproachYaw = minecraft.player.getYRot();
        tableApproachPitch = minecraft.player.getXRot();
        minecraft.setScreen(null);
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer active = server.getPlayerList().getPlayer(playerId);
            if (active == null || !active.isAlive()) {
                LOGGER.error("SURVIVAL_STARTER_PISTOL fixture cancelled: integrated-server player unavailable or dead");
                return;
            }
            var serverLevel = server.getLevel(dimension);
            serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(middleLog).inflate(6.0D))
                    .forEach(ItemEntity::discard);
            serverLevel.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(tablePos.below(), Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(tablePos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            for (BlockPos pos : logPositions) {
                serverLevel.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                serverLevel.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
            }
            active.getInventory().clearContent();
            active.setGameMode(GameType.SURVIVAL);
            active.setHealth(active.getMaxHealth());
            active.fallDistance = 0.0F;
            active.connection.teleport(tableApproachPosition.x, tableApproachPosition.y, tableApproachPosition.z,
                    tableApproachYaw, tableApproachPitch);
            LOGGER.info("SURVIVAL_STARTER_PISTOL fixture prepared on integrated-server thread: player={}, logs={}, table={}, survival=true, inventoryCleared=true, rawMaterialsGranted=false",
                    active.getGameProfile().getName(), logPositions, tablePos);

            boolean allAccepted = true;
            for (BlockPos pos : logPositions) {
                boolean accepted = active.gameMode.destroyBlock(pos);
                allAccepted &= accepted;
                LOGGER.info("SURVIVAL_STARTER_PISTOL real harvest: block=minecraft:oak_log, pos={}, accepted={}",
                        pos, accepted);
            }
            LOGGER.info("SURVIVAL_STARTER_PISTOL harvest batch finished: allAccepted={}, gameMode={}, rawMaterialsGranted=false",
                    allAccepted, active.gameMode.getGameModeForPlayer());
        });
        phase = Phase.WAIT_LOGS;
        phaseTicks = 0;
    }

    private static void recoverDeadProbePlayer(Minecraft minecraft, ServerPlayer serverPlayer) {
        var server = minecraft.getSingleplayerServer();
        var serverLevel = serverPlayer.serverLevel();
        BlockPos safeSpawn = minecraft.player.blockPosition();
        float yaw = minecraft.player.getYRot();
        var connection = serverPlayer.connection;
        deadPlayerRecoveryRequested = true;
        phaseTicks = 0;
        LOGGER.warn("SURVIVAL_STARTER_PISTOL stale-player recovery requested: player={}, deathTime={}, safeSpawn={}, dimension={}",
                minecraft.player.getGameProfile().getName(), minecraft.player.deathTime, safeSpawn,
                serverLevel.dimension().location());
        server.execute(() -> {
            serverLevel.setBlock(safeSpawn.below(), Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(safeSpawn, Blocks.AIR.defaultBlockState(), 3);
            serverLevel.setBlock(safeSpawn.above(), Blocks.AIR.defaultBlockState(), 3);
            serverPlayer.setRespawnPosition(serverLevel.dimension(), safeSpawn, yaw, true, false);
            ServerPlayer respawned = server.getPlayerList().respawn(serverPlayer, false);
            connection.player = respawned;
            LOGGER.warn("SURVIVAL_STARTER_PISTOL stale-player recovery respawned: player={}, alive={}, returnedPlayerPresent={}, connectionPlayerMatchesServerPlayer={}",
                    respawned.getGameProfile().getName(), respawned.isAlive(),
                    server.getPlayerList().getPlayer(respawned.getUUID()) == respawned,
                    connection.player == respawned);
        });
    }

    private static void waitForLogs(Minecraft minecraft, ServerPlayer serverPlayer) {
        int logs = minecraft.player.getInventory().countItem(Items.OAK_LOG);
        if (logs < 3) {
            assistRealDropPickup(minecraft, serverPlayer);
            return;
        }
        int logIndex = findInventorySlot(minecraft, Items.OAK_LOG, 3);
        if (logIndex < 0) {
            fail("three real oak-log drops are not present in one inventory stack");
            return;
        }
        minecraft.setScreen(new InventoryScreen(minecraft.player));
        clickSequence(minecraft, new int[]{SurvivalCraftingSlotLayout.playerInventoryMenuSlot(logIndex),
                        SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0)},
                new int[]{0, 0}, new ClickType[]{ClickType.PICKUP, ClickType.PICKUP},
                "place three harvested oak logs in vanilla 2x2 crafting grid");
        phase = Phase.WAIT_PLANK_RESULT;
        phaseTicks = 0;
    }

    private static void assistRealDropPickup(Minecraft minecraft, ServerPlayer serverPlayer) {
        int tick = phaseTicks;
        if (tick - lastPickupAssistTick < 25) return;
        var server = minecraft.getSingleplayerServer();
        var level = server.getLevel(minecraft.level.dimension());
        ItemEntity drop = level.getEntitiesOfClass(ItemEntity.class, new AABB(logPositions.get(1)).inflate(6.0D))
                .stream().filter(entity -> entity.isAlive() && entity.getItem().is(Items.OAK_LOG)
                        && entity.tickCount >= 20).findFirst().orElse(null);
        if (drop == null) return;
        lastPickupAssistTick = tick;
        Vec3 position = drop.position();
        server.execute(() -> {
            var active = server.getPlayerList().getPlayer(serverPlayer.getUUID());
            if (active == null || !drop.isAlive()) return;
            BlockPos floor = BlockPos.containing(position).below();
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
            active.connection.teleport(position.x, position.y, position.z, active.getYRot(), active.getXRot());
            LOGGER.info("SURVIVAL_STARTER_PISTOL positioned the surviving player next to actual log drop: stack={}, age={}, position={}",
                    drop.getItem(), drop.tickCount, position);
        });
    }

    private static void waitForPlankResult(Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof InventoryMenu menu)) {
            fail("vanilla player inventory crafting menu closed before plank output");
            return;
        }
        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(Items.OAK_PLANKS) || result.getCount() != 4) return;
        clickMenu(minecraft, 0, 0, ClickType.QUICK_MOVE, "craft all four-plank results from three harvested logs");
        phase = Phase.WAIT_PLANKS;
        phaseTicks = 0;
    }

    private static void waitForPlanks(Minecraft minecraft) {
        if (minecraft.player.getInventory().countItem(Items.OAK_PLANKS) != 12
                || minecraft.player.getInventory().countItem(Items.OAK_LOG) != 0) return;
        int plankIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 12);
        int source = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(plankIndex);
        clickSequence(minecraft, new int[]{source, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0),
                        SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 0), source},
                new int[]{0, 1, 1, 0}, new ClickType[]{ClickType.PICKUP, ClickType.PICKUP,
                        ClickType.PICKUP, ClickType.PICKUP},
                "place two of twelve crafted planks vertically for sticks");
        phase = Phase.WAIT_STICK_RESULT;
        phaseTicks = 0;
    }

    private static void waitForStickResult(Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof InventoryMenu menu)) return;
        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(Items.STICK) || result.getCount() != 4) return;
        clickMenu(minecraft, 0, 0, ClickType.QUICK_MOVE, "craft four sticks through vanilla player menu");
        phase = Phase.WAIT_STICKS;
        phaseTicks = 0;
    }

    private static void waitForSticks(Minecraft minecraft) {
        if (minecraft.player.getInventory().countItem(Items.OAK_PLANKS) != 10
                || minecraft.player.getInventory().countItem(Items.STICK) != 4) return;
        minecraft.setScreen(null);
        restoreTableApproachPosition(minecraft);
        tableInteractionAttempts = 0;
        phase = Phase.WAIT_TABLE;
        phaseTicks = 0;
    }

    private static void waitForCraftingTable(Minecraft minecraft) {
        if (!(minecraft.screen instanceof CraftingScreen)
                || !(minecraft.player.containerMenu instanceof CraftingMenu)) {
            if (minecraft.player.position().distanceToSqr(tableApproachPosition) > 0.25D) return;
            if (tableInteractionAttempts >= 3) {
                if (phaseTicks > 60) fail("crafting table interaction returned without opening CraftingScreen after three attempts; screen="
                        + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName())
                        + ", menu=" + minecraft.player.containerMenu.getClass().getName()
                        + ", table=" + minecraft.level.getBlockState(tablePos));
                return;
            }
            if (tableInteractionAttempts > 0 && phaseTicks % 20 != 0) return;
            minecraft.setScreen(null);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(tablePos).add(0.0D, 0.5D, 0.0D),
                    Direction.UP, tablePos, false);
            var result = minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
            tableInteractionAttempts++;
            LOGGER.info("SURVIVAL_STARTER_PISTOL crafting-table interaction: attempt={}/3, result={}, clientTable={}, playerPos={}, approachPos={}, screenAfter={}, menuAfter={}",
                    tableInteractionAttempts, result, BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(tablePos).getBlock()),
                    minecraft.player.position(), tableApproachPosition,
                    minecraft.screen == null ? "none" : minecraft.screen.getClass().getName(),
                    minecraft.player.containerMenu.getClass().getName());
            return;
        }
        CraftingMenu menu = (CraftingMenu) minecraft.player.containerMenu;
        int plankIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 10);
        if (plankIndex < 0) {
            fail("ten oak planks from survival crafting were not found before slab recipe");
            return;
        }
        var placement = CraftingMenuRecipePlacement.plan(new String[]{"PPP"}, Map.of('P',
                SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(plankIndex)));
        dispatchPlacement(minecraft, placement, "craft six wooden slabs through vanilla 3x3 table menu");
        phase = Phase.WAIT_SLAB_RESULT;
        phaseTicks = 0;
    }

    private static void waitForSlabResult(Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof CraftingMenu menu)) return;
        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(Items.OAK_SLAB) || result.getCount() != 6) return;
        clickMenu(minecraft, 0, 0, ClickType.QUICK_MOVE, "craft six oak slabs through vanilla crafting table");
        phase = Phase.WAIT_SLABS;
        phaseTicks = 0;
    }

    private static void restoreTableApproachPosition(Minecraft minecraft) {
        var server = minecraft.getSingleplayerServer();
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                LOGGER.error("SURVIVAL_STARTER_PISTOL cannot restore the normal table approach: player missing");
                return;
            }
            player.connection.teleport(tableApproachPosition.x, tableApproachPosition.y, tableApproachPosition.z,
                    tableApproachYaw, tableApproachPitch);
            LOGGER.info("SURVIVAL_STARTER_PISTOL restored a safe table approach after drop pickup: position={}, table={}",
                    tableApproachPosition, tablePos);
        });
    }

    private static void waitForSlabs(Minecraft minecraft) {
        if (minecraft.player.getInventory().countItem(Items.OAK_PLANKS) != 7
                || minecraft.player.getInventory().countItem(Items.STICK) != 4
                || minecraft.player.getInventory().countItem(Items.OAK_SLAB) != 6) return;
        int planks = findInventorySlot(minecraft, Items.OAK_PLANKS, 7);
        int sticks = findInventorySlot(minecraft, Items.STICK, 4);
        int slabs = findInventorySlot(minecraft, Items.OAK_SLAB, 6);
        var placement = StarterPistolSurvivalRecipePlan.menuClicks(
                SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(planks),
                SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(sticks),
                SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(slabs));
        dispatchPlacement(minecraft, placement, "place exact starter pistol pattern PPP/PS/L");
        phase = Phase.WAIT_PISTOL_RESULT;
        phaseTicks = 0;
    }

    private static void waitForPistolResult(Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof CraftingMenu menu)) return;
        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(ModItems.STARTER_PISTOL.get()) || result.getCount() != 1) return;
        if (minecraft.player.getAbilities().instabuild) {
            fail("player left survival mode before taking the starter pistol");
            return;
        }
        clickMenu(minecraft, 0, 0, ClickType.QUICK_MOVE, "craft and collect the real starter pistol recipe result");
        phase = Phase.WAIT_COMPLETE;
        phaseTicks = 0;
    }

    private static void assertComplete(Minecraft minecraft) {
        var inventory = minecraft.player.getInventory();
        if (inventory.countItem(Items.OAK_LOG) == 0
                && inventory.countItem(Items.OAK_PLANKS) == 3
                && inventory.countItem(Items.STICK) == 3
                && inventory.countItem(Items.OAK_SLAB) == 5
                && inventory.countItem(ModItems.STARTER_PISTOL.get()) == 1
                && !minecraft.player.getAbilities().instabuild) {
            phase = Phase.PASSED;
            LOGGER.info("SURVIVAL_STARTER_PISTOL_ASSERTION PASS pid={}, player={}, gameMode=SURVIVAL, harvestedLogs=3, oakLogs=0, oakPlanks=3, sticks=3, oakSlabs=5, starterPistol=1, recipe=PPP/PS/L, harvest=IntegratedServer.gameMode.destroyBlock, crafting=vanilla_inventory_and_crafting_table_menus",
                    ProcessHandle.current().pid(), minecraft.player.getGameProfile().getName());
            minecraft.execute(minecraft::stop);
        }
    }

    private static void dispatchPlacement(Minecraft minecraft,
                                          List<CraftingMenuRecipePlacement.Click> placement, String action) {
        int[] slots = placement.stream().mapToInt(CraftingMenuRecipePlacement.Click::slotId).toArray();
        int[] buttons = placement.stream().mapToInt(CraftingMenuRecipePlacement.Click::button).toArray();
        ClickType[] types = new ClickType[slots.length];
        java.util.Arrays.fill(types, ClickType.PICKUP);
        clickSequence(minecraft, slots, buttons, types, action + "; clickPlan=" + placement);
    }

    private static void clickMenu(Minecraft minecraft, int slot, int button, ClickType type, String action) {
        clickSequence(minecraft, new int[]{slot}, new int[]{button}, new ClickType[]{type}, action);
    }

    private static void clickSequence(Minecraft minecraft, int[] slots, int[] buttons,
                                      ClickType[] types, String action) {
        if (slots.length != buttons.length || slots.length != types.length) {
            throw new IllegalArgumentException("Each authoritative menu click needs a slot, button and click type");
        }
        var server = minecraft.getSingleplayerServer();
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                LOGGER.error("SURVIVAL_STARTER_PISTOL menu action failed: player missing, action={}", action);
                return;
            }
            var menu = player.containerMenu;
            for (int index = 0; index < slots.length; index++) {
                menu.clicked(slots[index], buttons[index], types[index], player);
                menu.broadcastChanges();
            }
            LOGGER.info("SURVIVAL_STARTER_PISTOL authoritative menu sequence complete: action={}, menu={}, cursor={}, inventory={}",
                    action, menu.getClass().getSimpleName(), menu.getCarried(), serverInventorySummary(player));
        });
    }

    private static int findInventorySlot(Minecraft minecraft, Item item, int minimumCount) {
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            ItemStack stack = minecraft.player.getInventory().getItem(index);
            if (stack.is(item) && stack.getCount() >= minimumCount) return index;
        }
        return -1;
    }

    private static String inventorySummary(Minecraft minecraft) {
        if (minecraft.player == null) return "player-missing";
        var inventory = minecraft.player.getInventory();
        return "logs=" + inventory.countItem(Items.OAK_LOG)
                + ",planks=" + inventory.countItem(Items.OAK_PLANKS)
                + ",sticks=" + inventory.countItem(Items.STICK)
                + ",slabs=" + inventory.countItem(Items.OAK_SLAB)
                + ",pistol=" + inventory.countItem(ModItems.STARTER_PISTOL.get());
    }

    private static String serverInventorySummary(ServerPlayer player) {
        return "logs=" + player.getInventory().countItem(Items.OAK_LOG)
                + ",planks=" + player.getInventory().countItem(Items.OAK_PLANKS)
                + ",sticks=" + player.getInventory().countItem(Items.STICK)
                + ",slabs=" + player.getInventory().countItem(Items.OAK_SLAB)
                + ",pistol=" + player.getInventory().countItem(ModItems.STARTER_PISTOL.get());
    }

    private static void fail(String reason) {
        if (phase == Phase.FAILED || phase == Phase.PASSED) return;
        phase = Phase.FAILED;
        LOGGER.error("SURVIVAL_STARTER_PISTOL_ASSERTION FAIL reason={}", reason);
    }
}
