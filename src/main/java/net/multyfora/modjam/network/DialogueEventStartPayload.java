package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

import java.util.List;

public record DialogueEventStartPayload(List<String> lines) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DialogueEventStartPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "dialogue_event_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueEventStartPayload> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
            .map(DialogueEventStartPayload::new, DialogueEventStartPayload::lines)
            .cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}