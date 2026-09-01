package net.multyfora.don.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record SealedSunChoicePayload(BlockPos pos, boolean help) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SealedSunChoicePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "sealed_sun_choice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SealedSunChoicePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, SealedSunChoicePayload::pos,
            ByteBufCodecs.BOOL, SealedSunChoicePayload::help,
            SealedSunChoicePayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
