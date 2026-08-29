package net.multyfora.don.client.godray;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.don;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = don.MODID)
public final class GodrayRenderHandler {

    private static final ContextKey<List<GodraySite>> DATA_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(don.MODID, "godrays"));
    private static final int FULL_BRIGHT = 15728880;

    private static final int PLANE_COUNT = 3;
    private static final int WIDTH_STRIPS = 4;


    private static final float WORLD_UNITS_PER_TILE = 5.0f;

    private static final float HALO_WIDTH_MULT = 2.6f;
    private static final float HALO_ALPHA_MULT = 0.14f;
    private static final float CORE_WIDTH_MULT = 0.6f;

    private static final float SOURCE_END_ATTENUATION = 0.15f;

    private static final float NEAR_FADE_DISTANCE = 3.5f;
    private static final float FAR_FADE_DISTANCE = 9.0f;

    private GodrayRenderHandler() {}

    @SubscribeEvent
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        event.getRenderState().setRenderData(DATA_KEY, GodrayRenderer.activeSites());
    }

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        List<GodraySite> sites = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (sites == null || sites.isEmpty()) return;

        long now = System.currentTimeMillis();
        float time = (now % 1_000_000) / 1000f;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();

        for (GodraySite site : sites) {
            Vec3 targetPos = Vec3.atCenterOf(site.target());
            float distance = (float) targetPos.distanceTo(camera);
            float proximityFade = smoothstep(distance, NEAR_FADE_DISTANCE, FAR_FADE_DISTANCE);
            if (proximityFade <= 0f) continue;

            Vec3 offset = targetPos.subtract(camera);
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);

            for (GodrayBeam beam : site.beams()) {
                float fade = site.fadeMultiplier(beam, now) * proximityFade;
                if (fade <= 0f) continue;
                submitBeam(event, poseStack, beam, fade, time);
            }
            poseStack.popPose();
        }
    }

    private static float smoothstep(float distance, float nearRadius, float farRadius) {
        if (farRadius <= nearRadius) return distance <= nearRadius ? 0f : 1f;
        float t = Math.clamp((distance - nearRadius) / (farRadius - nearRadius), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static void submitBeam(SubmitCustomGeometryEvent event, PoseStack poseStack, GodrayBeam beam, float siteFade, float time) {
        int r = (beam.color() >> 16) & 0xFF;
        int g = (beam.color() >> 8) & 0xFF;
        int b = beam.color() & 0xFF;

        event.getSubmitNodeCollector().submitCustomGeometry(
                poseStack,
                GodrayPipelines.BEAM,
                (pose, buffer) -> drawBeam(pose, buffer, beam, r, g, b, siteFade, time));
    }

    private static void drawBeam(PoseStack.Pose pose, VertexConsumer buffer, GodrayBeam beam,
                                 int r, int g, int b, float siteFade, float time) {
        Vec3 dir = beam.sourceDirection();
        Vec3 origin = dir.scale(beam.length());
        Vec3 tip = Vec3.ZERO;

        Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 base1 = dir.cross(up).normalize();
        Vec3 base2 = dir.cross(base1).normalize();

        double wobble = Math.sin(time * 0.35 + beam.flickerPhase() * 3f) * 0.05;

        for (int i = 0; i < PLANE_COUNT * 2; i++) {
            double angle = Math.PI * i / PLANE_COUNT + wobble;
            Vec3 side = base1.scale(Math.cos(angle)).add(base2.scale(Math.sin(angle))).normalize();

            drawPlane(pose, buffer, origin, tip, side,
                    beam.topWidth() * HALO_WIDTH_MULT, beam.bottomWidth() * HALO_WIDTH_MULT * 1.4f,
                    r, g, b, siteFade * HALO_ALPHA_MULT);

            drawPlane(pose, buffer, origin, tip, side,
                    beam.topWidth() * CORE_WIDTH_MULT, beam.bottomWidth(),
                    r, g, b, siteFade);
        }
    }

    private static void drawPlane(PoseStack.Pose pose, VertexConsumer buffer, Vec3 origin, Vec3 tip, Vec3 side,
                                  float topWidth, float bottomWidth, int r, int g, int b, float fade) {
        float vSpan = 1.0f / WORLD_UNITS_PER_TILE;

        int cr = Math.round(r * fade);
        int cg = Math.round(g * fade);
        int cb = Math.round(b * fade);

        int farR = Math.round(cr * SOURCE_END_ATTENUATION);
        int farG = Math.round(cg * SOURCE_END_ATTENUATION);
        int farB = Math.round(cb * SOURCE_END_ATTENUATION);

        for (int half = -1; half <= 1; half += 2) {
            for (int s = 0; s < WIDTH_STRIPS; s++) {
                float t0 = (float) s / WIDTH_STRIPS;
                float t1 = (float) (s + 1) / WIDTH_STRIPS;

                float rTop = t0;
                float rBot = t1;

                Vec3 o1 = origin.add(side.scale(topWidth * half * t0));
                Vec3 o2 = origin.add(side.scale(topWidth * half * t1));
                Vec3 t1p = tip.add(side.scale(bottomWidth * half * t0));
                Vec3 t2p = tip.add(side.scale(bottomWidth * half * t1));

                buffer.addVertex(pose, (float) o1.x, (float) o1.y, (float) o1.z)
                        .setColor((255 << 24) | (farR << 16) | (farG << 8) | farB)
                        .setUv(rTop, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
                buffer.addVertex(pose, (float) o2.x, (float) o2.y, (float) o2.z)
                        .setColor((255 << 24) | (farR << 16) | (farG << 8) | farB)
                        .setUv(rBot, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
                buffer.addVertex(pose, (float) t2p.x, (float) t2p.y, (float) t2p.z)
                        .setColor((255 << 24) | (cr << 16) | (cg << 8) | cb)
                        .setUv(rBot, vSpan).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
                buffer.addVertex(pose, (float) t1p.x, (float) t1p.y, (float) t1p.z)
                        .setColor((255 << 24) | (cr << 16) | (cg << 8) | cb)
                        .setUv(rTop, vSpan).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
            }
        }
    }
}