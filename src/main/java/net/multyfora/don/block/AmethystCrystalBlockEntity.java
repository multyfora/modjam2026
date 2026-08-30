package net.multyfora.don.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.multyfora.don.don;


public class AmethystCrystalBlockEntity extends BlockEntity {
    public static final int MAX_CHARGES = 3;

    private int charges = MAX_CHARGES;

    public AmethystCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(don.AMETHYST_CRYSTAL_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.getBlockState(worldPosition).is(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER)) {
            level.removeBlockEntity(worldPosition);
        }
    }

    public int getCharges() {
        return charges;
    }

    public boolean addCharge() {
        if (charges >= MAX_CHARGES || getLevel() == null) return false;
        charges++;
        setChanged();
        return true;
    }

    public void setCharges(int value) {
        charges = Math.clamp(value, 0, MAX_CHARGES);
        setChanged();
    }

    public boolean useLight() {
        if (charges <= 0 || getLevel() == null) return false;
        charges--;
        setChanged();
        if (charges <= 0) {
            BlockState crystalState = don.SINGULARITY_CRYSTAL_BLOCK.get().defaultBlockState()
                    .setValue(SingularityCrystalBlock.PULSE, true);
            getLevel().setBlock(worldPosition, crystalState, 3);
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
