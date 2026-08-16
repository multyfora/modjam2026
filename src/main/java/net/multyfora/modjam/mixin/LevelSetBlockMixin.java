package net.multyfora.modjam.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.modjam.block.CrystalTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    @Unique
    private static final ThreadLocal<BlockState> modjam$previousState = new ThreadLocal<>();

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void modjam$capturePrevious(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        modjam$previousState.set(((Level) (Object) this).getBlockState(pos));
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void modjam$trackChange(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        BlockState previous = modjam$previousState.get();
        modjam$previousState.remove();
        if (cir.getReturnValueZ()) {
            CrystalTracker.notifyBlockChanged((Level) (Object) this, pos, previous, blockState);
        }
    }
}