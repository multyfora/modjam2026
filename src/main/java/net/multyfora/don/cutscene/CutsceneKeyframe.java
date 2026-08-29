package net.multyfora.don.cutscene;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public record CutsceneKeyframe(int time, Vec3 pos, Optional<Vec3> lookAt, Optional<Vec3> rot) {
    public static final Codec<Vec3> YAW_PITCH_CODEC = Codec.FLOAT.listOf().comapFlatMap(
        list -> list.size() == 2
            ? DataResult.success(new Vec3(list.get(0), list.get(1), 0))
            : DataResult.error(() -> "rot must be a list of 2 floats [yaw, pitch]"),
        v -> List.of((float) v.x, (float) v.y)
    );

    public static final Codec<CutsceneKeyframe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("t").forGetter(CutsceneKeyframe::time),
        Vec3.CODEC.fieldOf("pos").forGetter(CutsceneKeyframe::pos),
        Vec3.CODEC.optionalFieldOf("look_at").forGetter(CutsceneKeyframe::lookAt),
        YAW_PITCH_CODEC.optionalFieldOf("rot").forGetter(CutsceneKeyframe::rot)
    ).apply(instance, CutsceneKeyframe::new));
}
