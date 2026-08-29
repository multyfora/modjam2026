package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record CutsceneCompletePayload(String id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CutsceneCompletePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "cutscene_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CutsceneCompletePayload> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(CutsceneCompletePayload::new, CutsceneCompletePayload::id).cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
