package net.multyfora.modjam.client.godray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** One converging light shaft, described relative to the block it targets. */
public record GodrayBeam(
        Vec3 sourceDirection,
        float length,
        float topWidth,
        float bottomWidth,
        int color,
        float alpha,
        float flickerSpeed,
        float flickerPhase,
        float igniteDelay
) {
    public static GodrayBeam of(Vec3 sourceDirection, float length, float topWidth, int color) {
        return new GodrayBeam(sourceDirection.normalize(), length, topWidth, 0.02f, color, 0.55f, 0f, 0f, 0f);
    }
}

record GodraySite(BlockPos target, List<GodrayBeam> beams, long spawnMillis, float fadeInSeconds) {
    public float fadeMultiplier(GodrayBeam beam, long nowMillis) {
        if (fadeInSeconds <= 0f) return 1f;
        float elapsed = (nowMillis - spawnMillis) / 1000f - beam.igniteDelay();
        if (elapsed <= 0f) return 0f;
        return Math.clamp(elapsed / fadeInSeconds, 0f, 1f);
    }
}