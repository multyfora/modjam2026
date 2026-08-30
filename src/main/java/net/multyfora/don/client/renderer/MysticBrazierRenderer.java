package net.multyfora.don.client.renderer;

import com.geckolib.renderer.GeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.block.MysticBrazierBlock;
import net.multyfora.don.block.MysticBrazierBlockEntity;
import org.joml.Matrix4f;

public class MysticBrazierRenderer extends GeoBlockRenderer<MysticBrazierBlockEntity, MysticBrazierRenderState> {
    private static final SpriteId SOUL_FIRE_SPRITE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.parse("minecraft:block/soul_campfire_fire"));
    private static final RenderType SOUL_FIRE_TYPE = RenderType.create("mystic_soul_fire", RenderSetup.builder(RenderPipelines.CUTOUT_BLOCK).withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS).useLightmap().useOverlay().createRenderSetup());

    public MysticBrazierRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new MysticBrazierModel());
    }

    @Override
    public MysticBrazierRenderState createRenderState() {
        return new MysticBrazierRenderState();
    }

    @Override
    public void extractRenderState(MysticBrazierBlockEntity be, MysticBrazierRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTick, cameraPos, crumblingOverlay);
        boolean lit = false;
        try {
            if (be.getLevel() != null) {
                var bs = be.getLevel().getBlockState(be.getBlockPos());
                if (bs.hasProperty(MysticBrazierBlock.LIT)) lit = bs.getValue(MysticBrazierBlock.LIT);
                else if (be.getBlockState().hasProperty(MysticBrazierBlock.LIT)) lit = be.getBlockState().getValue(MysticBrazierBlock.LIT);
            } else if (be.getBlockState().hasProperty(MysticBrazierBlock.LIT)) {
                lit = be.getBlockState().getValue(MysticBrazierBlock.LIT);
            }
        } catch (Exception ignored) {}
        state.lit = lit;
        state.fireVisible = be.isFireVisible();
    }

    @Override
    public void submit(MysticBrazierRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (!state.fireVisible) return;
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.52f, 0.5f);
        float w = 0.35f;
        float h = 0.45f;
        float y0 = -0.12f;
        float y1 = y0 + h;
        var sprite = Minecraft.getInstance().getAtlasManager().get(SOUL_FIRE_SPRITE);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        collector.submitCustomGeometry(poseStack, SOUL_FIRE_TYPE, (pose, buffer) -> {
            Matrix4f m = pose.pose();
            int light = 0xF000F0;
            doubleQuad(buffer, m, -w, y0, 0, w, y0, 0, w, y1, 0, -w, y1, 0, light, u0, u1, v0, v1, 0, 0, 1);
            doubleQuad(buffer, m, 0, y0, -w, 0, y0, w, 0, y1, w, 0, y1, -w, light, u0, u1, v0, v1, 1, 0, 0);
        });
        poseStack.popPose();
    }

    private static void quad(VertexConsumer buffer, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int light, float u0, float u1, float v0, float v1, float nx, float ny, float nz) {
        buffer.addVertex(m, ax, ay, az).setColor(-1).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(nx, ny, nz);
        buffer.addVertex(m, bx, by, bz).setColor(-1).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(nx, ny, nz);
        buffer.addVertex(m, cx, cy, cz).setColor(-1).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(nx, ny, nz);
        buffer.addVertex(m, dx, dy, dz).setColor(-1).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(nx, ny, nz);
    }

    private static void doubleQuad(VertexConsumer buffer, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int light, float u0, float u1, float v0, float v1, float nx, float ny, float nz) {
        quad(buffer, m, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, light, u0, u1, v0, v1, nx, ny, nz);
        buffer.addVertex(m, dx, dy, dz).setColor(-1).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(-nx, -ny, -nz);
        buffer.addVertex(m, cx, cy, cz).setColor(-1).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(-nx, -ny, -nz);
        buffer.addVertex(m, bx, by, bz).setColor(-1).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(-nx, -ny, -nz);
        buffer.addVertex(m, ax, ay, az).setColor(-1).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(-nx, -ny, -nz);
    }
}
