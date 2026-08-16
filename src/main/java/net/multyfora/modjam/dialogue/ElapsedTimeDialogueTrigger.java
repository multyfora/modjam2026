package net.multyfora.modjam.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record ElapsedTimeDialogueTrigger(long ticks) implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("modjam", "elapsed");
    public static final long ONE_DAY = 24000L;

    public static final MapCodec<ElapsedTimeDialogueTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.LONG.optionalFieldOf("ticks", ONE_DAY).forGetter(ElapsedTimeDialogueTrigger::ticks)
    ).apply(instance, ElapsedTimeDialogueTrigger::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public boolean matches(ServerPlayer player) {
        return player.level().getGameTime() >= ticks;
    }
}