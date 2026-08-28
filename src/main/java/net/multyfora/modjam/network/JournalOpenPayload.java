package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record JournalOpenPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<JournalOpenPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "journal_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalOpenPayload> STREAM_CODEC =
        StreamCodec.unit(new JournalOpenPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
