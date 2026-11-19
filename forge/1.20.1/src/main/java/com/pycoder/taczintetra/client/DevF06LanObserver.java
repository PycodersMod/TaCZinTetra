package com.pycoder.taczintetra.client;

import com.mojang.logging.LogUtils;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.logic.ResourceNbtAdapter;
import com.pycoder.taczintetra.network.StackIdentity;
import com.pycoder.taczintetra.runtime.ResourceInsertionService;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** 显式启用时，从 Guest 客户端记录 Host 副手枪械的已复制状态。 */
@Mod.EventBusSubscriber(modid = TaCZinTetra.MOD_ID, value = Dist.CLIENT)
public final class DevF06LanObserver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("taczintetra.dev_f06_lan_observer");
    private static final String HOST_USERNAME = System.getProperty("taczintetra.dev_f06_lan_host_username", "Dev");
    private static String lastSnapshot;

    private DevF06LanObserver() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Player host = minecraft.level.players().stream()
                .filter(candidate -> candidate != minecraft.player)
                .filter(candidate -> HOST_USERNAME.equals(candidate.getGameProfile().getName()))
                .findFirst().orElse(null);
        if (host == null || !(host.getOffhandItem().getItem() instanceof ModularGunItem gun)) return;
        var stack = host.getOffhandItem();
        var tag = stack.getOrCreateTag();
        int ammo = gun.getCurrentAmmoCount(stack);
        int reserve = ResourceNbtAdapter.read(tag, ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm");
        boolean pending = tag.getBoolean("taczintetra_reload_pending");
        String reloadState = tag.getString("taczintetra_reload_state");
        String snapshot = ammo + ":" + reserve + ":" + pending + ":" + reloadState;
        if (snapshot.equals(lastSnapshot)) return;
        lastSnapshot = snapshot;
        LOGGER.info("F06_LAN_HOST_OFFHAND_STATE epochMs={} host={} hostPlayerId={} item={} stackIdentity={} hasUniqueId={} ammo={} reserve9mm={} reloadPending={} reloadState={}",
                System.currentTimeMillis(), HOST_USERNAME, host.getId(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
                StackIdentity.of(stack), StackIdentity.hasUniqueId(stack), ammo, reserve, pending, reloadState);
    }
}
