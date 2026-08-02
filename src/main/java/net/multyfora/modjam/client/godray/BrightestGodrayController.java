package net.multyfora.modjam.client.godray;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.world.dimension.ModDimensions;
import net.multyfora.modjam.world.entity.BrightestEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public final class BrightestGodrayController {

    private static final float FADE_IN_SECONDS = 1.0f;
    private static final float IGNITE_SPREAD_SECONDS = 1.4f;
    private static final int BEAM_COUNT = 9;
    private static final int BURST_COUNT = 22;
    private static final int MOTES_PER_TICK = 2;
    private static final double MOTE_NEAR_CUTOFF = 3.5;
    private static final Random RANDOM = new Random();

    private BrightestGodrayController() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        Level level = minecraft.level;
        if (level.dimension() != ModDimensions.FIRST_CONTACT_LEVEL_KEY) {
            GodrayRenderer.clear();
            return;
        }

        BrightestEntity brightest = findBrightest(level, minecraft.player.getBoundingBox());
        if (brightest == null) {
            GodrayRenderer.clear();
            return;
        }

        BlockPos target = brightest.blockPosition().above();
        if (!GodrayRenderer.contains(target)) {
            List<net.multyfora.modjam.client.godray.GodrayBeam> beams = new ArrayList<>();
            beams.addAll(GodrayRenderer.dome(BEAM_COUNT, 22f, 58f, 14f, 0.35f, 0xFFFFFFFF, 14721L, IGNITE_SPREAD_SECONDS));
            beams.addAll(GodrayRenderer.burst(BURST_COUNT, 1.8f, 0.16f, 0xFFFFF3C0, 88213L));
            GodrayRenderer.spawn(target, beams, FADE_IN_SECONDS);
        }

        double distanceToTarget = minecraft.player.position().distanceTo(Vec3.atCenterOf(target));
        if (distanceToTarget > MOTE_NEAR_CUTOFF) {
            spawnDustMotes(level, target);
        }
    }

    private static void spawnDustMotes(Level level, BlockPos target) {
        Vec3 center = Vec3.atCenterOf(target);
        for (int i = 0; i < MOTES_PER_TICK; i++) {
            double azimuth = RANDOM.nextDouble() * Math.PI * 2;
            double polar = Math.toRadians(20 + RANDOM.nextDouble() * 45);
            double radius = 3.0 + RANDOM.nextDouble() * 8.0;

            double dx = Math.sin(polar) * Math.cos(azimuth);
            double dz = Math.sin(polar) * Math.sin(azimuth);
            double dy = Math.cos(polar);

            double x = center.x + dx * radius;
            double y = center.y + dy * radius;
            double z = center.z + dz * radius;

            double speed = 0.015 + RANDOM.nextDouble() * 0.01;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, -dx * speed, -dy * speed, -dz * speed);
        }

        if (RANDOM.nextFloat() < 0.3f) {
            level.addParticle(ParticleTypes.GLOW, center.x, center.y, center.z, 0.0, 0.01, 0.0);
        }
    }

    private static BrightestEntity findBrightest(Level level, AABB around) {
        AABB box = around.inflate(128.0);
        List<BrightestEntity> entities = level.getEntitiesOfClass(BrightestEntity.class, box);
        return entities.isEmpty() ? null : entities.get(0);
    }
}