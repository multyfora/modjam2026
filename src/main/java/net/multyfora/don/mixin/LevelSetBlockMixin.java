package net.multyfora.don.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.don.block.CrystalTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;

@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    @Unique
    private static final ThreadLocal<ArrayDeque<BlockState>> don$previousStates = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void don$capturePrevious(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        don$previousStates.get().push(((Level) (Object) this).getBlockState(pos));
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void don$trackChange(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        ArrayDeque<BlockState> stack = don$previousStates.get();
        BlockState previous = stack.poll();
        if (stack.isEmpty()) {
            don$previousStates.remove();
        }
        if (cir.getReturnValueZ() && previous != null) {
            CrystalTracker.notifyBlockChanged((Level) (Object) this, pos, previous, blockState);
        }
    }
}