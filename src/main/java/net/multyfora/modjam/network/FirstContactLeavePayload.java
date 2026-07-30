package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record FirstContactLeavePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FirstContactLeavePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact_leave"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FirstContactLeavePayload> STREAM_CODEC =
        StreamCodec.unit(new FirstContactLeavePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
