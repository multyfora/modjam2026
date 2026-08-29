package net.multyfora.don.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.multyfora.don.light.ForwardingLightSource;
import net.multyfora.don.light.LightEnergy;
import net.multyfora.don.light.LightEnergyManager;
import net.multyfora.don.don;

public class LightConduitBlockEntity extends BlockEntity implements ForwardingLightSource {
    private static final int TICK_INTERVAL = 5;
    private LightEnergy forwarded = LightEnergy.NONE;
    private int tickCounter = 0;

    public LightConduitBlockEntity(BlockPos pos, BlockState state) {
        super(don.LIGHT_CONDUIT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public LightEnergy forwardedEnergy() {
        return forwarded;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LightConduitBlockEntity be) {
        if (++be.tickCounter < TICK_INTERVAL) return;
        be.tickCounter = 0;
        LightEnergy incoming = LightEnergyManager.compute(level, pos);
        LightEnergy next;
        if (!incoming.isPresent() || incoming.mysticalComponent() == 0.0) {
            next = LightEnergy.NONE;
        } else {
            next = incoming;
        }
        if (next.intensity() != be.forwarded.intensity() || next.mysticalComponent() != be.forwarded.mysticalComponent()) {
            be.forwarded = next;
            be.setChanged();
            boolean powered = next.isPresent();
            if (state.getValue(LightConduitBlock.POWERED) != powered) {
                level.setBlock(pos, state.setValue(LightConduitBlock.POWERED, powered), 3);
            } else {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("forwarded_intensity", forwarded.intensity());
        output.putDouble("forwarded_mystical", forwarded.mysticalComponent());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double intensity = input.getDoubleOr("forwarded_intensity", 0.0);
        double mystical = input.getDoubleOr("forwarded_mystical", 0.0);
        forwarded = new LightEnergy(intensity, mystical);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putDouble("forwarded_intensity", forwarded.intensity());
        tag.putDouble("forwarded_mystical", forwarded.mysticalComponent());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
