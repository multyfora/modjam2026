package net.multyfora.modjam.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.modjam.block.SoulLightBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TorchBlock.class)
public abstract class TorchBlockMixin implements EntityBlock {
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getBlock() != Blocks.SOUL_TORCH) return null;
        return new SoulLightBlockEntity(pos, state);
    }
}
