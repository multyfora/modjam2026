package net.multyfora.don.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class DisplayBlockEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(DisplayBlockEntity.class, EntityDataSerializers.ITEM_STACK);

    public DisplayBlockEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    public ItemStack getDisplayItem() {
        return entityData.get(DATA_ITEM);
    }

    public void setDisplayItem(ItemStack stack) {
        ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        entityData.set(DATA_ITEM, copy);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("item", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> entityData.set(DATA_ITEM, stack));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ItemStack stack = getDisplayItem();
        if (!stack.isEmpty()) {
            output.store("item", ItemStack.OPTIONAL_CODEC, stack);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack handStack = player.getItemInHand(hand);
        ItemStack displayed = getDisplayItem();
        if (!level().isClientSide()) {
            if (!handStack.isEmpty() && displayed.isEmpty()) {
                setDisplayItem(handStack);
                if (!player.getAbilities().instabuild) {
                    handStack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            } else if (handStack.isEmpty() && !displayed.isEmpty()) {
                ItemStack drop = displayed.copy();
                setDisplayItem(ItemStack.EMPTY);
                Vec3 pos = position().add(0, 0.5, 0);
                ItemEntity itemEntity = new ItemEntity(level(), pos.x, pos.y, pos.z, drop);
                itemEntity.setDeltaMovement(0, 0.15, 0);
                itemEntity.setNoPickUpDelay();
                level().addFreshEntity(itemEntity);
                discard();
                return InteractionResult.SUCCESS;
            }
        }
        if (!handStack.isEmpty() && displayed.isEmpty()) return InteractionResult.SUCCESS;
        if (handStack.isEmpty() && !displayed.isEmpty()) return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }
}
