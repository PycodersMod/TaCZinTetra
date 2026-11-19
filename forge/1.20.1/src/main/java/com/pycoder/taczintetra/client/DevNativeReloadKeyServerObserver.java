package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Read-only logical-server observer for the real reload-key result. */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID)
public final class DevNativeReloadKeyServerObserver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_native_reload_key_probe");
    private static final Map<UUID, String> PREVIOUS = new HashMap<>();
    private static final Map<UUID, Boolean> READY_LOGGED = new HashMap<>();

    private DevNativeReloadKeyServerObserver() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ModularGunItem gun)) return;
        boolean modulesReady = stack.getOrCreateTag().getBoolean("taczintetra_dev_modules_ready");
        boolean playerAlive = player.isAlive();

        int rawAmmoBeforeGet = stack.getOrCreateTag().getInt("GunCurrentAmmoCount");
        int maxAmmo = gun.getMaxDummyAmmoAmount(stack);
        int ammo = gun.getCurrentAmmoCount(stack);
        int rawAmmoAfterGet = stack.getOrCreateTag().getInt("GunCurrentAmmoCount");
        int reserve = ResourceNbtAdapter.read(stack.getOrCreateTag(),
                ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm");
        boolean hasBulletInBarrel = gun.hasBulletInBarrel(stack);
        int identity = StackIdentity.of(stack);
        String uuid = StackIdentity.hasUniqueId(stack)
                ? stack.getOrCreateTag().getUUID(StackIdentity.UNIQUE_ID).toString() : "missing";
        var reloadState = IGunOperator.fromLivingEntity(player).getDataHolder().reloadStateType;
        String state = reloadState == null ? "null" : reloadState.name();
        String snapshot = modulesReady + "|" + playerAlive + "|" + identity + "|" + uuid + "|" + rawAmmoBeforeGet + "|" + maxAmmo
                + "|" + ammo + "|" + rawAmmoAfterGet + "|" + reserve + "|"
                + hasBulletInBarrel + "|" + state;

        if (player.isAlive() && NativeReloadReadinessPolicy.isReady(ammo, reserve, hasBulletInBarrel)
                && !READY_LOGGED.getOrDefault(player.getUUID(), false)) {
            READY_LOGGED.put(player.getUUID(), true);
            LOGGER.info("NATIVE_RELOAD_KEY_READY side=server player={} playerAlive={} modulesReady={} stackIdentity={} stackUuid={} ammo={} reserve9mm={} hasBulletInBarrel={} reloadState={}",
                    player.getGameProfile().getName(), playerAlive, modulesReady, identity, uuid, ammo, reserve,
                    hasBulletInBarrel, state);
        }
        if (!snapshot.equals(PREVIOUS.put(player.getUUID(), snapshot))) {
            LOGGER.info("NATIVE_RELOAD_KEY_STATE side=server player={} playerAlive={} modulesReady={} stackIdentity={} stackUuid={} rawAmmoBeforeGet={} maxAmmo={} ammo={} rawAmmoAfterGet={} reserve9mm={} hasBulletInBarrel={} reloadState={}",
                    player.getGameProfile().getName(), playerAlive, modulesReady, identity, uuid, rawAmmoBeforeGet, maxAmmo,
                    ammo, rawAmmoAfterGet, reserve, hasBulletInBarrel, state);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PREVIOUS.remove(id);
        READY_LOGGED.remove(id);
    }
}
