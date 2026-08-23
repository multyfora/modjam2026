package net.multyfora.modjam.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.modjam.block.SoulLightBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {

    @Inject(method = "newBlockEntity", at = @At("HEAD"), cancellable = true)
    private void modjam$soulChargeEntity(BlockPos pos, BlockState state, CallbackInfoReturnable<BlockEntity> cir) {
        if (state.getBlock() == Blocks.SOUL_CAMPFIRE) {
            cir.setReturnValue(new SoulLightBlockEntity(pos, state));
        }
    }
}
