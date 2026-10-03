package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.TaCZinTetra;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** 唯一协议通道；新增功能时在此注册对应数据包。 */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TaCZinTetra.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void registerMessages() {
        CHANNEL.registerMessage(0, ReloadIntentMessage.class,
                ReloadIntentMessage::encode, ReloadIntentMessage::decode,
                ReloadIntentMessage::handle);
        CHANNEL.registerMessage(1, FireModeIntentMessage.class,
                FireModeIntentMessage::encode, FireModeIntentMessage::decode,
                FireModeIntentMessage::handle);
        CHANNEL.registerMessage(2, OffhandShootIntentMessage.class,
                OffhandShootIntentMessage::encode, OffhandShootIntentMessage::decode,
                OffhandShootIntentMessage::handle);
        CHANNEL.registerMessage(3, ConfigDigestMessage.class,
                ConfigDigestMessage::encode, ConfigDigestMessage::decode,
                ConfigDigestMessage::handle);
    }

    private ModNetwork() {
    }
}
