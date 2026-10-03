package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.api.entity.IGunOperator;
import com.pycoder.taczintetra.runtime.ReloadRuntimeCoordinator;
import com.pycoder.taczintetra.runtime.ReloadRuntimeSettlementService;
import com.pycoder.taczintetra.compat.ModularGunLifecycleAdapter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** 客户端意图；服务端会在后续结算前验证当前手部状态。 */
public record ReloadIntentMessage(boolean offHand, boolean singleBatch,
                                  int stackIdentity, int sequence) {
    private static final String FILL_MODE = "taczintetra_reload_fill";
    private static final Map<ServerPlayer, ReloadIntentServerGate> SERVER_GATES = new WeakHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(ReloadIntentMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.offHand());
        buffer.writeBoolean(message.singleBatch());
        buffer.writeInt(message.stackIdentity());
        buffer.writeInt(message.sequence());
    }

    public static ReloadIntentMessage decode(FriendlyByteBuf buffer) {
        return new ReloadIntentMessage(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readInt(), buffer.readInt());
    }

    public static void handle(ReloadIntentMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }
        if (!ReloadIntentValidator.isValid(message)) {
            context.setPacketHandled(true);
            return;
        }
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> validateCurrentHand(sender, message));
        }
        context.setPacketHandled(true);
    }

    private static void validateCurrentHand(ServerPlayer sender, ReloadIntentMessage message) {
        InteractionHand hand = message.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = sender.getItemInHand(hand);
        boolean hasGun = held.getItem() instanceof ModularGunItem;
        ReloadIntentServerGate gate = SERVER_GATES.computeIfAbsent(sender,
                ignored -> new ReloadIntentServerGate());
        boolean accepted = gate.accept(message, StackIdentity.of(held), hasGun,
                ReloadRuntimeCoordinator.otherHandReloading(sender, hand),
                held.getDamageValue() >= held.getMaxDamage() && held.isDamageableItem(),
                held.getOrCreateTag().getBoolean("taczintetra_overheat"));
        if (accepted) {
            StackIdentity.ensureUniqueId(held);
            held.getOrCreateTag().putBoolean(FILL_MODE, !message.singleBatch());
            IGunOperator operator = IGunOperator.fromLivingEntity(sender);
            operator.getDataHolder().currentGunItem = () -> sender.getItemInHand(hand);
            boolean started = ModularGunLifecycleAdapter.startReload(operator.getDataHolder(), held, sender);
            boolean pending = ReloadRuntimeSettlementService.isPending(held);
            if (!started || !pending) {
                held.getOrCreateTag().remove(FILL_MODE);
                gate.rollback(message);
            }
            if (Boolean.getBoolean("taczintetra.dev_automation")) {
                LOGGER.info("TaCZinTetra dev reload intent result: hand={}, started={}, pending={}, fill={}, ammo={}, resource9mm={}, identity={}, sequence={}",
                        hand, started, pending, !message.singleBatch(),
                        ((ModularGunItem) held.getItem()).getCurrentAmmoCount(held),
                        com.pycoder.taczintetra.logic.ResourceNbtAdapter.read(held.getOrCreateTag(),
                                com.pycoder.taczintetra.runtime.ResourceInsertionService.ITEM_CHANNEL, "tacz:9mm"),
                        message.stackIdentity(), message.sequence());
            }
        } else if (Boolean.getBoolean("taczintetra.dev_automation")) {
            LOGGER.info("TaCZinTetra dev reload intent rejected: hand={}, identity={}, sequence={}",
                    hand, message.stackIdentity(), message.sequence());
        }
    }
}
