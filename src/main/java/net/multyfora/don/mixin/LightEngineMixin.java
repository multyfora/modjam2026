package net.multyfora.don.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LightEngine;
import net.multyfora.don.light.LightDrainField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin {

    @Inject(method = "getLightValue", at = @At("HEAD"), cancellable = true)
    private void don$drainLightValue(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (LightDrainField.isDrained(pos)) {
            cir.setReturnValue(0);
        }
    }
}