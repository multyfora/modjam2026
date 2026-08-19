package net.multyfora.modjam.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record NotFiredDialogueTrigger(String event) implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("modjam", "not_fired");

    public static final MapCodec<NotFiredDialogueTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("event").forGetter(NotFiredDialogueTrigger::event)
    ).apply(instance, NotFiredDialogueTrigger::new));

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
        return DialogueEventManager.getFiredDayTime(player, eventId) < 0;
    }
}