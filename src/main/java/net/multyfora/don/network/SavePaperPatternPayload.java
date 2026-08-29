package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record SavePaperPatternPayload(int hand, String packed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SavePaperPatternPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "save_paper_pattern"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SavePaperPatternPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SavePaperPatternPayload::hand,
            ByteBufCodecs.STRING_UTF8, SavePaperPatternPayload::packed,
            SavePaperPatternPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}