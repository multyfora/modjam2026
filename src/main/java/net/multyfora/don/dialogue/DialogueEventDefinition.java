package net.multyfora.don.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record DialogueEventDefinition(List<DialogueTrigger> triggers, List<String> lines, boolean once) {
    public static final Codec<DialogueEventDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DialogueTrigger.CODEC.listOf().fieldOf("triggers")
            .validate(list -> list.isEmpty()
                ? DataResult.error(() -> "Dialogue event must have at least one trigger")
                : DataResult.success(list))
            .forGetter(DialogueEventDefinition::triggers),
        Codec.STRING.listOf().fieldOf("lines")
            .validate(list -> list.isEmpty()
                ? DataResult.error(() -> "Dialogue event must have at least one line")
                : DataResult.success(list))
            .forGetter(DialogueEventDefinition::lines),
        Codec.BOOL.optionalFieldOf("once", true).forGetter(DialogueEventDefinition::once)
    ).apply(instance, DialogueEventDefinition::new));
}