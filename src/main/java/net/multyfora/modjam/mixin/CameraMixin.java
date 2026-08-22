package net.multyfora.modjam.mixin;

import net.minecraft.client.Camera;
import net.multyfora.modjam.client.cutscene.CutsceneClientController;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void modjam$applyCutsceneCamera(float partialTicks, CallbackInfo ci) {
        CutsceneClientController controller = CutsceneClientController.getInstance();
        if (!controller.isActive()) return;
        var state = controller.getCamera(partialTicks);
        if (state == null) return;
        this.setRotation(state.yaw(), state.pitch(), 0.0f);
        this.setPosition(state.pos());
        this.detached = true;
    }
}
