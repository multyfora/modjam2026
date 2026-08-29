package net.multyfora.don.client.godray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Client-side registry of active godray effects. Safe to call spawn/remove from any thread. */
public final class GodrayRenderer {
    private static final Map<BlockPos, GodraySite> SITES = Collections.synchronizedMap(new LinkedHashMap<>());

    private GodrayRenderer() {}

    public static void spawn(BlockPos target, List<GodrayBeam> beams) {
        spawn(target, beams, 1.5f);
    }

    public static void spawn(BlockPos target, List<GodrayBeam> beams, float fadeInSeconds) {
        BlockPos key = target.immutable();
        SITES.put(key, new GodraySite(key, List.copyOf(beams), System.currentTimeMillis(), fadeInSeconds));
    }

    public static void remove(BlockPos target) {
        SITES.remove(target);
    }

    public static boolean contains(BlockPos target) {
        return SITES.containsKey(target);
    }

    public static void clear() {
        SITES.clear();
    }

    public static List<GodraySite> activeSites() {
        synchronized (SITES) {
            return List.copyOf(SITES.values());
        }
    }

    public static List<GodrayBeam> dome(int count, float minAngleFromVerticalDeg, float maxAngleFromVerticalDeg,
                                        float length, float topWidth, int color, long seed, float igniteSpreadSeconds) {
        Random random = new Random(seed);
        List<GodrayBeam> beams = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double azimuth = (Math.PI * 2 * i / count) + random.nextDouble(-0.3, 0.3);
            double polar = Math.toRadians(minAngleFromVerticalDeg
                    + random.nextFloat() * (maxAngleFromVerticalDeg - minAngleFromVerticalDeg));
            double x = Math.sin(polar) * Math.cos(azimuth);
            double z = Math.sin(polar) * Math.sin(azimuth);
            double y = Math.cos(polar);
            float lengthJitter = length * (0.9f + random.nextFloat() * 0.2f);
            float widthJitter = topWidth * (0.85f + random.nextFloat() * 0.3f);
            float bottomWidth = Math.max(0.08f, widthJitter * 0.1f);
            float phase = random.nextFloat() * (float) (Math.PI * 2);
            float igniteDelay = (i / (float) count) * igniteSpreadSeconds + random.nextFloat() * 0.15f;
            beams.add(new GodrayBeam(new Vec3(x, y, z), lengthJitter, widthJitter, bottomWidth, color,
                    0.5f + random.nextFloat() * 0.15f, 0.15f + random.nextFloat() * 0.15f, phase, igniteDelay));
        }
        return beams;
    }

    public static List<GodrayBeam> burst(int count, float length, float topWidth, int color, long seed) {
        Random random = new Random(seed);
        List<GodrayBeam> beams = new ArrayList<>(count);
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double yComp = 1 - 2 * t;
            double radius = Math.sqrt(Math.max(0, 1 - yComp * yComp));
            double theta = goldenAngle * i;
            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;

            float lengthJitter = length * (0.7f + random.nextFloat() * 0.6f);
            float widthJitter = topWidth * (0.7f + random.nextFloat() * 0.6f);
            float phase = random.nextFloat() * (float) (Math.PI * 2);

            beams.add(new GodrayBeam(new Vec3(x, yComp, z), lengthJitter, widthJitter,
                    Math.max(0.05f, widthJitter * 0.15f), color, 0.9f + random.nextFloat() * 0.1f,
                    0.4f + random.nextFloat() * 0.3f, phase, 0f));
        }
        return beams;
    }
}