package net.multyfora.don.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.multyfora.don.world.entity.DisplayBlockEntity;

public class DisplayBlockRenderer extends EntityRenderer<DisplayBlockEntity, DisplayBlockRenderer.State> {
    private static final int FULL_BRIGHT = 15728880;
    private final ItemModelResolver itemModelResolver;

    public DisplayBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DisplayBlockEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = (entity.tickCount + partialTicks) * 1.2f;
        state.itemStack = entity.getDisplayItem().copy();
        if (!state.itemStack.isEmpty()) {
            itemModelResolver.updateForNonLiving(state.item, state.itemStack, ItemDisplayContext.FIXED, entity);
        } else {
            state.item.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0, 0.5, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
            state.item.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public net.minecraft.world.item.ItemStack itemStack = net.minecraft.world.item.ItemStack.EMPTY;
        public float yRot;
    }
}
