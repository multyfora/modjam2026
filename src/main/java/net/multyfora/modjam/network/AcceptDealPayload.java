package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record AcceptDealPayload() implements CustomPacketPayload {
    public static final AcceptDealPayload INSTANCE = new AcceptDealPayload();
    public static final CustomPacketPayload.Type<AcceptDealPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "accept_deal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AcceptDealPayload> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
