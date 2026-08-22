package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.multyfora.modjam.modjam;

import javax.annotation.Nullable;

public class CrackedQuartzBlock extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 1, 14);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final int LOOP_END = 4;
    private static final int FULL_POOL_SIZE = 14;
    private static final int STANDALONE_START = 9;

    public CrackedQuartzBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(VARIANT, 1)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        int variant = pickVariant(context.getLevel(), context.getClickedPos().below());
        return this.defaultBlockState()
            .setValue(VARIANT, variant)
            .setValue(FACING, facing);
    }

    private static int pickVariant(Level level, BlockPos belowPos) {
        RandomSource random = level.getRandom();
        BlockState below = level.getBlockState(belowPos);
        if (below.getBlock() instanceof CrackedQuartzBlock) {
            int belowVariant = below.getValue(VARIANT);
            if (belowVariant <= LOOP_END) {
                if (belowVariant == LOOP_END) {
                    return belowVariant + LOOP_END;
                }
                boolean stepUp = random.nextBoolean();
                return stepUp ? belowVariant + 1 : belowVariant + LOOP_END;
            }
        }
        int standaloneCount = FULL_POOL_SIZE - STANDALONE_START + 1;
        int roll = random.nextInt(LOOP_END + standaloneCount);
        return roll < LOOP_END ? roll + 1 : STANDALONE_START + (roll - LOOP_END);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            BlockState below = serverLevel.getBlockState(pos.below());
            boolean continued = below.getBlock() instanceof CrackedQuartzBlock
                && below.getValue(VARIANT) <= LOOP_END;
            modjam.LOGGER.info("Cracked quartz placed at {}: variant {} ({}), facing {}, {}",
                pos.toShortString(),
                state.getValue(VARIANT),
                continued ? "crack step +1/+4 from " + below.getValue(VARIANT) : "standalone pool",
                state.getValue(FACING),
                below.getBlock() instanceof CrackedQuartzBlock
                    ? "below=" + below.getValue(VARIANT)
                    : "no cracked quartz below");
        }
    }
}
