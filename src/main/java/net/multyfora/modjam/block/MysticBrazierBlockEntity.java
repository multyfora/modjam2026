package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.multyfora.modjam.modjam;

public class MysticBrazierBlockEntity extends BlockEntity {
    public static final int MAX_CHARGES = 3;

    private int charges = 0;

    public MysticBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(modjam.MYSTIC_BRAZIER_BLOCK_ENTITY.get(), pos, state);
    }

    public int getCharges() {
        return charges;
    }

    public boolean addCharge() {
        if (charges >= MAX_CHARGES || getLevel() == null) return false;
        charges++;
        setChanged();
        getLevel().setBlock(worldPosition, getBlockState().setValue(MysticBrazierBlock.LIT, true), 3);
        return true;
    }

    public boolean useCharge() {
        if (charges <= 0 || getLevel() == null) return false;
        charges--;
        setChanged();
        if (charges <= 0) {
            getLevel().setBlock(worldPosition, getBlockState().setValue(MysticBrazierBlock.LIT, false), 3);
        }
        return true;
    }

    public void snuff() {
        charges = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charges", charges);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("charges", 0);
    }
}
