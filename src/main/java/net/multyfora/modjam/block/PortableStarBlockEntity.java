package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.multyfora.modjam.light.TunableLightSource;
import net.multyfora.modjam.modjam;

public class PortableStarBlockEntity extends BlockEntity implements TunableLightSource {
    public static final double MIN_MYSTICAL = -1000.0;
    public static final double MAX_MYSTICAL = 1000.0;

    private double mystical = 0.0;

    public PortableStarBlockEntity(BlockPos pos, BlockState state) {
        super(modjam.PORTABLE_STAR_BLOCK_ENTITY.get(), pos, state);
    }

    public double getMystical() {
        return mystical;
    }

    public void setMystical(double value) {
        this.mystical = Math.clamp(value, MIN_MYSTICAL, MAX_MYSTICAL);
        setChanged();
        if (getLevel() != null) {
            getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public double tunedMystical() {
        return mystical;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("mystical", mystical);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mystical = input.getDoubleOr("mystical", 0.0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putDouble("mystical", mystical);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
