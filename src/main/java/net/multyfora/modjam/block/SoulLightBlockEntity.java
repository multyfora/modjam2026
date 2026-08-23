package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.multyfora.modjam.modjam;


public class SoulLightBlockEntity extends BlockEntity {
    public static final int MAX_CHARGES = 1;

    private int charges = MAX_CHARGES;

    public SoulLightBlockEntity(BlockPos pos, BlockState state) {
        super(modjam.SOUL_LIGHT_BLOCK_ENTITY.get(), pos, state);
    }

    public int getCharges() {
        return charges;
    }

    public boolean useLight() {
        if (charges <= 0 || getLevel() == null) return false;
        charges--;
        setChanged();
        if (charges <= 0) {
            BlockState current = getBlockState();
            Block block = current.getBlock();
            Block target;
            if (block == Blocks.SOUL_LANTERN) {
                target = Blocks.LANTERN;
            } else if (block == Blocks.SOUL_TORCH) {
                target = Blocks.TORCH;
            } else {
                target = Blocks.CAMPFIRE;
            }
            getLevel().setBlock(worldPosition, target.withPropertiesOf(current), 3);
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charges", charges);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("charges", MAX_CHARGES);
    }
}
