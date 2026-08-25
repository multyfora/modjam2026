package net.multyfora.modjam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.block.CrystalTracker;
import net.multyfora.modjam.modjam;
import org.joml.Vector4f;

import java.util.List;

public class SingularityDarknessManager {
    private static final SingularityDarknessManager INSTANCE = new SingularityDarknessManager();

    public static final float DARK_CORE = 2.5f;
    public static final float DARK_RADIUS = 8.0f;

    private List<BlockPos> sites = List.of();

    private float screenU = 0.5f;
    private float screenV = 0.5f;
    private float screenRadius = 0f;
    private float intensity = 0f;

    public static SingularityDarknessManager getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        sites = level == null ? List.of() : CrystalTracker.crystals(level.dimension());
        spawnSuctionParticles(level);
    }

    public List<BlockPos> sites() {
        return sites;
    }

    public void updateScreenData(CameraRenderState camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            intensity = 0f;
            return;
        }

        Vec3 camPos = camera.pos;

        BlockPos best = null;
        float bestI = 0f;
        for (BlockPos pos : sites) {
            float dist = (float) Vec3.atCenterOf(pos).distanceTo(camPos);
            if (dist <= 0.01f) dist = 0.01f;
            float i = 1f - smoothstep(DARK_CORE, DARK_RADIUS, dist);
            if (i > bestI) {
                bestI = i;
                best = pos;
            }
        }

        if (best == null || bestI <= 0f) {
            intensity = 0f;
            return;
        }

        Vec3 world = Vec3.atCenterOf(best);
        Vector4f camSpace = camera.viewRotationMatrix.transform(
            new Vector4f(
                (float) (world.x - camPos.x),
                (float) (world.y - camPos.y),
                (float) (world.z - camPos.z),
                1f
            ),
            new Vector4f()
        );
        if (camSpace.z >= 0f) {
            intensity = 0f;
            return;
        }

        Vector4f clip = camera.projectionMatrix.transform(
            new Vector4f(camSpace.x, camSpace.y, camSpace.z, 1f),
            new Vector4f()
        );
        float w = clip.w;
        if (w == 0f || !Float.isFinite(clip.x) || !Float.isFinite(clip.y) || !Float.isFinite(clip.z)) {
            intensity = 0f;
            return;
        }
        float ndcX = clip.x / w;
        float ndcY = clip.y / w;

        float uvPerBlock = Math.abs(camera.projectionMatrix.m11()) * 0.5f / -camSpace.z;

        screenU = ndcX * 0.5f + 0.5f;
        screenV = ndcY * 0.5f + 0.5f;
        screenRadius = DARK_RADIUS * uvPerBlock;
        intensity = bestI;
    }

    public float getScreenU() {
        return screenU;
    }

    public float getScreenV() {
        return screenV;
    }

    public float getScreenRadius() {
        return screenRadius;
    }

    public float getIntensity() {
        return intensity;
    }

    public static void writeSingularityConfig(java.nio.ByteBuffer data, SingularityDarknessManager manager) {
        data.putFloat(0, manager.screenU);
        data.putFloat(4, manager.screenV);
        data.putFloat(8, manager.screenRadius);
        data.putFloat(12, manager.intensity);
    }

    private static void spawnSuctionParticles(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (level == null || mc.player == null) return;
        Vec3 playerPos = mc.player.getEyePosition();
        for (BlockPos pos : INSTANCE.sites) {
            Vec3 crystal = Vec3.atCenterOf(pos);
            float dist = (float) crystal.distanceTo(playerPos);
            if (dist > 12f) continue;
            int count = 2;
            for (int i = 0; i < count; i++) {
                double theta = Math.random() * Math.PI * 2;
                double phi = Math.acos(Math.random() * 2 - 1);
                double r = 1.5 + Math.random() * 3.0;
                double x = crystal.x + Math.sin(phi) * Math.cos(theta) * r;
                double y = crystal.y + Math.abs(Math.cos(phi)) * r * 0.7;
                double z = crystal.z + Math.sin(phi) * Math.sin(theta) * r;
                Vec3 toCrystal = crystal.subtract(x, y, z).normalize().scale(0.12);
                level.addParticle(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    toCrystal.x + (Math.random() - 0.5) * 0.01,
                    toCrystal.y + (Math.random() - 0.5) * 0.01,
                    toCrystal.z + (Math.random() - 0.5) * 0.01
                );
            }
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}