package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

public record OpenCheatSheetPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenCheatSheetPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "open_cheat_sheet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCheatSheetPayload> STREAM_CODEC =
        StreamCodec.unit(new OpenCheatSheetPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
