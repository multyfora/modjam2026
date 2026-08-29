package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record FirstContactEnterPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FirstContactEnterPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "first_contact_enter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FirstContactEnterPayload> STREAM_CODEC =
        StreamCodec.unit(new FirstContactEnterPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
