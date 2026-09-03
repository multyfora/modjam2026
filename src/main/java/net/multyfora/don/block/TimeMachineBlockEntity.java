package net.multyfora.don.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
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
import net.multyfora.don.light.TunableLightSource;
import net.multyfora.don.don;

public class TimeMachineBlockEntity extends BlockEntity implements TunableLightSource, GeoBlockEntity {
    public static final double MIN_MYSTICAL = 0.0;
    public static final double MAX_MYSTICAL = 1000.0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation POWER_UP = RawAnimation.begin()
            .then("increase_power", LoopType.PLAY_ONCE)
            .thenLoop("high_power_rotate");
    private static final RawAnimation POWER_DOWN = RawAnimation.begin()
            .then("decrease_power", LoopType.PLAY_ONCE)
            .thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private double mystical = 0.0;
    private boolean wasHigh = false;

    public TimeMachineBlockEntity(BlockPos pos, BlockState state) {
        super(don.TIME_MACHINE_BLOCK_ENTITY.get(), pos, state);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", state -> {
            boolean high = mystical > 5.0;
            if (high) {
                wasHigh = true;
                return state.setAndContinue(POWER_UP);
            }
            if (wasHigh) {
                return state.setAndContinue(POWER_DOWN);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("mystical", mystical);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mystical = Math.clamp(input.getDoubleOr("mystical", 0.0), MIN_MYSTICAL, MAX_MYSTICAL);
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
