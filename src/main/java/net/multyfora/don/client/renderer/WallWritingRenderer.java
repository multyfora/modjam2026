package net.multyfora.don.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.multyfora.don.util.WallWritingText;
import net.multyfora.don.world.entity.WallWritingEntity;

import java.util.List;

public class WallWritingRenderer extends EntityRenderer<WallWritingEntity, WallWritingRenderer.State> {
    private static final FontDescription ALT = new FontDescription.Resource(Identifier.parse("minecraft:alt"));
    private static final int MAX_CHARS = 18;
    private static final float SCALE = 0.025f;

    public WallWritingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WallWritingEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.plain = entity.getPlain();
        state.facing = entity.getFacing();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.plain == null || state.plain.isEmpty() || state.facing == null) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        String sga = WallWritingText.toSga(state.plain);
        List<String> lines = WallWritingText.wrap(sga, MAX_CHARS);
        if (lines.isEmpty()) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        Font font = Minecraft.getInstance().font;
        float lineH = font.lineHeight + 1;
        poseStack.pushPose();
        float yaw = (float) Math.toDegrees(Math.atan2(state.facing.getStepX(), state.facing.getStepZ()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(0, 0.15, -0.032);
        poseStack.scale(SCALE, -SCALE, SCALE);
        float totalH = lines.size() * lineH;
        float y0 = -totalH / 2f;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Component comp = Component.literal(line).withStyle(Style.EMPTY.withFont(ALT));
            FormattedCharSequence seq = comp.getVisualOrderText();
            float w = font.width(seq);
            float x = -w / 2f;
            float y = y0 + i * lineH;
            collector.submitText(poseStack, x, y, seq, false, Font.DisplayMode.NORMAL, 0xF000F0, 0xFFE8D8A0, 0, 0);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public String plain = "";
        public net.minecraft.core.Direction facing;
    }
}
