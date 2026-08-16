package net.multyfora.modjam.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.world.entity.LightWeaverEntity;

public class LightWeaverRenderer extends GeoEntityRenderer<LightWeaverEntity, EntityRenderState> {

    public LightWeaverRenderer(EntityRendererProvider.Context context) {
        super(context, modjam.LIGHT_WEAVER_ENTITY.get());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo info) {
        PoseStack poseStack = info.poseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }
}
