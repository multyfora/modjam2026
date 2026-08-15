package net.multyfora.modjam.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.multyfora.modjam.client.BrightestVisitationManager;
import net.multyfora.modjam.client.ItemActivationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    private SubmitNodeStorage handAndScreenSubmitNodeStorage;

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;submit(ZZFLnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterScreenEffects(DeltaTracker deltaTracker, CallbackInfo ci) {
        ItemActivationManager.getInstance().render(
            (GameRenderer) (Object) this,
            this.handAndScreenSubmitNodeStorage,
            deltaTracker
        );
        BrightestVisitationManager.getInstance().render(
            (GameRenderer) (Object) this,
            this.handAndScreenSubmitNodeStorage,
            deltaTracker
        );
    }
}
