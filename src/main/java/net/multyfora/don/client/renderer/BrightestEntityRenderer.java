package net.multyfora.don.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.multyfora.don.world.entity.BrightestEntity;

public class BrightestEntityRenderer extends GeoEntityRenderer<BrightestEntity, EntityRenderState> {
    public BrightestEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new BrightestModel());
        this.shadowRadius = 0.0f;
    }

    @Override
    public EntityRenderState createRenderState(BrightestEntity entity, Void relatedObject) {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0, BrightestEntity.FLOAT_HEIGHT + 0.15f, 0);
        poseStack.scale(1.5f, 1.5f, 1.5f);
        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }
}
