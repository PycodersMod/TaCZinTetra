package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator;
import com.pycoder.taczintetra.logic.FireModePolicy;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.gun.FireMode;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;

/** Client intent for cycling the off-hand mode in dual-pistol mode. */
public record FireModeIntentMessage(boolean offHand, int stackIdentity, int sequence) {
    private static final Map<ServerPlayer, Integer> LAST_SEQUENCE = new WeakHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(FireModeIntentMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.offHand());
        buffer.writeInt(message.stackIdentity());
        buffer.writeInt(message.sequence());
    }

    public static FireModeIntentMessage decode(FriendlyByteBuf buffer) {
        return new FireModeIntentMessage(buffer.readBoolean(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(FireModeIntentMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }
        ServerPlayer sender = context.getSender();
        if (sender != null) context.enqueueWork(() -> apply(sender, message));
        context.setPacketHandled(true);
    }

    private static synchronized void apply(ServerPlayer sender, FireModeIntentMessage message) {
        if (!message.offHand() || message.sequence() <= LAST_SEQUENCE.getOrDefault(sender, 0)) return;
        InteractionHand hand = InteractionHand.OFF_HAND;
        ItemStack stack = sender.getItemInHand(hand);
        ItemStack mainStack = sender.getMainHandItem();
        if (!(mainStack.getItem() instanceof ModularGunItem mainGun)
                || TetraItemStackProfileResolver.isTwoHanded(mainGun, mainStack)) return;
        if (!(stack.getItem() instanceof ModularGunItem gun)
                || !FireModeIntentPolicy.canAccept(StackIdentity.of(stack), message.stackIdentity(), true,
                ReloadRuntimeCoordinator.handReloading(sender, hand)
                        || ReloadRuntimeCoordinator.otherHandReloading(sender, hand))) return;
        var gunIndex = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElse(null);
        if (gunIndex == null || gunIndex.getGunData() == null
                || gunIndex.getGunData().getFireModeSet() == null
                || gunIndex.getGunData().getFireModeSet().isEmpty()) return;
        StackIdentity.ensureUniqueId(stack);
        LAST_SEQUENCE.put(sender, message.sequence());
        gun.setFireMode(stack, FireModePolicy.next(gun.getFireMode(stack),
                gunIndex.getGunData().getFireModeSet()));
        LOGGER.info("TaCZinTetra fire-mode intent applied: hand=OFF, sequence={}, mode={}",
                message.sequence(), gun.getFireMode(stack));
    }
}
