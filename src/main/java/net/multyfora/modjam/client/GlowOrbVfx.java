package net.multyfora.modjam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class GlowOrbVfx {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(modjam.MODID, "textures/vfx/glow_orb.png");

    private static final RenderType RENDER_TYPE = RenderTypes.eyes(TEXTURE);

    private GlowOrbVfx() {
    }

    public static RenderType renderType() {
        return RENDER_TYPE;
    }

    public static void submitCube(SubmitNodeCollector renderTasks, PoseStack poseStack,
                                  float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                  float r, float g, float b, float alpha) {
        renderTasks.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) ->
            drawCuboid(buffer, pose.pose(), minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha));
    }

    private static void drawCuboid(VertexConsumer buffer, Matrix4fc m,
                                   float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                   float r, float g, float b, float a) {
        // down
        quad(buffer, m,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        // up
        quad(buffer, m,
            minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        // north
        quad(buffer, m,
            minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        // south
        quad(buffer, m,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        // west
        quad(buffer, m,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        // east
        quad(buffer, m,
            maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);

        quadR(buffer, m,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        quadR(buffer, m,
            minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        quadR(buffer, m,
            minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        quadR(buffer, m,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        quadR(buffer, m,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        quadR(buffer, m,
            maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
    }

    private static void quad(VertexConsumer buffer, Matrix4fc m,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a) {

        vertex(buffer, m, ax, ay, az, r, g, b, a);
        vertex(buffer, m, bx, by, bz, r, g, b, a);
        vertex(buffer, m, cx, cy, cz, r, g, b, a);
        vertex(buffer, m, dx, dy, dz, r, g, b, a);
    }

    private static void quadR(VertexConsumer buffer, Matrix4fc m,
                              float ax, float ay, float az, float bx, float by, float bz,
                              float cx, float cy, float cz, float dx, float dy, float dz,
                              float r, float g, float b, float a) {
        vertex(buffer, m, dx, dy, dz, r, g, b, a);
        vertex(buffer, m, cx, cy, cz, r, g, b, a);
        vertex(buffer, m, bx, by, bz, r, g, b, a);
        vertex(buffer, m, ax, ay, az, r, g, b, a);
    }

    private static void vertex(VertexConsumer buffer, Matrix4fc matrix,
                               float x, float y, float z,
                               float r, float g, float b, float a) {
        Vector3f pos = matrix.transformPosition(x, y, z, new Vector3f());
        buffer.addVertex(pos.x(), pos.y(), pos.z())
            .setColor(r, g, b, a)
            .setUv(0.5f, 0.5f)
            .setOverlay(0)
            .setLight(0xF000F0)
            .setNormal(0f, 1f, 0f);
    }
}
