package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record BetrayedPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BetrayedPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "betrayed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BetrayedPayload> STREAM_CODEC =
        StreamCodec.unit(new BetrayedPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
