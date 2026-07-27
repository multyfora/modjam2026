package net.multyfora.modjam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemActivationManager {
    private static final ItemActivationManager INSTANCE = new ItemActivationManager();
    private static final int DURATION = 160;

    private ItemStack activeStack;
    private int elapsedTicks;

    public static ItemActivationManager getInstance() {
        return INSTANCE;
    }

    public void activate(ItemStack stack) {
        this.activeStack = stack.copy();
        this.elapsedTicks = 0;
    }

    public void tick() {
        if (activeStack != null && ++elapsedTicks > DURATION) {
            activeStack = null;
        }
    }

    public void render(GameRenderer gameRenderer, SubmitNodeCollector submitNodeCollector, DeltaTracker deltaTracker) {
        if (activeStack == null || activeStack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        float progress = Math.min(1.0f, (elapsedTicks + partial) / (float) DURATION);

        float xOffset = (float) Math.sin(progress * Math.PI * 4) * 1.2f;

        float z;
        if (progress < 0.125f) {
            float t = progress / 0.125f;
            float easeOut = 1.0f - (1.0f - t) * (1.0f - t);
            z = -3.0f + easeOut * 2.0f;
        } else {
            float t = (progress - 0.125f) / 0.875f;
            z = -1.0f - t * 1.5f;
        }

        float yBob = (float) Math.sin(progress * Math.PI * 6) * 0.2f;

        float scale = 0.55f + (float) Math.sin(progress * Math.PI * 5) * 0.08f;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(xOffset, yBob, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(progress * Math.PI * 4) * 4.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(progress * Math.PI * 3 + 1.0f) * 3.0f));
        poseStack.scale(scale, scale, scale);

        gameRenderer.itemInHandRenderer.renderItem(
            mc.player,
            activeStack,
            ItemDisplayContext.NONE,
            poseStack,
            submitNodeCollector,
            15728880
        );

        if (elapsedTicks > DURATION) {
            activeStack = null;
        }
    }

    public void clear() {
        activeStack = null;
        elapsedTicks = 0;
    }
}
