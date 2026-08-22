package net.multyfora.modjam.cutscene;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.multyfora.modjam.dialogue.DialogueTrigger;

import java.util.List;

public record CutsceneDefinition(
    List<DialogueTrigger> triggers,
    int durationTicks,
    List<CutsceneKeyframe> keyframes,
    List<String> lines,
    boolean once,
    boolean lineSynced
) {
    public static final Codec<CutsceneDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DialogueTrigger.CODEC.listOf().fieldOf("triggers")
            .validate(list -> list.isEmpty()
                ? DataResult.error(() -> "Cutscene must have at least one trigger")
                : DataResult.success(list))
            .forGetter(CutsceneDefinition::triggers),
        Codec.INT.optionalFieldOf("duration_ticks", 200).forGetter(CutsceneDefinition::durationTicks),
        CutsceneKeyframe.CODEC.listOf().fieldOf("keyframes")
            .validate(list -> list.isEmpty()
                ? DataResult.error(() -> "Cutscene must have at least one keyframe")
                : DataResult.success(list))
            .forGetter(CutsceneDefinition::keyframes),
        Codec.STRING.listOf().optionalFieldOf("lines", List.of()).forGetter(CutsceneDefinition::lines),
        Codec.BOOL.optionalFieldOf("once", true).forGetter(CutsceneDefinition::once),
        Codec.BOOL.optionalFieldOf("line_synced", false).forGetter(CutsceneDefinition::lineSynced)
    ).apply(instance, CutsceneDefinition::new));
}
