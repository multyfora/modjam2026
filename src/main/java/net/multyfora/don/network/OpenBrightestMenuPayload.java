package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record OpenBrightestMenuPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBrightestMenuPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "open_brightest_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBrightestMenuPayload> STREAM_CODEC =
        StreamCodec.unit(new OpenBrightestMenuPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
