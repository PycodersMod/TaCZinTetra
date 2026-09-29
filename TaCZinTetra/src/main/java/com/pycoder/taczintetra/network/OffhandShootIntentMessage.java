package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Server-authoritative right-click shot for the off-hand pistol in dual mode. */
public record OffhandShootIntentMessage(int stackIdentity, int sequence, long timestamp) {
    private static final Map<ServerPlayer, Integer> LAST_SEQUENCE = new WeakHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(OffhandShootIntentMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.stackIdentity());
        buffer.writeInt(message.sequence());
        buffer.writeLong(message.timestamp());
    }

    public static OffhandShootIntentMessage decode(FriendlyByteBuf buffer) {
        return new OffhandShootIntentMessage(buffer.readInt(), buffer.readInt(), buffer.readLong());
    }

    public static void handle(OffhandShootIntentMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }
        ServerPlayer sender = context.getSender();
        if (sender != null) context.enqueueWork(() -> shoot(sender, message));
        context.setPacketHandled(true);
    }

    private static synchronized void shoot(ServerPlayer sender, OffhandShootIntentMessage message) {
        if (message.sequence() <= LAST_SEQUENCE.getOrDefault(sender, 0)) return;
        ItemStack main = sender.getMainHandItem();
        ItemStack offhand = sender.getOffhandItem();
        boolean mainIsModular = main.getItem() instanceof ModularGunItem;
        boolean mainTwoHanded = mainIsModular
                && TetraItemStackProfileResolver.isTwoHanded((ModularGunItem) main.getItem(), main);
        if (!com.pycoder.taczintetra.runtime.GunInputRouter.offhandShootAllowed(
                mainIsModular, offhand.getItem() instanceof ModularGunItem, mainTwoHanded)) return;
        ModularGunItem offhandGun = (ModularGunItem) offhand.getItem();
        if (StackIdentity.of(offhand) != message.stackIdentity()
                || ReloadRuntimeCoordinator.handReloading(sender, InteractionHand.OFF_HAND)
                || ReloadRuntimeCoordinator.otherHandReloading(sender, InteractionHand.OFF_HAND)) return;
        StackIdentity.ensureUniqueId(offhand);

        if (Boolean.getBoolean("taczintetra.dev_automation")) {
            com.pycoder.taczintetra.client.DevServerAutomation.stopOffhandBroadcast(sender);
        }

        IGunOperator operator = IGunOperator.fromLivingEntity(sender);
        var data = operator.getDataHolder();
        var previous = data.currentGunItem;
        data.currentGunItem = () -> sender.getItemInHand(InteractionHand.OFF_HAND);
        try {
            int ammoBefore = offhandGun.getCurrentAmmoCount(offhand);
            float heatBefore = offhandGun.getHeatAmount(offhand);
            ShootResult result = operator.shoot(sender::getXRot, sender::getYRot, message.timestamp());
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info(
                        "TaCZinTetra dev off-hand shoot: result={}, ammo={}->{}, heat={}->{}, sequence={}, identity={}",
                        result, ammoBefore, offhandGun.getCurrentAmmoCount(offhand),
                        heatBefore, offhandGun.getHeatAmount(offhand), message.sequence(), message.stackIdentity());
            }
            if (result == ShootResult.SUCCESS) {
                LAST_SEQUENCE.put(sender, message.sequence());
            }
        } finally {
            data.currentGunItem = previous;
        }
    }
}
