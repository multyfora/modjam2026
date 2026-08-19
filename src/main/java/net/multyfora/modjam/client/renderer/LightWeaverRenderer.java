package net.multyfora.modjam.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.multyfora.modjam.world.entity.LightWeaverEntity;

public class LightWeaverRenderer extends GeoEntityRenderer<LightWeaverEntity, LightWeaverRenderState> {

    private static final float ORBIT_RADIUS = 0.5F;
    private static final float ORBIT_Y = 0.45F;
    private static final float ORBIT_BOB = 0.4F;
    private static final float ORBIT_SPEED = 0.05F;
    private static final float ORBIT_SCALE = 0.55F;
    private static final int MAX_ORBIT_ITEMS = 5;

    private long lastDebugLog;

    private final ItemModelResolver itemModelResolver;

    public LightWeaverRenderer(EntityRendererProvider.Context context) {
        super(context, new LightWeaverModel());
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public LightWeaverRenderState createRenderState(LightWeaverEntity entity, Void relatedObject) {
        return new LightWeaverRenderState();
    }

    @Override
    public void extractRenderState(LightWeaverEntity entity, LightWeaverRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var held = entity.getHeldItem();
        if (held.isEmpty()) {
            state.orbitCount = 0;
        } else {
            itemModelResolver.updateForNonLiving(state.orbitItem, held, ItemDisplayContext.GROUND, entity);
            state.orbitCount = Math.min(held.getCount(), MAX_ORBIT_ITEMS);
        }
        var paper = entity.getPendingPaper();
        if (paper.isEmpty()) {
            state.paperCount = 0;
        } else {
            itemModelResolver.updateForNonLiving(state.orbitPaper, paper, ItemDisplayContext.GROUND, entity);
            state.paperCount = 1;
        }
        long now = System.nanoTime();
        if (now - lastDebugLog > 1_000_000_000L) {
            lastDebugLog = now;
            net.multyfora.modjam.modjam.LOGGER.info("DBG client orbit held={} count={} empty={} paper={} paperEmpty={}",
                    held, held.getCount(), state.orbitItem.isEmpty(), paper, state.orbitPaper.isEmpty());
        }
    }

    @Override
    public void submit(LightWeaverRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState camera) {
        super.submit(state, poseStack, renderTasks, camera);

        int total = state.orbitCount + state.paperCount;
        if (total <= 0) return;

        float time = state.ageInTicks * ORBIT_SPEED;
        int index = 0;
        if (state.paperCount > 0) {
            submitOrbit(state.orbitPaper, index++, total, time, state, poseStack, renderTasks);
        }
        for (int i = 0; i < state.orbitCount; i++) {
            submitOrbit(state.orbitItem, index++, total, time, state, poseStack, renderTasks);
        }
    }

    private void submitOrbit(ItemStackRenderState item, int index, int total, float time, LightWeaverRenderState state,
                             PoseStack poseStack, SubmitNodeCollector renderTasks) {
        float angle = time + (float) (index * (Math.PI * 2.0) / total);
        poseStack.pushPose();
        poseStack.translate(
                Math.cos(angle) * ORBIT_RADIUS,
                ORBIT_Y + (Math.sin(time * 2.0 + index) * 0.5f + 0.5f) * ORBIT_BOB,
                Math.sin(angle) * ORBIT_RADIUS);
        poseStack.mulPose(Axis.YP.rotation(angle + (float) Math.PI / 2));
        poseStack.scale(ORBIT_SCALE, ORBIT_SCALE, ORBIT_SCALE);
        item.submit(poseStack, renderTasks, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}