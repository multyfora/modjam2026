package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record DialogueCompletePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DialogueCompletePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "dialogue_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueCompletePayload> STREAM_CODEC =
        StreamCodec.unit(new DialogueCompletePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
