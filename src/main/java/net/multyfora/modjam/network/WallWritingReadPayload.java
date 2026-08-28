package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record WallWritingReadPayload(String plain) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WallWritingReadPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "wall_writing_read"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WallWritingReadPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(WallWritingReadPayload::new, WallWritingReadPayload::plain).cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
