package net.multyfora.modjam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.client.godray.GodrayPipelines;
import net.multyfora.modjam.modjam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public final class ShotBeamRenderer {
    public static final float BEAM_WIDTH = 1.6f;
    public static final int BEAM_COLOR = 0xFFFFF3C0;
    public static final double MUZZLE_DROP = 0.25;

    private static final int LIFETIME_TICKS = 56;
    private static final int GROW_TICKS = 14;
    private static final int DISSIPATE_START = 44;
    private static final int IMPACT_RING_TICKS = 10;
    private static final int RING_SWEEP_TICKS = GROW_TICKS;
    private static final int RING_LOCAL_FADE_TICKS = 3;
    private static final int MUZZLE_FLASH_TICKS = 8;
    private static final int EMBER_EXPOSE_TICKS = 8;

    private static final float START_OFFSET = 2.5f;
    private static final int BEAM_SEGMENTS = 24;
    private static final int FACE_COUNT = 4;
    private static final float SCROLL_SCALE = 0.2f;
    private static final float BOX_ALPHA = 0.95f;

    private static final float GROW_FLARE_BASE = 0.85f;
    private static final float GROW_FLARE_GAIN = 0.9f;

    private static final float PULSE_SPEED = 0.16f;
    private static final int PULSE_COUNT = 2;
    private static final float PULSE_STRENGTH = 0.75f;
    private static final float PULSE_SIGMA = 0.08f;

    private static final float VIBRATION_AMOUNT = 0.045f;
    private static final float VIBRATION_FREQ = 23f;

    private static final float MUZZLE_RADIUS = 2.4f;

    private static final int RING_SEGMENTS = 24;
    private static final int RING_TUBE_SEGMENTS = 6;
    private static final float RING_TUBE_RADIUS = 0.16f;
    private static final float RING_SPACING = 3.0f;
    private static final float RING_MIN_RADIUS = 0.6f;
    private static final float RING_MAX_RADIUS = 1.9f;
    private static final float RING_SWEEP_BOOST = 1.8f;

    private static final int EMBER_COUNT = 22;
    private static final float EMBER_SIZE = 0.05f;
    private static final float EMBER_DROP = 0.022f;
    private static final float EMBER_DRIFT = 0.03f;
    private static final float EMBER_ALPHA = 0.85f;

    private static final int FULL_BRIGHT = 15728880;

    private static final int HALO_R = 0xFF;
    private static final int HALO_G = 0xC8;
    private static final int HALO_B = 0x70;

    private static final ContextKey<List<ShotBeam>> DATA_KEY =
        new ContextKey<>(Identifier.fromNamespaceAndPath(modjam.MODID, "shot_beams"));

    private static final List<ShotBeam> ACTIVE = new ArrayList<>();

    public record ShotBeam(Vec3 start, Vec3 end, float farWidth, float nearWidth, int color, int age) {
    }

    private ShotBeamRenderer() {
    }

    public static void spawnShot(Vec3 start, Vec3 end, float farWidth, int color) {
        ACTIVE.add(new ShotBeam(start, end, farWidth, Math.max(0.25f, farWidth * 0.95f), color, 0));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.playLocalSound(start.x, start.y, start.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.5f, 1.0f, false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ACTIVE.removeIf(beam -> beam.age() >= LIFETIME_TICKS);
        for (int i = 0; i < ACTIVE.size(); i++) {
            ShotBeam beam = ACTIVE.get(i);
            int newAge = beam.age() + 1;
            if (newAge == GROW_TICKS) {
                ShotBeamImpactOverlay.trigger(1.0f);
            }
            ACTIVE.set(i, new ShotBeam(beam.start(), beam.end(), beam.farWidth(), beam.nearWidth(), beam.color(), newAge));
        }
    }

    @SubscribeEvent
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(ACTIVE));
    }

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        List<ShotBeam> beams = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (beams == null || beams.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        for (ShotBeam beam : beams) {
            Vec3 offset = beam.start().subtract(event.getLevelRenderState().cameraRenderState.pos);
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            submitBeam(event, poseStack, beam);
            poseStack.popPose();
        }
    }

    private static void submitBeam(SubmitCustomGeometryEvent event, PoseStack poseStack, ShotBeam beam) {
        event.getSubmitNodeCollector().submitCustomGeometry(
            poseStack,
            GodrayPipelines.BEAM,
            (pose, buffer) -> drawBeam(pose, buffer, beam));
    }

    private static float smoothstep(float t) {
        t = Math.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float easeOut(float t) {
        return 1f - (1f - Math.clamp(t, 0f, 1f)) * (1f - Math.clamp(t, 0f, 1f));
    }

    private static float growProgress(int age) {
        return age <= 0 ? 0.06f : easeOut(age / (float) GROW_TICKS);
    }

    private static float dissipateFade(int age) {
        if (age <= DISSIPATE_START) {
            return 1f;
        }
        float t = (age - DISSIPATE_START) / (float) (LIFETIME_TICKS - DISSIPATE_START);
        float fade = 1f - smoothstep(t);
        float flicker = 0.6f + 0.4f * (float) Math.sin(age * 2.6);
        return Math.max(0f, fade * flicker);
    }

    private static float envelope(double u) {
        if (u < 0.02) {
            return GROW_FLARE_BASE + GROW_FLARE_GAIN * (float) (u / 0.02);
        }
        if (u < 0.92) {
            return 1f;
        }
        double t = (u - 0.92) / 0.08;
        double taper = Math.max(0.0, 1.0 - t);
        double taper2 = taper * taper;
        double flare = Math.sin((1.0 - taper) * Math.PI) * 0.22;
        return (float) Math.max(0.05, taper2 + flare);
    }

    private static void drawBeam(PoseStack.Pose pose, VertexConsumer buffer, ShotBeam beam) {
        Vec3 delta = beam.end().subtract(beam.start());
        double length = delta.length();
        if (length - START_OFFSET < 1.0) {
            return;
        }
        int age = beam.age();
        Vec3 dir = delta.scale(1.0 / length);

        Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 base1 = dir.cross(up).normalize();
        Vec3 base2 = dir.cross(base1).normalize();

        float grow = growProgress(age);
        float fade = dissipateFade(age);
        if (fade <= 0f) {
            return;
        }

        drawBox(pose, buffer, dir, length, base1, base2, beam, grow, fade);
        if (age <= GROW_TICKS) {
            drawSweepRings(pose, buffer, dir, length, base1, base2, age, grow);
            drawMuzzleFlash(pose, buffer, dir, base1, base2, age);
        } else {
            drawImpactRing(pose, buffer, dir, length, base1, base2, age);
            drawEmbers(pose, buffer, dir, length, base1, base2, beam, age, fade);
        }
    }

    private static float brightnessBoost(float u, int age) {
        if (age <= GROW_TICKS) {
            return 1f;
        }
        float t = age - GROW_TICKS;
        float boost = 1f;
        for (int k = 0; k < PULSE_COUNT; k++) {
            float center = ((t * PULSE_SPEED) + k * 0.5f) % 1f;
            float d = Math.abs(u - center);
            d = Math.min(d, 1f - d);
            boost += PULSE_STRENGTH * (float) Math.exp(-(d * d) / (2f * PULSE_SIGMA * PULSE_SIGMA));
        }
        return Math.min(1.9f, boost);
    }

    private static void drawBox(PoseStack.Pose pose, VertexConsumer buffer, Vec3 dir, double length,
                                Vec3 base1, Vec3 base2, ShotBeam beam, float grow, float fade) {
        float halfMax = beam.farWidth() * 0.5f;
        double drawLen = length - START_OFFSET;
        int age = beam.age();
        float vib = age > GROW_TICKS ? 1f + VIBRATION_AMOUNT : 1f;
        float diode = (float) Math.sqrt(2.0);

        int segs = Math.max(1, Math.round(grow * BEAM_SEGMENTS));
        for (int s = 0; s < segs; s++) {
            double u0 = (double) s / BEAM_SEGMENTS;
            double u1 = (double) (s + 1) / BEAM_SEGMENTS;
            if (u0 >= grow) {
                continue;
            }
            u1 = Math.min(u1, grow);
            float h0 = (float) (halfMax * envelope(u0) * (1f + vib * (float) Math.sin(age * VIBRATION_FREQ + (float) u0 * 37f)));
            float h1 = (float) (halfMax * envelope(u1) * (1f + vib * (float) Math.sin(age * VIBRATION_FREQ + (float) u1 * 37f)));

            Vec3 p0 = dir.scale(START_OFFSET + u0 * drawLen);
            Vec3 p1 = dir.scale(START_OFFSET + u1 * drawLen);

            double y0 = (START_OFFSET + u0 * drawLen) * SCROLL_SCALE;
            double y1 = (START_OFFSET + u1 * drawLen) * SCROLL_SCALE;

            for (int f = 0; f < FACE_COUNT; f++) {
                Vec3 d = switch (f) {
                    case 0 -> base1.add(base2).normalize();
                    case 1 -> base1.subtract(base2).normalize();
                    case 2 -> base2.subtract(base1).normalize();
                    default -> base1.add(base2).scale(-1.0).normalize();
                };
                Vec3 c0 = p0.add(d.scale(h0 * diode));
                Vec3 c1 = p1.add(d.scale(h1 * diode));
                addVert(pose, buffer, p0, 0f, (float) y0, fade, brightnessBoost((float) u0, age));
                addVert(pose, buffer, c0, 1f, (float) y0, fade, brightnessBoost((float) u0, age));
                addVert(pose, buffer, c1, 1f, (float) y1, fade, brightnessBoost((float) u1, age));
                addVert(pose, buffer, p1, 0f, (float) y1, fade, brightnessBoost((float) u1, age));
            }
        }
    }

    private static void drawSweepRings(PoseStack.Pose pose, VertexConsumer buffer, Vec3 dir, double length,
                                       Vec3 base1, Vec3 base2, int age, float grow) {
        double drawLen = length - START_OFFSET;
        float ringFade = 1f - smoothstep(age / (float) GROW_TICKS);
        for (float t = (float) (START_OFFSET + RING_SPACING * 0.5f); t < length - 1.0; t += RING_SPACING) {
            float u = (float) ((t - START_OFFSET) / drawLen);
            if (u > grow) {
                continue;
            }
            float ringAge = age - u * RING_SWEEP_TICKS;
            if (ringAge < 0f) {
                continue;
            }
            float local = Math.min(1f, ringAge / RING_LOCAL_FADE_TICKS);
            float localFade = local * local * (3f - 2f * local);
            int alpha = Math.round(255 * ringFade * localFade);
            if (alpha <= 0) {
                continue;
            }
            float radius = (RING_MIN_RADIUS + (RING_MAX_RADIUS - RING_MIN_RADIUS) * envelope(u)) * RING_SWEEP_BOOST;
            Vec3 center = dir.scale(t);
            emitTube(pose, buffer, center, dir, base1, base2, radius, alpha, 255, 255, 255);
        }
    }

    private static void drawMuzzleFlash(PoseStack.Pose pose, VertexConsumer buffer,
                                        Vec3 dir, Vec3 base1, Vec3 base2, int age) {
        if (age >= MUZZLE_FLASH_TICKS) {
            return;
        }
        float p = age / (float) MUZZLE_FLASH_TICKS;
        int alpha = Math.round(255 * (1f - p) * 0.9f);
        if (alpha <= 0) {
            return;
        }
        float expand = easeOut(p);
        Vec3 center = dir.scale(START_OFFSET * 0.55f);
        float r1 = MUZZLE_RADIUS * (0.25f + 0.75f * expand);
        float r2 = r1 * 0.45f;
        int r = 255, g = 245, b = 210;
        if (age < 2) {
            emitTube(pose, buffer, center, dir, base1, base2, r1, alpha, r, g, b);
        }
        emitTube(pose, buffer, center, dir, base1, base2, r2, alpha, 255, 255, 255);
    }

    private static void drawImpactRing(PoseStack.Pose pose, VertexConsumer buffer,
                                       Vec3 dir, double length, Vec3 base1, Vec3 base2, int age) {
        int elapsed = age - GROW_TICKS;
        if (elapsed >= IMPACT_RING_TICKS) {
            return;
        }
        float p = elapsed / (float) IMPACT_RING_TICKS;
        float radius = 0.3f + (RING_MAX_RADIUS * 2.4f) * easeOut(p);
        int alpha = Math.round(255 * (1f - p) * 0.85f);
        if (alpha <= 0) {
            return;
        }
        float alpha2 = Math.round(alpha * 0.7f);
        Vec3 center = dir.scale(length - 0.35);
        emitTube(pose, buffer, center, dir, base1, base2, radius, alpha, 255, 230, 170);
        emitTube(pose, buffer, center, dir, base1, base2, radius * 0.55f, Math.round(alpha2), 255, 255, 255);
    }

    private static float hash(float x) {
        x = (float) Math.sin(x * 12.9898 + 78.233) * 43758.5453f;
        return x - (float) Math.floor(x);
    }

    private static void drawEmbers(PoseStack.Pose pose, VertexConsumer buffer, Vec3 dir, double length,
                                   Vec3 base1, Vec3 base2, ShotBeam beam, int age, float fade) {
        int elapsed = age - GROW_TICKS;
        float expose = smoothstep(Math.min(1f, elapsed / (float) EMBER_EXPOSE_TICKS));
        float die = 1f - smoothstep((age - DISSIPATE_START) / (float) (LIFETIME_TICKS - DISSIPATE_START));
        float strength = expose * die;
        if (strength <= 0.01f) {
            return;
        }
        double drawLen = length - START_OFFSET;
        float seed = hash((float) beam.start().x * 0.31f + (float) beam.start().z * 0.17f);
        for (int k = 0; k < EMBER_COUNT; k++) {
            float flicker = 0.55f + 0.45f * (float) Math.sin(age * 9f + k * 2.3f + seed * 17f);
            int alpha = Math.round(255 * EMBER_ALPHA * strength * flicker);
            if (alpha <= 0) {
                continue;
            }
            float u = 0.05f + 0.9f * (hash(k * 0.37f + seed) + age * 0.0006f) % 1f;
            float az = hash(k * 0.618f + seed * 1.7f) * (float) Math.PI * 2f;
            float half = (float) (beam.farWidth() * 0.5f * envelope(u));
            float radius = half * (1.15f + 0.25f * hash(k * 0.29f + 0.5f));
            float drift = EMBER_DRIFT * elapsed;
            float drop = (float) Math.min(2.0, EMBER_DROP * elapsed * elapsed);
            Vec3 base = dir.scale(START_OFFSET + u * drawLen)
                .add(base1.scale((radius + drift) * (float) Math.cos(az)))
                .add(base2.scale((radius + drift) * (float) Math.sin(az)))
                .add(0, -drop, 0);
            float size = EMBER_SIZE * (0.8f + 0.4f * hash(k * 0.41f));
            emitPoint(pose, buffer, base, size, alpha, 255, 224, 160);
        }
    }

    private static void emitTube(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, Vec3 dir,
                                 Vec3 base1, Vec3 base2, float radius,
                                 int alpha, int r, int g, int b) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double th0 = Math.PI * 2 * i / RING_SEGMENTS;
            double th1 = Math.PI * 2 * (i + 1) / RING_SEGMENTS;
            float cos0 = (float) Math.cos(th0);
            float sin0 = (float) Math.sin(th0);
            float cos1 = (float) Math.cos(th1);
            float sin1 = (float) Math.sin(th1);
            Vec3 p0 = center.add(base1.scale(radius * cos0)).add(base2.scale(radius * sin0));
            Vec3 p1 = center.add(base1.scale(radius * cos1)).add(base2.scale(radius * sin1));
            Vec3 tangent0 = base1.scale(-sin0).add(base2.scale(cos0)).normalize();
            Vec3 tangent1 = base1.scale(-sin1).add(base2.scale(cos1)).normalize();
            for (int j = 0; j < RING_TUBE_SEGMENTS; j++) {
                double ph0 = Math.PI * 2 * j / RING_TUBE_SEGMENTS;
                double ph1 = Math.PI * 2 * (j + 1) / RING_TUBE_SEGMENTS;
                float cosA = (float) Math.cos(ph0) * RING_TUBE_RADIUS;
                float sinA = (float) Math.sin(ph0) * RING_TUBE_RADIUS;
                float cosB = (float) Math.cos(ph1) * RING_TUBE_RADIUS;
                float sinB = (float) Math.sin(ph1) * RING_TUBE_RADIUS;
                addVert(pose, buffer,
                    p0.add(tangent0.scale(cosA)).add(dir.scale(sinA)), 0f, 0f, alpha, r, g, b);
                addVert(pose, buffer,
                    p0.add(tangent0.scale(cosB)).add(dir.scale(sinB)), 0f, 0f, alpha, r, g, b);
                addVert(pose, buffer,
                    p1.add(tangent1.scale(cosB)).add(dir.scale(sinB)), 0f, 0f, alpha, r, g, b);
                addVert(pose, buffer,
                    p1.add(tangent1.scale(cosA)).add(dir.scale(sinA)), 0f, 0f, alpha, r, g, b);
            }
        }
    }

    private static void emitPoint(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center,
                                  float size, int alpha, int r, int g, int b) {
        for (int i = 0; i < 3; i++) {
            double a0 = Math.PI * 2 * i / 3;
            double a1 = Math.PI * 2 * (i + 1) / 3;
            Vec3 p0 = center.add(size * (float) Math.cos(a0), size * (float) Math.sin(a0), 0);
            Vec3 p1 = center.add(size * (float) Math.cos(a1), size * (float) Math.sin(a1), 0);
            addVert(pose, buffer, center, 0f, 0f, alpha, r, g, b);
            addVert(pose, buffer, p0, 0f, 0f, alpha, r, g, b);
            addVert(pose, buffer, p1, 0f, 0f, alpha, r, g, b);
            addVert(pose, buffer, center, 0f, 0f, alpha, r, g, b);
        }
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static void addVert(PoseStack.Pose pose, VertexConsumer buffer, Vec3 pos,
                                float radial, float u, float brightness, float fade) {
        float w = 1f - Math.clamp(radial, 0f, 1f);
        int r = Math.min(255, Math.round(lerp(HALO_R, 255, w) * brightness));
        int g = Math.min(255, Math.round(lerp(HALO_G, 255, w) * brightness));
        int b = Math.min(255, Math.round(lerp(HALO_B, 255, w) * brightness));
        int alpha = Math.round(255 * BOX_ALPHA * fade);
        addVert(pose, buffer, pos, radial, u, alpha, r, g, b);
    }

    private static void addVert(PoseStack.Pose pose, VertexConsumer buffer, Vec3 pos,
                                float radial, float u, int alpha, int r, int g, int b) {
        int color = (alpha << 24) | (r << 16) | (g << 8) | b;
        buffer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(color).setUv(radial, u).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
    }
}