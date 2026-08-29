package net.multyfora.don.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.don.block.AmethystCrystalBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AmethystClusterBlock.class)
public abstract class AmethystClusterMixin implements EntityBlock {
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getBlock() != Blocks.AMETHYST_CLUSTER) return null;
        return new AmethystCrystalBlockEntity(pos, state);
    }
}
