package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;

/** Observes real server reload state and interrupts it by changing the held hotbar stack. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class DevF06ReloadInterruptServerAutomation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_f06_reload_interrupt_probe");
    private static final String STAGE = "taczintetra_f06_reload_interrupt_stage";
    private static final String GUN_SLOT = "taczintetra_f06_reload_interrupt_gun_slot";
    private static final String OTHER_SLOT = "taczintetra_f06_reload_interrupt_other_slot";
    private static final String GUN_IDENTITY = "taczintetra_f06_reload_interrupt_identity";
    private static final String AMMO_BEFORE = "taczintetra_f06_reload_interrupt_ammo";
    private static final String RESOURCE_BEFORE = "taczintetra_f06_reload_interrupt_resource";
    private static final String WAIT_TICKS = "taczintetra_f06_reload_interrupt_wait";
    private static final int MAX_RELOAD_ACTIVE_WAIT_TICKS = 40;
    private static final Map<ServerPlayer, ItemStack> ORIGINAL_GUNS = new WeakHashMap<>();

    private DevF06ReloadInterruptServerAutomation() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (!ENABLED || event.phase != TickEvent.Phase.END || player.level().isClientSide()
                || !(player instanceof ServerPlayer serverPlayer)) return;
        var data = player.getPersistentData();
        int stage = data.getInt(STAGE);
        if (stage >= 3) return;

        if (stage == 0) {
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof ModularGunItem gun)
                    || !held.getOrCreateTag().getBoolean("taczintetra_dev_modules_ready")
                    || !held.getOrCreateTag().getBoolean("taczintetra_dev_f06_reload_probe_ready")) return;
            int gunSlot = player.getInventory().selected;
            int otherSlot = (gunSlot + 1) % 9;
            StackIdentity.ensureUniqueId(held);
            int ammo = gun.getCurrentAmmoCount(held);
            int reserve = ResourceNbtAdapter.read(held.getOrCreateTag(),
                    ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm");
            player.getInventory().setItem(otherSlot, new ItemStack(Items.STICK));
            serverPlayer.containerMenu.broadcastChanges();
            ORIGINAL_GUNS.put(serverPlayer, held);
            data.putInt(GUN_SLOT, gunSlot);
            data.putInt(OTHER_SLOT, otherSlot);
            data.putInt(GUN_IDENTITY, StackIdentity.of(held));
            data.putInt(AMMO_BEFORE, ammo);
            data.putInt(RESOURCE_BEFORE, reserve);
            data.putInt(WAIT_TICKS, 0);
            data.putInt(STAGE, 1);
            LOGGER.info("F06_RELOAD_INTERRUPT_SETUP identity={} gunSlot={} otherSlot={} ammo={} resource9mm={}",
                    StackIdentity.of(held), gunSlot, otherSlot, ammo, reserve);
            return;
        }

        int waitTicks = data.getInt(WAIT_TICKS) + 1;
        data.putInt(WAIT_TICKS, waitTicks);
        int gunSlot = data.getInt(GUN_SLOT);
        ItemStack original = ORIGINAL_GUNS.get(serverPlayer);
        if (original == null) {
            fail(serverPlayer, "original modular gun reference was lost");
            return;
        }
        if (!(original.getItem() instanceof ModularGunItem gun)) {
            fail(serverPlayer, "original modular gun disappeared from its inventory slot");
            return;
        }
        var holder = IGunOperator.fromLivingEntity(player).getDataHolder();
        boolean pending = ReloadRuntimeSettlementService.isPending(original);
        if (stage == 1) {
            if (pending && holder.reloadStateType != null && holder.reloadStateType.isReloading()) {
                int otherSlot = data.getInt(OTHER_SLOT);
                player.getInventory().selected = otherSlot;
                serverPlayer.connection.send(new ClientboundSetCarriedItemPacket(otherSlot));
                data.putInt(STAGE, 2);
                data.putInt(WAIT_TICKS, 0);
                LOGGER.info("F06_RELOAD_INTERRUPT_REQUEST state={} pending=true activeTicks={} ammo={} resource9mm={}",
                        holder.reloadStateType, waitTicks, gun.getCurrentAmmoCount(original),
                        ResourceNbtAdapter.read(original.getOrCreateTag(),
                                ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm"));
            } else if (waitTicks >= MAX_RELOAD_ACTIVE_WAIT_TICKS) {
                fail(serverPlayer, "client reload intent did not reach an active pending server reload; pending="
                        + pending + ", state=" + holder.reloadStateType + ", waitTicks=" + waitTicks);
            } else if (waitTicks == 1 || waitTicks % 20 == 0) {
                LOGGER.info("F06_RELOAD_INTERRUPT_WAIT pending={} state={} ammo={} waitTicks={}", pending,
                        holder.reloadStateType, gun.getCurrentAmmoCount(original), waitTicks);
            }
            return;
        }

        if (stage == 2) {
            boolean holderReloading = holder.reloadStateType != null && holder.reloadStateType.isReloading();
            ItemStack slotStack = player.getInventory().getItem(gunSlot);
            ModularGunItem slotGun = slotStack.getItem() instanceof ModularGunItem modularGun ? modularGun : null;
            boolean pendingOnSlot = slotGun != null && ReloadRuntimeSettlementService.isPending(slotStack);
            if (pending || pendingOnSlot || holderReloading) {
                if (waitTicks >= 40) fail(serverPlayer, "reload state did not clear after hotbar stack change");
                return;
            }
            int slotIdentity = StackIdentity.of(slotStack);
            boolean sameLogicalStack = DevF06ReloadInterruptPolicy.isSameLogicalGunStack(
                    data.getInt(GUN_IDENTITY), slotIdentity, slotGun != null);
            int ammoAfter = slotGun == null ? -1 : slotGun.getCurrentAmmoCount(slotStack);
            int resourceAfter = slotGun == null ? -1 : ResourceNbtAdapter.read(slotStack.getOrCreateTag(),
                    ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm");
            boolean passes = DevF06ReloadInterruptPolicy.passes(data.getInt(AMMO_BEFORE), ammoAfter,
                    data.getInt(RESOURCE_BEFORE), resourceAfter, true, pending || pendingOnSlot,
                    holderReloading, sameLogicalStack);
            LOGGER.info("F06_RELOAD_INTERRUPT_ASSERTION {} identity={} slotIdentity={} slotIsGun={} ammo={}->{} resource9mm={}->{} pendingOriginalAfter={} pendingSlotAfter={} holderState={} waitTicks={}",
                    passes ? "PASS" : "FAIL", data.getInt(GUN_IDENTITY), slotIdentity, slotGun != null,
                    data.getInt(AMMO_BEFORE), ammoAfter, data.getInt(RESOURCE_BEFORE), resourceAfter,
                    pending, pendingOnSlot, holder.reloadStateType, waitTicks);
            int gunSlotToRestore = data.getInt(GUN_SLOT);
            player.getInventory().selected = gunSlotToRestore;
            serverPlayer.connection.send(new ClientboundSetCarriedItemPacket(gunSlotToRestore));
            serverPlayer.containerMenu.broadcastChanges();
            data.putInt(STAGE, 3);
            ORIGINAL_GUNS.remove(serverPlayer);
            return;
        }
    }

    private static void fail(ServerPlayer player, String reason) {
        player.getPersistentData().putInt(STAGE, 3);
        ORIGINAL_GUNS.remove(player);
        LOGGER.error("F06_RELOAD_INTERRUPT_ASSERTION FAIL reason={}", reason);
    }
}
