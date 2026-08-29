package net.multyfora.don.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.multyfora.don.don;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.util.ArrayList;
import java.util.List;

public class MonocleWornLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final StandaloneModelKey<QuadCollection> MODEL_KEY = new StandaloneModelKey<>(
        () -> Identifier.fromNamespaceAndPath(don.MODID, "monocle_worn").toString());

    private static final float PIVOT_X = 8f / 16f;
    private static final float PIVOT_Y = 24f / 16f;
    private static final float PIVOT_Z = 8f / 16f;

    private static final float OFFSET_X = 0f;   // 2 px toward the player's right
    private static final float OFFSET_Y = 0.0f;
    private static final float OFFSET_Z = 0f;  // 2 px forward

    public MonocleWornLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisibleToPlayer || !state.headEquipment.is(don.MYSTICAL_MONOCLE.get())) {
            return;
        }

        QuadCollection quads = Minecraft.getInstance().getModelManager().getStandaloneModel(MODEL_KEY);
        if (quads == null) {
            return;
        }

        List<BakedQuad> all = new ArrayList<>(quads.getQuads(null));
        for (Direction direction : Direction.values()) {
            all.addAll(quads.getQuads(direction));
        }
        if (all.isEmpty()) {
            return;
        }

        PlayerModel model = this.getParentModel();
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        model.translateToHead(poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(-PIVOT_X + OFFSET_X, -PIVOT_Y + OFFSET_Y, -PIVOT_Z + OFFSET_Z);

        collector.submitItem(poseStack, ItemDisplayContext.GROUND, lightCoords,
            OverlayTexture.NO_OVERLAY, state.outlineColor, new int[0], List.copyOf(all),
            ItemStackRenderState.FoilType.NONE);
        poseStack.popPose();
    }
}
