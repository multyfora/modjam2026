package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record RefuseDealPayload() implements CustomPacketPayload {
    public static final RefuseDealPayload INSTANCE = new RefuseDealPayload();
    public static final CustomPacketPayload.Type<RefuseDealPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "refuse_deal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefuseDealPayload> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
