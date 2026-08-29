package net.multyfora.don.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.don;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrightestVisitationManager {
    private static final BrightestVisitationManager INSTANCE = new BrightestVisitationManager();
    private static final float MOVE_DURATION = 22f;
    private static final float APPEAR_DURATION = 10f;
    private static final float LEAVE_DURATION = 24f;
    private static final Vec3 LEAVE_TARGET = new Vec3(0.0, -2.6, -6.5);

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
    private boolean leaving;
    private float leaveTicks;
    private Vec3 leaveFrom = Vec3.ZERO;
    private float leaveFromScale = 1f;

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
        leaving = false;
        leaveTicks = 0f;
    }

    public void end() {
        if (active && !leaving) {
            leaving = true;
            leaveTicks = 0f;
            leaveFrom = shownPos(0f);
            leaveFromScale = scaleForDepth(leaveFrom);
        }
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
        if (leaving) {
            leaveTicks++;
            if (leaveTicks >= LEAVE_DURATION) {
                active = false;
                leaving = false;
            }
            return;
        }
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

        PoseStack poseStack = new PoseStack();

        if (leaving) {
            float p = Mth.clamp((leaveTicks + partial) / LEAVE_DURATION, 0f, 1f);
            float eased = p * p;
            Vec3 pos = leaveFrom.lerp(LEAVE_TARGET, eased);
            float scale = leaveFromScale * (1f - eased);
            poseStack.translate((float) pos.x, (float) pos.y, (float) pos.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(p * 420f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(p * 50f));
            poseStack.mulPose(Axis.XP.rotationDegrees(p * -30f));
            poseStack.scale(scale, scale, scale);
            renderBrightest(gameRenderer, submitNodeCollector, poseStack);
            return;
        }

        Vec3 pos = shownPos(partial);
        float glide = 1f - Mth.clamp((moveProgress + partial) / MOVE_DURATION, 0f, 1f);
        float glideSway = (float) Math.sin(Mth.clamp((moveProgress + partial) / MOVE_DURATION, 0f, 1f) * Math.PI);

        float depthScale = scaleForDepth(pos);
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

        poseStack.translate((float) pos.x, (float) pos.y, (float) pos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(scale, scale, scale);
        renderBrightest(gameRenderer, submitNodeCollector, poseStack);
    }

    private void renderBrightest(GameRenderer gameRenderer, SubmitNodeCollector submitNodeCollector, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        gameRenderer.itemInHandRenderer.renderItem(
            mc.player,
            don.BRIGHTEST.get().getDefaultInstance(),
            ItemDisplayContext.NONE,
            poseStack,
            submitNodeCollector,
            15728880
        );
    }

    private static float scaleForDepth(Vec3 pos) {
        float depth = Math.max(1.5f, (float) -pos.z);
        return Mth.clamp(2.2f / depth, 0.5f, 2.2f);
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