package net.multyfora.modjam.client.renderer;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.multyfora.modjam.client.GlowOrbVfx;
import net.multyfora.modjam.item.SealedSingularityItem;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SealedSingularityRenderer extends GeoItemRenderer<SealedSingularityItem> {
    private static final float CUBE_MIN_XZ = -4.5f / 16f;
    private static final float CUBE_MAX_XZ = 4.5f / 16f;
    private static final float CUBE_MIN_Y = -6.5f / 16f;
    private static final float CUBE_MAX_Y = 1.5f / 16f;

    private static final Map<Long, Long> GLOW_START = new HashMap<>();

    public SealedSingularityRenderer() {
        super(new SealedSingularityModel());
        withRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void addPerBoneRender(RenderPassInfo<GeoRenderState> renderPassInfo,
                                         BiConsumer<GeoBone, PerBoneRender<GeoRenderState>> consumer) {
                GeoRenderState state = renderPassInfo.renderState();
                long id = state.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, 0L);
                if (!shouldGlow(state)) {
                    GLOW_START.remove(id);
                    return;
                }
                GLOW_START.computeIfAbsent(id, k -> System.currentTimeMillis());
                getDefaultBakedModel(state).getBone("bone").ifPresent(bone ->
                    consumer.accept(bone, (info, b, renderTasks) -> submitGlowCube(info, renderTasks, id)));
            }
        });
    }

    @Override
    public void captureDefaultRenderState(SealedSingularityItem animatable, GeoItemRenderer.RenderData renderData,
                                          GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, renderData, renderState, partialTick);

        ItemStack stack = renderData.itemStack();
        boolean full = SealedSingularityItem.getCharges(stack) >= SealedSingularityItem.MAX_CHARGES;
        SealedSingularityModel.setCharged(full);

        renderState.addGeckolibData(SealedSingularityItem.CHARGES_DATA,
            SealedSingularityItem.getCharges(stack));
        renderState.addGeckolibData(SealedSingularityItem.MODE_DATA,
            SealedSingularityItem.getMode(stack));

        LivingEntity owner = renderData.itemOwner() != null ? renderData.itemOwner().asLivingEntity() : null;
        boolean using;
        if (owner != null && owner.isUsingItem()) {
            using = owner.getUseItem().is(animatable);
        } else {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            using = player != null && player.isUsingItem() && player.getUseItem().is(animatable);
        }
        renderState.addGeckolibData(SealedSingularityItem.USING_DATA, using);
    }

    private static boolean shouldGlow(GeoRenderState renderState) {
        Boolean using = renderState.getOrDefaultGeckolibData(SealedSingularityItem.USING_DATA, false);
        Integer mode = renderState.getGeckolibData(SealedSingularityItem.MODE_DATA);
        return using && mode != null && mode == SealedSingularityItem.MODE_ALT;
    }

    private static void submitGlowCube(RenderPassInfo<GeoRenderState> renderPassInfo,
                                       SubmitNodeCollector renderTasks, long instanceId) {
        PoseStack pose = renderPassInfo.poseStack();
        pose.pushPose();
        pose.translate(0f, 5f / 16f, 0f);

        float centerY = (CUBE_MIN_Y + CUBE_MAX_Y) * 0.5f;
        float halfXZ = (CUBE_MAX_XZ - CUBE_MIN_XZ) * 0.5f;
        float halfH = (CUBE_MAX_Y - CUBE_MIN_Y) * 0.5f;


        float[] scales = {1.0f, 0.62f, 0.24f};
        float[][] colors = {
            {1.00f, 0.79f, 1.00f},
            {0.84f, 0.69f, 0.87f},
            {1.00f, 1.00f, 1.00f},
        };
        float[] alphas = {0.3f, 0.15f, 0.6f};
        float[][] tumble = {
            {11f, 17f, 23f},
            {19f, 8f, 13f},
            {0f, 0f, 0f},
        };
        float[] fadeInStart = {0.5f, 0.25f, 0.0f};
        float fadeInDuration = 0.3f;

        long now = System.currentTimeMillis();
        float elapsed = (now - GLOW_START.getOrDefault(instanceId, now)) / 1000f;

        float time = (now % 200000L) / 1000f;
        for (int i = 0; i < scales.length; i++) {
            float fade = Mth.clamp((elapsed - fadeInStart[i]) / fadeInDuration, 0f, 1f);
            if (fade <= 0f) {
                continue;
            }

            float s = scales[i];
            float alpha = alphas[i] * fade;
            if (i == 0) {
                alpha *= 0.8f + 0.2f * Mth.sin(time * 6f);
            }

            float[] c = colors[i];
            float[] t = tumble[i];
            pose.pushPose();
            pose.translate(0f, centerY, 0f);
            if (t[0] != 0f || t[1] != 0f || t[2] != 0f) {
                pose.mulPose(Axis.XP.rotationDegrees(time * t[0]));
                pose.mulPose(Axis.YP.rotationDegrees(time * t[1]));
                pose.mulPose(Axis.ZP.rotationDegrees(time * t[2]));
            }
            GlowOrbVfx.submitCube(renderTasks, pose,
                -halfXZ * s, -halfH * s, -halfXZ * s,
                halfXZ * s, halfH * s, halfXZ * s,
                c[0], c[1], c[2], alpha);
            pose.popPose();
        }

        pose.popPose();
    }
}
