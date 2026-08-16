package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record LightWeaverInfusePayload(int entityId, String packed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LightWeaverInfusePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "light_weaver_infuse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LightWeaverInfusePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LightWeaverInfusePayload::entityId,
            ByteBufCodecs.STRING_UTF8, LightWeaverInfusePayload::packed,
            LightWeaverInfusePayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
