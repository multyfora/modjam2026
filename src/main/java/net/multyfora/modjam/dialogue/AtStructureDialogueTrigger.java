package net.multyfora.modjam.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public record AtStructureDialogueTrigger(String tag) implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("modjam", "at_structure");
    public static final String DEFAULT_TAG = "minecraft:village";

    public static final MapCodec<AtStructureDialogueTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("tag", DEFAULT_TAG).forGetter(AtStructureDialogueTrigger::tag)
    ).apply(instance, AtStructureDialogueTrigger::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public boolean matches(ServerPlayer player) {
        Identifier tagId;
        try {
            tagId = Identifier.parse(tag);
        } catch (Exception e) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagId);
        StructureStart start = serverLevel.structureManager()
            .getStructureWithPieceAt(player.blockPosition(), tagKey);
        return start != StructureStart.INVALID_START;
    }
}