package net.multyfora.don.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record TimeAfterDialogueTrigger(String event, long ticks) implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("don", "time_after");
    public static final long TWO_DAYS = 48000L;

    public static final MapCodec<TimeAfterDialogueTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("event").forGetter(TimeAfterDialogueTrigger::event),
        Codec.LONG.optionalFieldOf("ticks", TWO_DAYS).forGetter(TimeAfterDialogueTrigger::ticks)
    ).apply(instance, TimeAfterDialogueTrigger::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public boolean matches(ServerPlayer player) {
        Identifier eventId;
        try {
            eventId = Identifier.parse(event);
        } catch (Exception e) {
            return false;
        }
        long firedAt = DialogueEventManager.getFiredDayTime(player, eventId);
        if (firedAt < 0) return false;
        return player.level().getOverworldClockTime() - firedAt >= ticks;
    }
}