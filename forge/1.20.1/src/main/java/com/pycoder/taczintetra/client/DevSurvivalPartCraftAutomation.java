package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/** Isolated real-client survival recipe probe; enabled only with an explicit development JVM property. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevSurvivalPartCraftAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_survival_recipe_craft");
    private static final int MAX_STAGE_TICKS = 200;
    private static Phase phase = Phase.IDLE;
    private static BlockPos craftingTablePos;
    private static BlockPos oakLogPos;
    private static int phaseTicks;
    private static int setupStateLogTicks;
    private static boolean spawnSupportRequested;
    private static boolean deadPlayerRecoveryRequested;
    private static boolean pickupAssistRequested;
    private static int plankInventoryIndex = -1;
    private static int logInventoryIndex = -1;
    private static int stickInventoryIndex = -1;
    private static int emptyInventoryIndex = -1;

    private enum Phase {
        IDLE,
        WAIT_SURVIVAL_INPUTS,
        WAIT_LOG_DROP,
        WAIT_LOG_CURSOR,
        WAIT_LOG_IN_CRAFT_GRID,
        WAIT_PLANK_RESULT,
        WAIT_PLANK_OUTPUT_CURSOR,
        WAIT_PLANK_SETTLED,
        WAIT_STICK_RESULT,
        WAIT_STICK_OUTPUT_CURSOR,
        WAIT_STICK_SETTLED,
        WAIT_CRAFTING_SCREEN,
        WAIT_RECIPE_RESULT,
        WAIT_RECIPE_CURSOR_SETTLED,
        WAIT_OUTPUT_CURSOR,
        WAIT_SETTLED_INVENTORY,
        PASSED,
        FAILED
    }

    private DevSurvivalPartCraftAutomation() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        phase = Phase.IDLE;
        phaseTicks = 0;
        setupStateLogTicks = 0;
        spawnSupportRequested = false;
        deadPlayerRecoveryRequested = false;
        pickupAssistRequested = false;
        plankInventoryIndex = -1;
        craftingTablePos = null;
        oakLogPos = null;
        logInventoryIndex = -1;
        stickInventoryIndex = -1;
        emptyInventoryIndex = -1;
        LOGGER.info("SURVIVAL_PART_CRAFT probe armed for a real local client session");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == Phase.PASSED || phase == Phase.FAILED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.level == null) return;
        var server = minecraft.getSingleplayerServer();
        var serverPlayer = server == null ? null : server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (deadPlayerRecoveryRequested) {
            if (SurvivalProbeRecoveryPolicy.shouldResetAfterRecovery(minecraft.player.isAlive(),
                    serverPlayer != null && serverPlayer.isAlive(), true)) {
                deadPlayerRecoveryRequested = false;
                phase = Phase.IDLE;
                phaseTicks = 0;
                setupStateLogTicks = 0;
                spawnSupportRequested = false;
                pickupAssistRequested = false;
                plankInventoryIndex = -1;
                logInventoryIndex = -1;
                stickInventoryIndex = -1;
                emptyInventoryIndex = -1;
                LOGGER.info("SURVIVAL_PART_CRAFT recovery completed; restarting probe from a clean survival fixture");
            } else if (++phaseTicks > MAX_STAGE_TICKS) {
                fail("integrated-server respawn did not restore both client and server players within the bounded recovery window");
            }
            return;
        }
        if (!minecraft.player.isAlive()) {
            if (minecraft.screen instanceof DeathScreen) {
                var player = minecraft.player;
                if (server != null && serverPlayer != null
                        && SurvivalProbeRecoveryPolicy.shouldRecover(serverPlayer.isAlive(), false)) {
                    recoverDeadProbePlayer(minecraft, serverPlayer);
                    return;
                }
                fail("player is on the death screen; refusing to run the survival recipe probe"
                        + ", clientHealth=" + player.getHealth()
                        + ", clientAlive=" + player.isAlive()
                        + ", clientPos=" + player.position()
                        + ", clientVelocity=" + player.getDeltaMovement()
                        + ", clientBelow=" + BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(player.blockPosition().below()).getBlock())
                        + ", serverHealth=" + (serverPlayer == null ? "missing" : serverPlayer.getHealth())
                        + ", serverAlive=" + (serverPlayer == null ? "missing" : serverPlayer.isAlive())
                        + ", serverPos=" + (serverPlayer == null ? "missing" : serverPlayer.position()));
            } else {
                ensureSpawnSupport(minecraft);
                if (++phaseTicks > MAX_STAGE_TICKS) {
                    fail("client player did not become alive after joining; screen="
                            + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName()));
                }
            }
            return;
        }
        if (phase == Phase.IDLE) phaseTicks = 0;
        if (minecraft.getSingleplayerServer() == null) {
            fail("not an integrated single-player test world");
            return;
        }

        switch (phase) {
            case IDLE -> prepareSurvivalWorld(minecraft);
            case WAIT_SURVIVAL_INPUTS -> waitForSurvivalInputs(minecraft);
            case WAIT_LOG_DROP -> waitForOakLogDrop(minecraft);
            case WAIT_LOG_CURSOR -> waitForLogCursor(minecraft);
            case WAIT_LOG_IN_CRAFT_GRID -> waitForLogInCraftGrid(minecraft);
            case WAIT_PLANK_RESULT -> waitForPlankResult(minecraft);
            case WAIT_PLANK_OUTPUT_CURSOR -> waitForPlankOutputCursor(minecraft);
            case WAIT_PLANK_SETTLED -> waitForPlanksSettled(minecraft);
            case WAIT_STICK_RESULT -> waitForStickResult(minecraft);
            case WAIT_STICK_OUTPUT_CURSOR -> waitForStickOutputCursor(minecraft);
            case WAIT_STICK_SETTLED -> waitForSticksSettled(minecraft);
            case WAIT_CRAFTING_SCREEN -> waitForCraftingScreen(minecraft);
            case WAIT_RECIPE_RESULT -> waitForRecipeResult(minecraft);
            case WAIT_RECIPE_CURSOR_SETTLED -> waitForRecipeCursorSettled(minecraft);
            case WAIT_OUTPUT_CURSOR -> waitForOutputCursor(minecraft);
            case WAIT_SETTLED_INVENTORY -> waitForSettledInventory(minecraft);
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onProbePlayerDeath(LivingDeathEvent event) {
        if (!ENABLED || !(event.getEntity() instanceof Player player)) return;
        LOGGER.error("SURVIVAL_PART_CRAFT player death observed: player={}, source={}, pos={}, health={}, clientSide={}",
                player.getGameProfile().getName(), event.getSource().getMsgId(), player.blockPosition(),
                player.getHealth(), player.level().isClientSide);
    }

    private static void prepareSurvivalWorld(Minecraft minecraft) {
        var player = minecraft.player;
        var server = minecraft.getSingleplayerServer();
        var serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
        if (serverPlayer == null) {
            fail("integrated-server player is missing before survival fixture preparation");
            return;
        }
        if (SurvivalProbeRecoveryPolicy.shouldRecover(serverPlayer.isAlive(), deadPlayerRecoveryRequested)) {
            recoverDeadProbePlayer(minecraft, serverPlayer);
            return;
        }
        BlockPos supportPos = player.blockPosition().below();
        craftingTablePos = player.blockPosition().relative(player.getDirection(), 2).below();
        oakLogPos = player.blockPosition().relative(player.getDirection(), 1);
        minecraft.setScreen(null);
        var serverLevel = server.getLevel(minecraft.level.dimension());
        server.execute(() -> {
            var currentServerPlayer = server.getPlayerList().getPlayer(player.getUUID());
            if (currentServerPlayer == null || !currentServerPlayer.isAlive()) {
                LOGGER.error("SURVIVAL_PART_CRAFT fixture setup cancelled because integrated-server player is missing or dead: player={}, health={}, alive={}",
                        currentServerPlayer == null ? "missing" : currentServerPlayer.getGameProfile().getName(),
                        currentServerPlayer == null ? "missing" : currentServerPlayer.getHealth(),
                        currentServerPlayer != null && currentServerPlayer.isAlive());
                return;
            }
            serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(oakLogPos).inflate(3.0D))
                    .forEach(ItemEntity::discard);
            serverLevel.setBlock(supportPos, Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(craftingTablePos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            serverLevel.setBlock(oakLogPos, Blocks.OAK_LOG.defaultBlockState(), 3);
            currentServerPlayer.getInventory().clearContent();
            currentServerPlayer.setGameMode(GameType.SURVIVAL);
            currentServerPlayer.setHealth(currentServerPlayer.getMaxHealth());
            currentServerPlayer.fallDistance = 0.0F;
            Vec3 pickupReachOffset = Vec3.atLowerCornerOf(player.getDirection().getNormal()).scale(0.2D);
            currentServerPlayer.teleportTo(supportPos.getX() + 0.5D + pickupReachOffset.x,
                    supportPos.getY() + 1.0D, supportPos.getZ() + 0.5D + pickupReachOffset.z);
            LOGGER.info("SURVIVAL_PART_CRAFT fixture prepared through integrated-server API: player={}, health={}/{}, supportPos={}, tablePos={}, oakLogPos={}, rawMaterialsGranted=false",
                    currentServerPlayer.getGameProfile().getName(), currentServerPlayer.getHealth(),
                    currentServerPlayer.getMaxHealth(), supportPos, craftingTablePos, oakLogPos);
            LOGGER.info("SURVIVAL_PART_CRAFT positioned survival player within natural item pickup reach: playerPos={}, oakLogPos={}, offset={}",
                    currentServerPlayer.position(), oakLogPos, pickupReachOffset);
        });
        phase = Phase.WAIT_SURVIVAL_INPUTS;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT setup requested: player={}, supportPos={}, tablePos={}, oakLogBlockPos={}, rawMaterialsGranted=false, recipe=taczintetra:parts/wood_barrel",
                player.getGameProfile().getName(), supportPos, craftingTablePos, oakLogPos);
    }

    private static void ensureSpawnSupport(Minecraft minecraft) {
        if (spawnSupportRequested) return;
        BlockPos supportPos = minecraft.player.blockPosition().below();
        var server = minecraft.getSingleplayerServer();
        var serverLevel = server.getLevel(minecraft.level.dimension());
        server.execute(() -> serverLevel.setBlock(supportPos, Blocks.STONE.defaultBlockState(), 3));
        spawnSupportRequested = true;
        LOGGER.info("SURVIVAL_PART_CRAFT requested temporary spawn support before player became alive: pos={}", supportPos);
    }

    private static void recoverDeadProbePlayer(Minecraft minecraft, ServerPlayer serverPlayer) {
        var server = minecraft.getSingleplayerServer();
        var serverLevel = serverPlayer.serverLevel();
        var player = minecraft.player;
        BlockPos safeSpawn = player.blockPosition();
        deadPlayerRecoveryRequested = true;
        phaseTicks = 0;
        LOGGER.warn("SURVIVAL_PART_CRAFT stale-player recovery requested: player={}, health={}, deathTime={}, pos={}, dimension={}, safeSpawn={}",
                player.getGameProfile().getName(), player.getHealth(), player.deathTime, player.blockPosition(),
                serverLevel.dimension().location(), safeSpawn);
        server.execute(() -> {
            BlockPos floor = safeSpawn.below();
            serverLevel.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
            serverLevel.setBlock(safeSpawn, Blocks.AIR.defaultBlockState(), 3);
            serverLevel.setBlock(safeSpawn.above(), Blocks.AIR.defaultBlockState(), 3);
            serverPlayer.setRespawnPosition(serverLevel.dimension(), safeSpawn, player.getYRot(), true, false);
            var connection = serverPlayer.connection;
            ServerPlayer respawned = server.getPlayerList().respawn(serverPlayer, false);
            connection.player = respawned;
            LOGGER.warn("SURVIVAL_PART_CRAFT stale-player recovery respawned: player={}, health={}, alive={}, pos={}, dimension={}, returnedPlayerPresent={}, connectionPlayerMatchesServerPlayer={}",
                    respawned.getGameProfile().getName(), respawned.getHealth(), respawned.isAlive(), respawned.position(),
                    respawned.serverLevel().dimension().location(),
                    server.getPlayerList().getPlayer(respawned.getUUID()) == respawned, connection.player == respawned);
        });
    }

    private static void waitForSurvivalInputs(Minecraft minecraft) {
        var player = minecraft.player;
        var serverLevel = minecraft.getSingleplayerServer().getLevel(minecraft.level.dimension());
        String setupState = "instabuild=" + player.getAbilities().instabuild
                + ", dimension=" + minecraft.level.dimension().location()
                + ", player=" + player.blockPosition()
                + ", table=" + BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(craftingTablePos).getBlock())
                + ", oakLog=" + BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(oakLogPos).getBlock())
                + ", serverTable=" + BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(craftingTablePos).getBlock())
                + ", serverOakLog=" + BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(oakLogPos).getBlock())
                + ", screen=" + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
        if (setupStateLogTicks++ % 20 == 0) {
            LOGGER.info("SURVIVAL_PART_CRAFT setup state: {}", setupState);
        }
        if (player.getAbilities().instabuild
                || !serverLevel.getBlockState(craftingTablePos).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)) {
            if (timedOut("survival mode or crafting table did not become ready: " + setupState)) return;
            return;
        }
        if (!serverLevel.getBlockState(oakLogPos).is(Blocks.OAK_LOG)) {
            if (timedOut("controlled oak log block did not become ready: " + setupState)) return;
            return;
        }
        var server = minecraft.getSingleplayerServer();
        var serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
        if (serverPlayer == null) {
            fail("integrated-server player disappeared before the survival harvest");
            return;
        }
        var serverTableState = serverLevel.getBlockState(craftingTablePos);
        var serverLogState = serverLevel.getBlockState(oakLogPos);
        minecraft.level.setBlock(craftingTablePos, serverTableState, 3);
        minecraft.level.setBlock(oakLogPos, serverLogState, 3);
        server.execute(() -> {
            var activeServerPlayer = server.getPlayerList().getPlayer(player.getUUID());
            boolean accepted = activeServerPlayer != null && activeServerPlayer.gameMode.destroyBlock(oakLogPos);
            LOGGER.info("SURVIVAL_PART_CRAFT requested integrated-server survival harvest: block=minecraft:oak_log, pos={}, accepted={}, serverGameMode={}",
                    oakLogPos, accepted, activeServerPlayer == null ? "missing" : activeServerPlayer.gameMode.getGameModeForPlayer());
        });
        phase = Phase.WAIT_LOG_DROP;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForOakLogDrop(Minecraft minecraft) {
        var player = minecraft.player;
        var server = minecraft.getSingleplayerServer();
        var serverLevel = server.getLevel(minecraft.level.dimension());
        var serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
        int serverOakLogs = serverPlayer == null ? 0 : serverPlayer.getInventory().countItem(Items.OAK_LOG);
        if (serverOakLogs < 1 && !pickupAssistRequested && serverPlayer != null) {
            ItemEntity droppedLog = serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(oakLogPos).inflate(5.0D)).stream()
                    .filter(entity -> entity.getItem().is(Items.OAK_LOG) && entity.tickCount >= 20)
                    .findFirst().orElse(null);
            if (droppedLog != null) {
                pickupAssistRequested = true;
                Vec3 dropPosition = droppedLog.position();
                server.execute(() -> {
                    var activeServerPlayer = server.getPlayerList().getPlayer(player.getUUID());
                    if (activeServerPlayer == null || !droppedLog.isAlive()) {
                        LOGGER.error("SURVIVAL_PART_CRAFT controlled pickup positioning failed: player or harvested drop disappeared");
                        return;
                    }
                    BlockPos pickupFloor = BlockPos.containing(dropPosition).below();
                    ServerPlayer connectionPlayer = activeServerPlayer.connection.player;
                    boolean connectionPlayerMatchesServerPlayer = connectionPlayer == activeServerPlayer;
                    serverLevel.setBlock(pickupFloor, Blocks.STONE.defaultBlockState(), 3);
                    activeServerPlayer.connection.teleport(dropPosition.x, dropPosition.y, dropPosition.z,
                            activeServerPlayer.getYRot(), activeServerPlayer.getXRot());
                    LOGGER.info("SURVIVAL_PART_CRAFT positioned the development player beside the real harvested drop after vanilla pickup delay: player={}, connectionPlayerMatchesServerPlayer={}, drop={}, age={}, floor={}, target={}, pickupPlayerPos={}, connectionPlayerPos={}, pickupDistanceSqr={}",
                            activeServerPlayer.getGameProfile().getName(), connectionPlayerMatchesServerPlayer,
                            droppedLog.getItem(), droppedLog.tickCount, pickupFloor, dropPosition, activeServerPlayer.position(),
                            connectionPlayer.position(),
                            activeServerPlayer.distanceToSqr(droppedLog));
                });
            }
        }
        if (player.getInventory().countItem(Items.OAK_LOG) < 1 || serverOakLogs < 1
                || !serverLevel.getBlockState(oakLogPos).isAir()) {
            String harvestState = "serverLog="
                    + BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(oakLogPos).getBlock())
                    + ", playerOakLogs=" + player.getInventory().countItem(Items.OAK_LOG)
                    + ", serverOakLogs=" + serverOakLogs
                    + ", nearbyDrops=" + nearbyDrops(serverLevel, serverPlayer);
            if (phaseTicks % 20 == 0) LOGGER.info("SURVIVAL_PART_CRAFT authoritative harvest/pickup state: {}", harvestState);
            if (timedOut("survival harvest did not produce and collect one oak log: " + harvestState)) return;
            return;
        }
        logInventoryIndex = findInventorySlot(minecraft, Items.OAK_LOG, 1);
        if (logInventoryIndex < 0) {
            fail("oak log drop was not found in player inventory");
            return;
        }
        minecraft.setScreen(new InventoryScreen(player));
        int logMenuSlot = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(logInventoryIndex);
        LOGGER.info("SURVIVAL_PART_CRAFT opened inventory screen for harvested ingredient transfer: inventoryIndex={}, menuSlot={}, screen={}",
                logInventoryIndex, logMenuSlot, minecraft.screen.getClass().getName());
        clickIntegratedServerMenu(minecraft, logMenuSlot, 0, ClickType.PICKUP, "pick up harvested oak log");
        phase = Phase.WAIT_LOG_CURSOR;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT waiting for authoritative server cursor to confirm harvested oak log pickup");
    }

    private static void waitForLogCursor(Minecraft minecraft) {
        var player = minecraft.player;
        InventoryMenu menu = player.inventoryMenu;
        if (!menu.getCarried().is(Items.OAK_LOG) || menu.getCarried().getCount() != 1) {
            String state = "cursor=" + menu.getCarried() + ", sourceIndex=" + logInventoryIndex
                    + ", sourceStack=" + player.getInventory().getItem(logInventoryIndex)
                    + ", menuId=" + menu.containerId;
            if (setupStateLogTicks++ % 20 == 0) LOGGER.info("SURVIVAL_PART_CRAFT log cursor state: {}", state);
            if (timedOut("inventory menu did not acknowledge picking up the harvested oak log: " + state)) return;
            return;
        }
        clickIntegratedServerMenu(minecraft,
                SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0), 0, ClickType.PICKUP,
                "place harvested oak log in player crafting grid");
        phase = Phase.WAIT_LOG_IN_CRAFT_GRID;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT waiting for authoritative server menu to confirm oak log in crafting slot 1");
    }

    private static void waitForLogInCraftGrid(Minecraft minecraft) {
        InventoryMenu menu = minecraft.player.inventoryMenu;
        ItemStack gridLog = menu.getSlot(SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0)).getItem();
        if (!gridLog.is(Items.OAK_LOG) || gridLog.getCount() != 1 || !menu.getCarried().isEmpty()) {
            String state = "gridSlot1=" + gridLog + ", cursor=" + menu.getCarried() + ", menuId=" + menu.containerId;
            if (setupStateLogTicks++ % 20 == 0) LOGGER.info("SURVIVAL_PART_CRAFT log crafting-grid state: {}", state);
            if (timedOut("inventory menu did not acknowledge placing the harvested oak log: " + state)) return;
            return;
        }
        phase = Phase.WAIT_PLANK_RESULT;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT server-confirmed harvested oak log in vanilla player crafting grid");
    }

    private static void clickIntegratedServerMenu(Minecraft minecraft, int slotId, int button,
                                                   ClickType clickType, String action) {
        var server = minecraft.getSingleplayerServer();
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            var serverPlayer = server.getPlayerList().getPlayer(playerId);
            if (serverPlayer == null) {
                LOGGER.error("SURVIVAL_PART_CRAFT server menu click failed: player missing, action={}", action);
                return;
            }
            var menu = serverPlayer.containerMenu;
            LOGGER.info("SURVIVAL_PART_CRAFT server menu click before: action={}, slot={}, button={}, type={}, menuId={}, stateId={}, cursor={}, slotStack={}",
                    action, slotId, button, clickType, menu.containerId, menu.getStateId(), menu.getCarried(),
                    menu.isValidSlotIndex(slotId) ? menu.getSlot(slotId).getItem() : "invalid-slot");
            menu.clicked(slotId, button, clickType, serverPlayer);
            menu.broadcastChanges();
            LOGGER.info("SURVIVAL_PART_CRAFT server menu click after: action={}, menuId={}, stateId={}, cursor={}, slotStack={}",
                    action, menu.containerId, menu.getStateId(), menu.getCarried(),
                    menu.isValidSlotIndex(slotId) ? menu.getSlot(slotId).getItem() : "invalid-slot");
        });
    }

    private static void clickIntegratedServerMenuSequence(Minecraft minecraft, int[] slotIds, int[] buttons,
                                                           String action) {
        if (slotIds.length != buttons.length) throw new IllegalArgumentException("Every menu slot needs a button value");
        var server = minecraft.getSingleplayerServer();
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            var serverPlayer = server.getPlayerList().getPlayer(playerId);
            if (serverPlayer == null) {
                LOGGER.error("SURVIVAL_PART_CRAFT server menu sequence failed: player missing, action={}", action);
                return;
            }
            var menu = serverPlayer.containerMenu;
            LOGGER.info("SURVIVAL_PART_CRAFT server menu sequence started: action={}, menuClass={}, menuId={}, stateId={}, cursor={}",
                    action, menu.getClass().getName(), menu.containerId, menu.getStateId(), menu.getCarried());
            for (int index = 0; index < slotIds.length; index++) {
                int slotId = slotIds[index];
                int button = buttons[index];
                menu.clicked(slotId, button, ClickType.PICKUP, serverPlayer);
                menu.broadcastChanges();
                LOGGER.info("SURVIVAL_PART_CRAFT server menu sequence step: action={}, step={}/{}, slot={}, button={}, stateId={}, cursor={}, slotStack={}",
                        action, index + 1, slotIds.length, slotId, button, menu.getStateId(), menu.getCarried(),
                        menu.isValidSlotIndex(slotId) ? menu.getSlot(slotId).getItem() : "invalid-slot");
            }
        });
    }

    private static void waitForPlankResult(Minecraft minecraft) {
        InventoryMenu menu = minecraft.player.inventoryMenu;
        ItemStack result = menu.getSlot(0).getItem();
        String plankGrid = "[" + menu.getSlot(1).getItem() + ", " + menu.getSlot(2).getItem()
                + ", " + menu.getSlot(3).getItem() + ", " + menu.getSlot(4).getItem() + "]";
        if (!result.is(Items.OAK_PLANKS) || result.getCount() != 4) {
            String state = "plankGrid=" + plankGrid + ", result=" + result + ", cursor="
                    + minecraft.player.containerMenu.getCarried() + ", inventoryOakLogs="
                    + minecraft.player.getInventory().countItem(Items.OAK_LOG);
            if (setupStateLogTicks++ % 20 == 0) {
                LOGGER.info("SURVIVAL_PART_CRAFT plank recipe state: {}", state);
                var server = minecraft.getSingleplayerServer();
                var playerId = minecraft.player.getUUID();
                server.execute(() -> {
                    var serverPlayer = server.getPlayerList().getPlayer(playerId);
                    if (serverPlayer == null) {
                        LOGGER.info("SURVIVAL_PART_CRAFT server menu snapshot: player=missing");
                        return;
                    }
                    var serverMenu = serverPlayer.containerMenu;
                    LOGGER.info("SURVIVAL_PART_CRAFT server menu snapshot: player={}, menuClass={}, menuId={}, serverGridSlot1={}, serverCursor={}, inventoryOakLogs={}",
                            serverPlayer.getGameProfile().getName(), serverMenu.getClass().getName(), serverMenu.containerId,
                            serverMenu.getSlot(1).getItem(), serverMenu.getCarried(),
                            serverPlayer.getInventory().countItem(Items.OAK_LOG));
                });
            }
            if (timedOut("vanilla player crafting menu did not produce four oak planks from the harvested log: " + state)) return;
            return;
        }
        clickIntegratedServerMenu(minecraft, 0, 0, ClickType.PICKUP, "take four oak planks from player crafting result");
        phase = Phase.WAIT_PLANK_OUTPUT_CURSOR;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForPlankOutputCursor(Minecraft minecraft) {
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (!carried.is(Items.OAK_PLANKS) || carried.getCount() != 4) {
            if (timedOut("taking the vanilla plank result did not produce four oak planks on the cursor")) return;
            return;
        }
        InventoryMenu menu = minecraft.player.inventoryMenu;
        clickIntegratedServerMenu(minecraft,
                SurvivalCraftingSlotLayout.playerInventoryMenuSlot(logInventoryIndex), 0, ClickType.PICKUP,
                "store four crafted oak planks in survival inventory");
        phase = Phase.WAIT_PLANK_SETTLED;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForPlanksSettled(Minecraft minecraft) {
        if (minecraft.player.getInventory().countItem(Items.OAK_PLANKS) < 4) {
            if (timedOut("crafted planks did not settle in the survival inventory")) return;
            return;
        }
        plankInventoryIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 4);
        InventoryMenu menu = minecraft.player.inventoryMenu;
        int sourceSlot = SurvivalCraftingSlotLayout.playerInventoryMenuSlot(plankInventoryIndex);
        clickIntegratedServerMenuSequence(minecraft,
                new int[]{sourceSlot, SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(0, 0),
                        SurvivalCraftingSlotLayout.inventoryCraftingMenuSlot(1, 0)},
                new int[]{1, 1, 1}, "place two oak planks vertically in vanilla player crafting grid");
        phase = Phase.WAIT_STICK_RESULT;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT placed two oak planks into vanilla player crafting grid for sticks");
    }

    private static void waitForStickResult(Minecraft minecraft) {
        InventoryMenu menu = minecraft.player.inventoryMenu;
        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(Items.STICK) || result.getCount() != 4) {
            String state = "grid=[" + menu.getSlot(1).getItem() + ", " + menu.getSlot(2).getItem()
                    + ", " + menu.getSlot(3).getItem() + ", " + menu.getSlot(4).getItem() + "], result=" + result
                    + ", cursor=" + minecraft.player.containerMenu.getCarried();
            if (setupStateLogTicks++ % 20 == 0) LOGGER.info("SURVIVAL_PART_CRAFT stick recipe state: {}", state);
            if (timedOut("vanilla player crafting menu did not produce four sticks from two oak planks: " + state)) return;
            return;
        }
        clickIntegratedServerMenu(minecraft, 0, 0, ClickType.PICKUP, "take four sticks from player crafting result");
        phase = Phase.WAIT_STICK_OUTPUT_CURSOR;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForStickOutputCursor(Minecraft minecraft) {
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (!carried.is(Items.STICK) || carried.getCount() != 4) {
            if (timedOut("taking the vanilla stick result did not produce four sticks on the cursor")) return;
            return;
        }
        emptyInventoryIndex = findEmptyInventorySlot(minecraft);
        if (emptyInventoryIndex < 0) {
            fail("no empty player inventory slot available for crafted sticks");
            return;
        }
        InventoryMenu menu = minecraft.player.inventoryMenu;
        clickIntegratedServerMenu(minecraft,
                SurvivalCraftingSlotLayout.playerInventoryMenuSlot(emptyInventoryIndex), 0, ClickType.PICKUP,
                "store crafted sticks in survival inventory");
        phase = Phase.WAIT_STICK_SETTLED;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForSticksSettled(Minecraft minecraft) {
        if (minecraft.player.getInventory().countItem(Items.STICK) < 4
                || minecraft.player.getInventory().countItem(Items.OAK_PLANKS) < 2) {
            if (timedOut("crafted sticks and remaining planks did not settle in the survival inventory")) return;
            return;
        }
        minecraft.setScreen(null);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(craftingTablePos), Direction.UP, craftingTablePos, false);
        var player = minecraft.player;
        var result = minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        LOGGER.info("SURVIVAL_PART_CRAFT opened vanilla crafting table: interactionResult={}, survival=true",
                result);
        phase = Phase.WAIT_CRAFTING_SCREEN;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForCraftingScreen(Minecraft minecraft) {
        if (!(minecraft.screen instanceof CraftingScreen)
                || !(minecraft.player.containerMenu instanceof CraftingMenu menu)) {
            if (timedOut("vanilla crafting screen did not open")) return;
            return;
        }
        var player = minecraft.player;
        plankInventoryIndex = findInventorySlot(minecraft, Items.OAK_PLANKS, 2);
        stickInventoryIndex = findInventorySlot(minecraft, Items.STICK, 1);
        if (plankInventoryIndex < 0 || stickInventoryIndex < 0) {
            fail("could not locate the exact survival ingredients in player inventory");
            return;
        }
        int containerId = menu.containerId;
        var placement = CraftingMenuRecipePlacement.plan(new String[]{"P", "S", "P"}, Map.of(
                'P', SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(plankInventoryIndex),
                'S', SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(stickInventoryIndex)));
        int[] slotIds = placement.stream().mapToInt(CraftingMenuRecipePlacement.Click::slotId).toArray();
        int[] buttons = placement.stream().mapToInt(CraftingMenuRecipePlacement.Click::button).toArray();
        clickIntegratedServerMenuSequence(minecraft, slotIds, buttons,
                "place planks and stick in vanilla shaped recipe; plan=" + placement);
        phase = Phase.WAIT_RECIPE_RESULT;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT sent vanilla menu clicks to place 2 oak planks and 1 stick in the shaped recipe");
    }

    private static void waitForRecipeResult(Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof CraftingMenu menu)) {
            fail("crafting menu closed before recipe result appeared");
            return;
        }
        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(configuredOutput())) {
            String state = "grid=[" + menu.getSlot(1).getItem() + ", " + menu.getSlot(2).getItem()
                    + ", " + menu.getSlot(3).getItem() + ", " + menu.getSlot(4).getItem()
                    + ", " + menu.getSlot(5).getItem() + ", " + menu.getSlot(6).getItem()
                    + ", " + menu.getSlot(7).getItem() + ", " + menu.getSlot(8).getItem()
                    + ", " + menu.getSlot(9).getItem() + "], result=" + result
                    + ", cursor=" + menu.getCarried() + ", oakPlanks=" + minecraft.player.getInventory().countItem(Items.OAK_PLANKS)
                    + ", sticks=" + minecraft.player.getInventory().countItem(Items.STICK);
            if (setupStateLogTicks++ % 20 == 0) LOGGER.info("SURVIVAL_PART_CRAFT configured recipe state: {}", state);
            if (timedOut("vanilla crafting result slot did not show taczintetra:wood_barrel: " + state)) return;
            return;
        }
        if (minecraft.player.getAbilities().instabuild) {
            fail("player left survival mode before taking the recipe result");
            return;
        }
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            if (!carried.is(Items.STICK) || carried.getCount() != 3 || stickInventoryIndex < 0) {
                fail("unexpected cursor contents before taking configured recipe output: " + carried);
                return;
            }
            clickIntegratedServerMenu(minecraft,
                    SurvivalCraftingSlotLayout.craftingTablePlayerInventoryMenuSlot(stickInventoryIndex),
                    0, ClickType.PICKUP, "return the three unused sticks to their survival inventory slot");
            phase = Phase.WAIT_RECIPE_CURSOR_SETTLED;
            phaseTicks = MAX_STAGE_TICKS;
            return;
        }
        clickIntegratedServerMenu(minecraft, 0, 0, ClickType.PICKUP, "take configured wood barrel result");
        phase = Phase.WAIT_OUTPUT_CURSOR;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT vanilla result slot showed configured output; take-result click sent");
    }

    private static void waitForRecipeCursorSettled(Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof CraftingMenu menu)) {
            fail("crafting menu closed while returning unused sticks");
            return;
        }
        if (!menu.getCarried().isEmpty()) {
            if (timedOut("unused sticks did not return to their inventory slot: cursor=" + menu.getCarried())) return;
            return;
        }
        if (!menu.getSlot(0).getItem().is(configuredOutput())) {
            fail("configured recipe result changed while returning unused sticks");
            return;
        }
        clickIntegratedServerMenu(minecraft, 0, 0, ClickType.PICKUP, "take configured wood barrel result");
        phase = Phase.WAIT_OUTPUT_CURSOR;
        phaseTicks = MAX_STAGE_TICKS;
        LOGGER.info("SURVIVAL_PART_CRAFT unused sticks returned; taking configured recipe output");
    }

    private static void waitForOutputCursor(Minecraft minecraft) {
        if (!minecraft.player.containerMenu.getCarried().is(configuredOutput())) {
            if (timedOut("taking crafting output did not put the configured part on the cursor")) return;
            return;
        }
        clickIntegratedServerMenu(minecraft,
                SurvivalCraftingSlotLayout.playerInventoryMenuSlot(plankInventoryIndex), 0, ClickType.PICKUP,
                "store configured wood barrel in survival inventory");
        phase = Phase.WAIT_SETTLED_INVENTORY;
        phaseTicks = MAX_STAGE_TICKS;
    }

    private static void waitForSettledInventory(Minecraft minecraft) {
        var inventory = minecraft.player.getInventory();
        int planksRemaining = inventory.countItem(Items.OAK_PLANKS);
        int sticksRemaining = inventory.countItem(Items.STICK);
        int outputCount = inventory.countItem(configuredOutput());
        if (planksRemaining == 0 && sticksRemaining == 3 && outputCount == 1
                && inventory.countItem(Items.OAK_LOG) == 0 && minecraft.level.getBlockState(oakLogPos).isAir()
                && !minecraft.player.getAbilities().instabuild) {
            phase = Phase.PASSED;
            LOGGER.info("SURVIVAL_PART_CRAFT_ASSERTION PASS recipe=taczintetra:parts/wood_barrel, gameMode=SURVIVAL, oakLogBlock=minecraft:oak_log->air, oakLogInventory=0, oakPlanks=4->0, sticks=4->3, outputCount=1, harvest=gameMode.destroyBlock, intermediateCrafting=vanilla_inventory_menu, screen={}, tablePos={}",
                    minecraft.screen == null ? "closed" : minecraft.screen.getClass().getName(), craftingTablePos);
            return;
        }
        if (timedOut("ingredient consumption or output inventory did not settle: planks=" + planksRemaining
                + ", sticks=" + sticksRemaining + ", output=" + outputCount)) return;
    }

    private static int findInventorySlot(Minecraft minecraft, Item item, int minimumCount) {
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            ItemStack stack = minecraft.player.getInventory().getItem(index);
            if (stack.is(item) && stack.getCount() >= minimumCount) return index;
        }
        return -1;
    }

    private static int findEmptyInventorySlot(Minecraft minecraft) {
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            if (minecraft.player.getInventory().getItem(index).isEmpty()) return index;
        }
        return -1;
    }

    private static String nearbyDrops(net.minecraft.server.level.ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(oakLogPos).inflate(3.0D)).stream()
                .map(entity -> BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()) + "x" + entity.getItem().getCount()
                        + "@" + entity.position() + ":age=" + entity.tickCount
                        + ":distanceSqr=" + (player == null ? "missing" : player.distanceToSqr(entity)))
                .toList().toString();
    }

    private static Item configuredOutput() {
        return ModItems.configuredPart("wood_barrel").get();
    }

    private static boolean timedOut(String reason) {
        if (phaseTicks-- > 0) return false;
        fail(reason);
        return true;
    }

    private static void fail(String reason) {
        Phase failedPhase = phase;
        phase = Phase.FAILED;
        LOGGER.error("SURVIVAL_PART_CRAFT_ASSERTION FAIL phase={}, reason={}", failedPhase, reason);
    }
}
