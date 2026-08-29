package net.multyfora.don.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;

public record SetStarMysticalPayload(int x, int y, int z, double mystical) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetStarMysticalPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(don.MODID, "set_star_mystical"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetStarMysticalPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetStarMysticalPayload::x,
            ByteBufCodecs.VAR_INT, SetStarMysticalPayload::y,
            ByteBufCodecs.VAR_INT, SetStarMysticalPayload::z,
            ByteBufCodecs.DOUBLE, SetStarMysticalPayload::mystical,
            SetStarMysticalPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
