package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.TaCZinTetra;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.Supplier;

/** 登录后发送的服务端权威配置兼容性检查。 */
public record ConfigDigestMessage(String digest) {
    public static void encode(ConfigDigestMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.digest() == null ? "" : message.digest(), 32);
    }

    public static ConfigDigestMessage decode(FriendlyByteBuf buffer) {
        return new ConfigDigestMessage(buffer.readUtf(32));
    }

    public static void handle(ConfigDigestMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            if (!ConfigDigest.of(TaCZinTetra.MODULE_CONFIG).equals(message.digest())) {
                context.getNetworkManager().disconnect(Component.literal(
                        "TaCZinTetra 配置与服务器不一致，请同步 taczintetra.json 后重连"));
            }
        });
        context.setPacketHandled(true);
    }
}
