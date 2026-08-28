package net.multyfora.modjam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;

import java.util.List;

public record JournalSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<JournalSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modjam.MODID, "journal_sync"));

    public record Entry(String id, String image, String description, boolean descriptionIsKey, String title, boolean titleIsKey) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
            (buf, e) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, e.id());
                ByteBufCodecs.STRING_UTF8.encode(buf, e.image() == null ? "" : e.image());
                ByteBufCodecs.STRING_UTF8.encode(buf, e.description());
                ByteBufCodecs.BOOL.encode(buf, e.descriptionIsKey());
                ByteBufCodecs.STRING_UTF8.encode(buf, e.title() == null ? "" : e.title());
                ByteBufCodecs.BOOL.encode(buf, e.titleIsKey());
            },
            buf -> new Entry(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                emptyToNull(ByteBufCodecs.STRING_UTF8.decode(buf)),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                emptyToNull(ByteBufCodecs.STRING_UTF8.decode(buf)),
                ByteBufCodecs.BOOL.decode(buf)
            )
        );

        private static String emptyToNull(String s) {
            return s.isEmpty() ? null : s;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalSyncPayload> STREAM_CODEC =
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(JournalSyncPayload::new, JournalSyncPayload::entries).cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
