package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record FirstContactTogglePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FirstContactTogglePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "first_contact_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FirstContactTogglePayload> STREAM_CODEC =
        StreamCodec.unit(new FirstContactTogglePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
