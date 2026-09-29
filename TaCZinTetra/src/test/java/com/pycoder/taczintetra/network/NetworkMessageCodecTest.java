package com.pycoder.taczintetra.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkMessageCodecTest {
    @Test
    void reloadIntentRoundTripsEveryField() {
        assertEquals(new ReloadIntentMessage(true, false, 123, 456),
                roundTrip(new ReloadIntentMessage(true, false, 123, 456), ReloadIntentMessage::encode,
                        ReloadIntentMessage::decode));
    }

    @Test
    void fireModeIntentRoundTripsEveryField() {
        assertEquals(new FireModeIntentMessage(true, 987, 654),
                roundTrip(new FireModeIntentMessage(true, 987, 654), FireModeIntentMessage::encode,
                        FireModeIntentMessage::decode));
    }

    @Test
    void offhandShootIntentRoundTripsEveryField() {
        assertEquals(new OffhandShootIntentMessage(321, 654, 123456789L),
                roundTrip(new OffhandShootIntentMessage(321, 654, 123456789L), OffhandShootIntentMessage::encode,
                        OffhandShootIntentMessage::decode));
    }

    private static <T> T roundTrip(T message, Encoder<T> encoder, Decoder<T> decoder) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            encoder.encode(message, buffer);
            return decoder.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    @FunctionalInterface
    private interface Encoder<T> {
        void encode(T message, FriendlyByteBuf buffer);
    }

    @FunctionalInterface
    private interface Decoder<T> {
        T decode(FriendlyByteBuf buffer);
    }
}
