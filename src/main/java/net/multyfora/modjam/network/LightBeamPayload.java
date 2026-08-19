package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record LightBeamPayload(
        double startX, double startY, double startZ,
        double dirX, double dirY, double dirZ,
        float range
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LightBeamPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "light_beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LightBeamPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeDouble(payload.startX);
            buf.writeDouble(payload.startY);
            buf.writeDouble(payload.startZ);
            buf.writeDouble(payload.dirX);
            buf.writeDouble(payload.dirY);
            buf.writeDouble(payload.dirZ);
            buf.writeFloat(payload.range);
        },
        buf -> new LightBeamPayload(
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat()
        )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}