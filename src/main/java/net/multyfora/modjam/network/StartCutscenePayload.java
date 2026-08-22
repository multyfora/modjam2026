package net.multyfora.modjam.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

import java.util.List;

public record StartCutscenePayload(String id, int durationTicks, List<Frame> frames, List<String> lines, boolean lineSynced)
    implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartCutscenePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "start_cutscene"));

    public record Frame(
        int time,
        double x, double y, double z,
        boolean hasLookAt, double lookX, double lookY, double lookZ,
        boolean hasRot, float yaw, float pitch
    ) {
        public static final StreamCodec<ByteBuf, Frame> STREAM_CODEC = StreamCodec.of(
            (buf, frame) -> {
                buf.writeInt(frame.time());
                buf.writeDouble(frame.x());
                buf.writeDouble(frame.y());
                buf.writeDouble(frame.z());
                buf.writeBoolean(frame.hasLookAt());
                if (frame.hasLookAt()) {
                    buf.writeDouble(frame.lookX());
                    buf.writeDouble(frame.lookY());
                    buf.writeDouble(frame.lookZ());
                }
                buf.writeBoolean(frame.hasRot());
                if (frame.hasRot()) {
                    buf.writeFloat(frame.yaw());
                    buf.writeFloat(frame.pitch());
                }
            },
            buf -> {
                int time = buf.readInt();
                double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
                boolean hasLookAt = buf.readBoolean();
                double lx = 0, ly = 0, lz = 0;
                if (hasLookAt) {
                    lx = buf.readDouble();
                    ly = buf.readDouble();
                    lz = buf.readDouble();
                }
                boolean hasRot = buf.readBoolean();
                float yaw = 0, pitch = 0;
                if (hasRot) {
                    yaw = buf.readFloat();
                    pitch = buf.readFloat();
                }
                return new Frame(time, x, y, z, hasLookAt, lx, ly, lz, hasRot, yaw, pitch);
            }
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, StartCutscenePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, StartCutscenePayload::id,
        ByteBufCodecs.VAR_INT, StartCutscenePayload::durationTicks,
        Frame.STREAM_CODEC.apply(ByteBufCodecs.list()), StartCutscenePayload::frames,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), StartCutscenePayload::lines,
        ByteBufCodecs.BOOL, StartCutscenePayload::lineSynced,
        StartCutscenePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
