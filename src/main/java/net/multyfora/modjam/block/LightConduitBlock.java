package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.multyfora.modjam.modjam;

public class LightConduitBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CORE = Block.box(5.0, 2.0, 5.0, 11.0, 8.0, 11.0);
    private static final VoxelShape ARM_NORTH = Block.box(6.0, 3.0, 0.0, 10.0, 7.0, 5.0);
    private static final VoxelShape ARM_SOUTH = Block.box(6.0, 3.0, 11.0, 10.0, 7.0, 16.0);
    private static final VoxelShape ARM_EAST = Block.box(11.0, 3.0, 6.0, 16.0, 7.0, 10.0);
    private static final VoxelShape ARM_WEST = Block.box(0.0, 3.0, 6.0, 5.0, 7.0, 10.0);
    private static final VoxelShape ARM_UP = Block.box(6.0, 8.0, 6.0, 10.0, 16.0, 10.0);
    private static final VoxelShape ARM_DOWN = Block.box(6.0, 0.0, 6.0, 10.0, 2.0, 10.0);

    public LightConduitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(POWERED, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    private boolean connectsTo(BlockState neighbor) {
        return neighbor.is(modjam.LIGHT_CONDUIT_BLOCK.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState().setValue(POWERED, false);
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighbor = level.getBlockState(neighborPos);
            boolean connected = connectsTo(neighbor);
            state = state.setValue(propertyFor(dir), connected);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        boolean connected = connectsTo(neighborState);
        return state.setValue(propertyFor(direction), connected);
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_UP);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, ARM_DOWN);
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightConduitBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof LightConduitBlockEntity conduit) LightConduitBlockEntity.tick(lvl, pos, st, conduit);
        };
    }
}
