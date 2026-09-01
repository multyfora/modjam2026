package net.multyfora.don.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.multyfora.don.lightweaver.LightWeaverShapes;
import net.multyfora.don.world.entity.WeaverGlyphEntity;

public class WeaverGlyphRenderer extends EntityRenderer<WeaverGlyphEntity, WeaverGlyphRenderer.State> {
    private static final int GLYPH_COLOR = 0xFFD8B451;
    private static final int GLYPH_COLOR_LIGHT = 0xFFFFE8A0;
    private static final float CELL = 1.0f / 9.0f;
    private static final float BOX_DEPTH = CELL * 0.45f;

    public WeaverGlyphRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WeaverGlyphEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.facing = entity.getFacing();
        state.shape = entity.getShape();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.shape == null || state.facing == null) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        boolean[] pattern = state.shape.pattern();
        poseStack.pushPose();
        float yaw = (float) Math.toDegrees(Math.atan2(state.facing.getStepX(), state.facing.getStepZ()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(0, 0, -0.001f);
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (!pattern[r * 9 + c]) continue;
                    float x0 = -0.5f + c * CELL;
                    float x1 = x0 + CELL;
                    float y0 = 0.5f - r * CELL;
                    float y1 = y0 - CELL;
                    float z0 = 0.0f;
                    float z1 = -BOX_DEPTH;
                    int topColor = GLYPH_COLOR_LIGHT;
                    int sideColor = GLYPH_COLOR;
                    int bottomColor = 0xFF8A6A20;
                    consumer.addVertex(pose, x0, y0, z0).setColor(topColor);
                    consumer.addVertex(pose, x1, y0, z0).setColor(topColor);
                    consumer.addVertex(pose, x1, y1, z0).setColor(topColor);
                    consumer.addVertex(pose, x0, y1, z0).setColor(topColor);
                    consumer.addVertex(pose, x0, y0, z1).setColor(sideColor);
                    consumer.addVertex(pose, x1, y0, z1).setColor(sideColor);
                    consumer.addVertex(pose, x1, y0, z0).setColor(sideColor);
                    consumer.addVertex(pose, x0, y0, z0).setColor(sideColor);
                    consumer.addVertex(pose, x1, y0, z1).setColor(sideColor);
                    consumer.addVertex(pose, x1, y1, z1).setColor(sideColor);
                    consumer.addVertex(pose, x1, y1, z0).setColor(sideColor);
                    consumer.addVertex(pose, x1, y0, z0).setColor(sideColor);
                    consumer.addVertex(pose, x1, y1, z1).setColor(sideColor);
                    consumer.addVertex(pose, x0, y1, z1).setColor(sideColor);
                    consumer.addVertex(pose, x0, y1, z0).setColor(sideColor);
                    consumer.addVertex(pose, x1, y1, z0).setColor(sideColor);
                    consumer.addVertex(pose, x0, y1, z1).setColor(sideColor);
                    consumer.addVertex(pose, x0, y0, z1).setColor(sideColor);
                    consumer.addVertex(pose, x0, y0, z0).setColor(sideColor);
                    consumer.addVertex(pose, x0, y1, z0).setColor(sideColor);
                }
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public net.minecraft.core.Direction facing;
        public LightWeaverShapes.WeaverShape shape;
    }
}
