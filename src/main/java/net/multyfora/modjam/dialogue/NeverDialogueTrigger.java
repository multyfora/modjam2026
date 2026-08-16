package net.multyfora.modjam.dialogue;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record NeverDialogueTrigger() implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("modjam", "never");

    public static final MapCodec<NeverDialogueTrigger> CODEC = MapCodec.unit(NeverDialogueTrigger::new);

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public boolean matches(ServerPlayer player) {
        return false;
    }
}
