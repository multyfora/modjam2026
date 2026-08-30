package net.multyfora.don.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
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
import net.multyfora.don.don;

public class MysticBrazierBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int MAX_CHARGES = 3;

    private static final RawAnimation LIGHT_UP = RawAnimation.begin().then("light_upo", LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation LIGHT_DOWN = RawAnimation.begin().then("light_down", LoopType.HOLD_ON_LAST_FRAME);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int charges = 0;
    private boolean wasLit = false;
    private boolean reversing = false;
    private int litTicks = 0;
    private boolean fireVisible = false;

    public MysticBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(don.MYSTIC_BRAZIER_BLOCK_ENTITY.get(), pos, state);
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
        if (getLevel() != null) {
            getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean lit = state.getValue(MysticBrazierBlock.LIT);
        if (lit) {
            if (litTicks < 30) litTicks++;
            if (litTicks >= 30) {
                if (!fireVisible) {
                    fireVisible = true;
                    setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        } else {
            if (litTicks != 0 || fireVisible) {
                litTicks = 0;
                fireVisible = false;
                setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    public boolean isFireVisible() {
        return fireVisible;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", state -> {
            boolean lit = false;
            if (getLevel() != null) {
                var bs = getLevel().getBlockState(worldPosition);
                if (bs.hasProperty(MysticBrazierBlock.LIT)) lit = bs.getValue(MysticBrazierBlock.LIT);
                else if (getBlockState().hasProperty(MysticBrazierBlock.LIT)) lit = getBlockState().getValue(MysticBrazierBlock.LIT);
            } else if (getBlockState().hasProperty(MysticBrazierBlock.LIT)) {
                lit = getBlockState().getValue(MysticBrazierBlock.LIT);
            }
            var controller = state.controller();
            if (lit) {
                reversing = false;
                controller.setAnimationSpeed(1);
                if (!wasLit) {
                    wasLit = true;
                }
                return state.setAndContinue(LIGHT_UP);
            }
            if (wasLit) {
                wasLit = false;
                reversing = true;
                controller.setAnimationSpeed(1);
                return state.setAndContinue(LIGHT_DOWN);
            }
            if (reversing) {
                if (controller.hasAnimationFinished()) {
                    reversing = false;
                }
                controller.setAnimationSpeed(1);
                return state.setAndContinue(LIGHT_DOWN);
            }
            controller.setAnimationSpeed(1);
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charges", charges);
        output.putInt("litTicks", litTicks);
        output.putBoolean("fireVisible", fireVisible);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("charges", 0);
        litTicks = input.getIntOr("litTicks", 0);
        fireVisible = input.getBooleanOr("fireVisible", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("charges", charges);
        tag.putInt("litTicks", litTicks);
        tag.putBoolean("fireVisible", fireVisible);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
