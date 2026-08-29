package net.multyfora.don.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.multyfora.don.dialogue.DialogueTrigger;

import java.util.List;
import java.util.Optional;

public record JournalEntryDefinition(
    Optional<Identifier> image,
    String description,
    boolean descriptionIsKey,
    Optional<String> title,
    boolean titleIsKey,
    List<DialogueTrigger> triggers
) {
    public static final Codec<JournalEntryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("image").forGetter(JournalEntryDefinition::image),
        Codec.STRING.fieldOf("description").forGetter(JournalEntryDefinition::description),
        Codec.BOOL.optionalFieldOf("descriptionIsKey", true).forGetter(JournalEntryDefinition::descriptionIsKey),
        Codec.STRING.optionalFieldOf("title").forGetter(JournalEntryDefinition::title),
        Codec.BOOL.optionalFieldOf("titleIsKey", true).forGetter(JournalEntryDefinition::titleIsKey),
        DialogueTrigger.CODEC.listOf().fieldOf("triggers")
            .validate(list -> list.isEmpty()
                ? DataResult.error(() -> "Journal entry must have at least one trigger")
                : DataResult.success(list))
            .forGetter(JournalEntryDefinition::triggers)
    ).apply(instance, JournalEntryDefinition::new));
}
