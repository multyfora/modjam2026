package net.multyfora.modjam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.modjam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrightestVisitationManager {
    private static final BrightestVisitationManager INSTANCE = new BrightestVisitationManager();
    private static final float MOVE_DURATION = 22f;
    private static final float APPEAR_DURATION = 10f;

    private static final List<Vec3> ANCHORS = List.of(
        new Vec3(1.6, -0.1, -2.0),
        new Vec3(0.3, 0.8, -3.3),
        new Vec3(-1.5, -0.2, -2.2),
        new Vec3(0.0, -0.5, -3.3),
        new Vec3(1.3, 0.5, -3.0),
        new Vec3(-0.9, 0.3, -2.3),
        new Vec3(0.8, -0.1, -1.6),
        new Vec3(-1.6, 0.7, -3.1),
        new Vec3(0.5, 0.9, -2.7),
        new Vec3(-0.3, -0.2, -1.9)
    );

    private boolean active;
    private int elapsedTicks;
    private List<Vec3> route = List.of();
    private int routeIndex;
    private Vec3 current;
    private Vec3 target;
    private float moveProgress = 1f;
    private float appearTicks;

    public static BrightestVisitationManager getInstance() {
        return INSTANCE;
    }

    public void start(int lineCount) {
        List<Vec3> shuffled = new ArrayList<>(ANCHORS);
        Collections.shuffle(shuffled);
        route = List.copyOf(shuffled);
        routeIndex = 0;
        current = route.get(0);
        target = current;
        moveProgress = 1f;
        appearTicks = 0f;
        active = true;
        elapsedTicks = 0;
    }

    public void end() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void onLineChange() {
        if (!active || route.isEmpty()) return;
        current = shownPos(0f);
        for (int i = 0; i < route.size(); i++) {
            Vec3 candidate = route.get((routeIndex + 1 + i) % route.size());
            if (!candidate.equals(target)) {
                routeIndex = (routeIndex + 1 + i) % route.size();
                target = candidate;
                moveProgress = 0f;
                return;
            }
        }
    }

    public void tick() {
        if (!active) return;
        elapsedTicks++;
        appearTicks++;
        if (moveProgress < MOVE_DURATION) {
            moveProgress++;
        }
    }

    public void render(GameRenderer gameRenderer, SubmitNodeCollector submitNodeCollector, DeltaTracker deltaTracker) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        float t = (elapsedTicks + partial) / 20.0f;

        Vec3 pos = shownPos(partial);
        float glide = 1f - Mth.clamp((moveProgress + partial) / MOVE_DURATION, 0f, 1f);
        float glideSway = (float) Math.sin(Mth.clamp((moveProgress + partial) / MOVE_DURATION, 0f, 1f) * Math.PI);

        float depth = Math.max(1.5f, (float) -pos.z);
        float depthScale = Mth.clamp(2.2f / depth, 0.5f, 2.2f);
        float appear = Mth.clamp((appearTicks + partial) / APPEAR_DURATION, 0f, 1f);
        float appearEase = 1f;
        if (appear < 1f) {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            appearEase = 1f + c3 * (appear - 1f) * (appear - 1f) * (appear - 1f) + c1 * (appear - 1f) * (appear - 1f);
        }
        float scale = depthScale * appearEase * (1.0f + (float) Math.sin(t * 2.2f) * 0.05f + glide * 0.10f + glideSway * 0.04f);
        float yaw = (float) Math.sin(t * 0.8f) * 16.0f + (float) (target.x - current.x) * 10.0f * glide;
        float roll = (float) Math.sin(t * 0.9f) * 6.0f + glideSway * 7.0f;
        float pitch = (float) Math.sin(t * 1.1f) * 3.0f + (float) (target.y - current.y) * 6.0f * glide;

        PoseStack poseStack = new PoseStack();
        poseStack.translate((float) pos.x, (float) pos.y, (float) pos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(scale, scale, scale);

        gameRenderer.itemInHandRenderer.renderItem(
            mc.player,
            modjam.BRIGHTEST.get().getDefaultInstance(),
            ItemDisplayContext.NONE,
            poseStack,
            submitNodeCollector,
            15728880
        );
    }

    private Vec3 shownPos(float partial) {
        float p = Mth.clamp((moveProgress + partial) / MOVE_DURATION, 0f, 1f);
        float eased = p < 0.5f ? 8f * p * p * p * p : 1f - 8f * (1f - p) * (1f - p) * (1f - p) * (1f - p);
        float z = (float) Mth.lerp(eased, current.z, target.z);
        float y = Math.max((float) Mth.lerp(eased, current.y, target.y), -0.25f * -z + 0.35f);
        float x = (float) Mth.lerp(eased, current.x, target.x);
        return new Vec3(x, y, z);
    }
}