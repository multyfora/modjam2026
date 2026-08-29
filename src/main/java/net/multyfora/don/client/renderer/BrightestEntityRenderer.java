package net.multyfora.don.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.don;
import net.multyfora.don.world.entity.BrightestEntity;

public class BrightestEntityRenderer extends EntityRenderer<BrightestEntity, BrightestEntityRenderer.BrightestRenderState> {

    private static final int FULL_BRIGHT = 15728880;

    private final ItemModelResolver itemModelResolver;
    private ItemStack displayStack;

    public BrightestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.0f;
    }

    @Override
    public BrightestRenderState createRenderState() {
        return new BrightestRenderState();
    }

    @Override
    public void extractRenderState(BrightestEntity entity, BrightestRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (displayStack == null) {
            displayStack = new ItemStack(don.BRIGHTEST.get());
        }
        itemModelResolver.updateForNonLiving(state.item, displayStack, ItemDisplayContext.NONE, entity);
        Vec3 boxCenter = entity.getBoundingBox().getCenter();
        state.anchorX = (float) (boxCenter.x - entity.getX());
        state.anchorY = (float) (boxCenter.y - entity.getY());
        state.anchorZ = (float) (boxCenter.z - entity.getZ());
    }

    @Override
    public void submit(BrightestRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            var boundingBox = state.item.getModelBoundingBox();
            float modelHeight = (float) boundingBox.getYsize();
            float scale = modelHeight > 0.0f ? BrightestEntity.MODEL_HEIGHT / modelHeight : 1.0f;
            var modelCenter = boundingBox.getCenter();

            poseStack.translate(
                state.anchorX - modelCenter.x * scale,
                state.anchorY - modelCenter.y * scale,
                state.anchorZ - modelCenter.z * scale);
            poseStack.scale(scale, scale, scale);

            state.item.submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static class BrightestRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float anchorX;
        public float anchorY;
        public float anchorZ;
    }
}