package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record LabMusicPayload(boolean play) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LabMusicPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "lab_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LabMusicPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, LabMusicPayload::play,
            LabMusicPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
