package net.multyfora.don.world.entity;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.stateless.StatelessGeoEntity;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.light.LightEnergy;
import net.multyfora.don.light.LightEnergyManager;
import net.multyfora.don.lightweaver.LightWeaverShapes;
import net.multyfora.don.lightweaver.LightWeaverShapes.WeaverShape;
import net.multyfora.don.lightweaver.WeaverPaper;
import net.multyfora.don.don;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class LightWeaverEntity extends PathfinderMob implements StatelessGeoEntity {
    private static final int TICKS_PER_TIER = 20;

    private static final double LIGHT_MIN = 3.4;
    private static final double LIGHT_MAX = 3.6;

    private static final double LIGHT_SEEK_RANGE = 12.0;
    private static final double LIGHT_STOP_DISTANCE_SQ = 2.5 * 2.5;
    private static final int LIGHT_SCAN_INTERVAL = 40;

    private static final EntityDataAccessor<Boolean> DATA_PROCESSING = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MOVING = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PROGRESS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_PROGRESS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_PAPER = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.ITEM_STACK);

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(1);

    private Holder<Enchantment> pendingEnchantment;
    private int pendingLevel;
    private String pendingPattern;
    private int breedCooldown;

    private static final int BREED_COOLDOWN_TICKS = 6000;

    private final AnimatableInstanceCache animatableCache = new InstancedAnimatableInstanceCache(this);

    public LightWeaverEntity(EntityType<? extends LightWeaverEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SeekLightSourceGoal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROCESSING, false);
        builder.define(DATA_MOVING, false);
        builder.define(DATA_PROGRESS, 0);
        builder.define(DATA_MAX_PROGRESS, 1);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_PAPER, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("armor", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> {
            if (!stack.isEmpty()) {
                try (Transaction transaction = Transaction.openRoot()) {
                    itemHandler.insert(0, ItemResource.of(stack), stack.getCount(), transaction);
                    transaction.commit();
                }
                entityData.set(DATA_ITEM, itemHandler.getResource(0).toStack(itemHandler.getAmountAsInt(0)));
            }
        });
        input.read("paper", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> {
            if (!stack.isEmpty()) {
                entityData.set(DATA_PAPER, stack);
            }
        });
        input.getString("pattern").ifPresent(packed -> {
            if (LightWeaverShapes.isValidPacked(packed)) {
                pendingPattern = packed;
            }
        });
        breedCooldown = input.getInt("breed_cooldown").orElse(0);
        if (input.getBooleanOr("processing", false)) {
            entityData.set(DATA_PROCESSING, true);
            setProgress(input.getInt("progress").orElse(0));
            setMaxProgress(input.getInt("max_progress").orElse(1));
            input.getString("enchantment").ifPresent(id -> {
                Identifier enchId = Identifier.tryParse(id);
                Holder<Enchantment> holder = enchId == null ? null : resolveEnchantment(level(), ResourceKey.create(Registries.ENCHANTMENT, enchId));
                if (holder != null) {
                    pendingEnchantment = holder;
                    pendingLevel = Math.min(input.getInt("enchantment_level").orElse(1),
                            Math.max(1, holder.value().getMaxLevel()));
                } else {
                    entityData.set(DATA_PROCESSING, false);
                }
            });
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ItemResource resource = itemHandler.getResource(0);
        if (!resource.isEmpty()) {
            output.store("armor", ItemStack.OPTIONAL_CODEC, resource.toStack(itemHandler.getAmountAsInt(0)));
        }
        output.storeNullable("pattern", Codec.STRING, pendingPattern);
        if (breedCooldown > 0) {
            output.putInt("breed_cooldown", breedCooldown);
        }
        if (!getPendingPaper().isEmpty()) {
            output.store("paper", ItemStack.OPTIONAL_CODEC, getPendingPaper());
        }
        if (isProcessing() && pendingEnchantment != null && pendingEnchantment.unwrapKey().isPresent()) {
            output.putBoolean("processing", true);
            output.putInt("progress", getProgress());
            output.putInt("max_progress", getMaxProgress());
            output.putString("enchantment", pendingEnchantment.unwrapKey().orElseThrow().identifier().toString());
            output.putInt("enchantment_level", pendingLevel);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (breedCooldown > 0) {
            breedCooldown--;
        }
        if (level().isClientSide()) {
            if (isProcessing() && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.END_ROD,
                        position().x + (random.nextDouble() - 0.5) * 0.6,
                        position().y + 1.05 + random.nextDouble() * 0.3,
                        position().z + (random.nextDouble() - 0.5) * 0.6,
                        0.0, 0.02, 0.0);
            }
            return;
        }

        if (isProcessing()) {
            if (!getNavigation().isDone()) {
                getNavigation().stop();
            }
            setProgress(getProgress() + 1);

            if (level() instanceof ServerLevel serverLevel && getProgress() % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        position().x, position().y + 1.05, position().z,
                        6, 0.3, 0.25, 0.3, 0.03);
            }

            if (getProgress() >= getMaxProgress()) {
                finishInfuse();
            }
        } else {
            tryStartProcessing();
        }

        boolean moving = !getNavigation().isDone();
        if (moving != isMoving()) {
            setMoving(moving);
        }
    }

    private void tryStartProcessing() {
        if (pendingPattern == null || isProcessing() || itemHandler.getResource(0).isEmpty()) {
            return;
        }

        LightEnergy energy = LightEnergyManager.compute(level(), blockPosition());
        if (!energy.isPresent() || energy.mysticalComponent() < LIGHT_MIN || energy.mysticalComponent() > LIGHT_MAX) {
            return;
        }

        boolean[] cells = LightWeaverShapes.unpack(pendingPattern);
        WeaverShape shape = LightWeaverShapes.matchOrEmpty(cells);
        if (shape == null) {
            return;
        }

        Holder<Enchantment> enchantment = resolveEnchantment(level(), shape.enchantment());
        if (enchantment == null) {
            return;
        }

        ItemStack stored = itemHandler.getResource(0).toStack(itemHandler.getAmountAsInt(0));
        if (!enchantment.value().isSupportedItem(stored) && !stored.is(Items.BOOK)) {
            return;
        }

        int appliedLevel = Mth.clamp((int) Math.round(energy.intensity()), 1, enchantment.value().getMaxLevel());
        pendingEnchantment = enchantment;
        pendingLevel = appliedLevel;
        setProgress(0);
        setMaxProgress(Math.max(1, shape.tier() * TICKS_PER_TIER));
        setProcessing(true);
        playSound(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.4f);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.AMETHYST_SHARD)) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return tryBreed(player, held);
        }
        if (level().isClientSide()) {
            return WeaverPaper.isPaper(held) || EnchantmentHelper.canStoreEnchantments(held)
                    || (player.isShiftKeyDown() && !getHeldItem().isEmpty())
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        if (player.isShiftKeyDown() && !itemHandler.getResource(0).isEmpty()) {
            returnStoredItem();
            playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.7f, 1.2f);
            return InteractionResult.SUCCESS;
        }

        if (WeaverPaper.isPaper(held)) {
            return acceptPaper(held);
        }
        if (EnchantmentHelper.canStoreEnchantments(held)) {
            return acceptItem(held);
        }

        playFail();
        return InteractionResult.SUCCESS;
    }

    private InteractionResult acceptPaper(ItemStack paper) {
        if (isProcessing() || !itemHandler.getResource(0).isEmpty()) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        String packed = WeaverPaper.readPattern(paper);
        boolean[] cells = packed == null ? null : LightWeaverShapes.unpack(packed);
        if (cells == null || LightWeaverShapes.matchOrEmpty(cells) == null) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        pendingPattern = packed;
        ItemStack previousPaper = getPendingPaper();
        if (!previousPaper.isEmpty() && level() instanceof ServerLevel serverLevel) {
            spawnAtLocation(serverLevel, previousPaper);
        }
        ItemStack shownPaper = paper.copyWithCount(1);
        paper.shrink(1);
        entityData.set(DATA_PAPER, shownPaper);
        level().playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.4f);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult acceptItem(ItemStack held) {
        if (pendingPattern == null || isProcessing()) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        boolean[] cells = LightWeaverShapes.unpack(pendingPattern);
        WeaverShape shape = LightWeaverShapes.matchOrEmpty(cells);
        if (shape == null) {
            pendingPattern = null;
            entityData.set(DATA_PAPER, ItemStack.EMPTY);
            playFail();
            return InteractionResult.SUCCESS;
        }

        Holder<Enchantment> enchantment = resolveEnchantment(level(), shape.enchantment());
        if (enchantment == null || (!enchantment.value().isSupportedItem(held) && !held.is(Items.BOOK))) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        if (held.getEnchantments().getLevel(enchantment) >= 1) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        if (!itemHandler.getResource(0).isEmpty()) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        ItemStack orbitCopy = held.copy();
        try (Transaction transaction = Transaction.openRoot()) {
            itemHandler.insert(0, ItemResource.of(held), held.getCount(), transaction);
            transaction.commit();
        }
        held.shrink(held.getCount());
        entityData.set(DATA_ITEM, orbitCopy);
        playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.7f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    private void finishInfuse() {
        setProcessing(false);

        if (pendingEnchantment == null) {
            returnStoredItem();
            pendingPattern = null;
            entityData.set(DATA_PAPER, ItemStack.EMPTY);
            return;
        }

        ItemResource armorResource = itemHandler.getResource(0);
        if (armorResource.isEmpty()) {
            pendingEnchantment = null;
            pendingPattern = null;
            entityData.set(DATA_ITEM, ItemStack.EMPTY);
            entityData.set(DATA_PAPER, ItemStack.EMPTY);
            return;
        }

        ItemStack stack = armorResource.toStack(itemHandler.getAmountAsInt(0));
        ItemStack enchanted = stack.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : stack.copy();
        enchanted.enchant(pendingEnchantment, pendingLevel);

        try (Transaction transaction = Transaction.openRoot()) {
            itemHandler.extract(0, armorResource, itemHandler.getAmountAsInt(0), transaction);
            transaction.commit();
        }

        if (level() instanceof ServerLevel serverLevel) {
            spawnAtLocation(serverLevel, enchanted);

            LightEnergyManager.drainAll(level(), blockPosition());

            playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.1f);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    position().x, position().y + 1.2, position().z,
                    24, 0.4, 0.4, 0.4, 0.06);
        }

        entityData.set(DATA_ITEM, ItemStack.EMPTY);
        entityData.set(DATA_PAPER, ItemStack.EMPTY);
        pendingEnchantment = null;
        pendingPattern = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            returnStoredItem();
            ItemStack paper = getPendingPaper();
            if (!paper.isEmpty() && level() instanceof ServerLevel serverLevel) {
                spawnAtLocation(serverLevel, paper);
                entityData.set(DATA_PAPER, ItemStack.EMPTY);
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    public boolean isProcessing() {
        return entityData.get(DATA_PROCESSING);
    }

    public ItemStack getHeldItem() {
        return entityData.get(DATA_ITEM);
    }

    public ItemStack getPendingPaper() {
        return entityData.get(DATA_PAPER);
    }

    private void setProcessing(boolean processing) {
        entityData.set(DATA_PROCESSING, processing);
    }

    public float getProgressFraction() {
        return getMaxProgress() <= 0 ? 0f : Mth.clamp((float) getProgress() / getMaxProgress(), 0f, 1f);
    }

    private int getProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    private void setProgress(int progress) {
        entityData.set(DATA_PROGRESS, progress);
    }

    private int getMaxProgress() {
        return entityData.get(DATA_MAX_PROGRESS);
    }

    private void setMaxProgress(int maxProgress) {
        entityData.set(DATA_MAX_PROGRESS, maxProgress);
    }

    private void returnStoredItem() {
        ItemResource resource = itemHandler.getResource(0);
        if (resource.isEmpty()) {
            entityData.set(DATA_ITEM, ItemStack.EMPTY);
            return;
        }
        ItemStack stack = resource.toStack(itemHandler.getAmountAsInt(0));
        try (Transaction transaction = Transaction.openRoot()) {
            itemHandler.extract(0, resource, itemHandler.getAmountAsInt(0), transaction);
            transaction.commit();
        }
        if (level() instanceof ServerLevel serverLevel) {
            spawnAtLocation(serverLevel, stack);
        }
        entityData.set(DATA_ITEM, ItemStack.EMPTY);
    }

    private InteractionResult tryBreed(Player player, ItemStack held) {
        if (breedCooldown > 0) {
            playFail();
            return InteractionResult.SUCCESS;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        breedCooldown = BREED_COOLDOWN_TICKS;
        LightWeaverEntity child = don.LIGHT_WEAVER_ENTITY.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
        if (child == null) {
            return InteractionResult.SUCCESS;
        }
        Vec3 offset = new Vec3((random.nextDouble() - 0.5) * 2.0, 0.0, (random.nextDouble() - 0.5) * 2.0);
        Vec3 pos = position().add(offset);
        child.setPos(pos.x, pos.y, pos.z);
        child.setYRot(random.nextFloat() * 360.0f);
        serverLevel.addFreshEntity(child);
        serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 7, 0.4, 0.4, 0.4, 0.1);
        child.level().addParticle(ParticleTypes.HEART, child.getX(), child.getY() + 1.0, child.getZ(), 0.0, 0.4, 0.0);
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        serverLevel.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.8f, 1.4f);
        return InteractionResult.SUCCESS;
    }

    private void playFail() {
        level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    @Nullable
    private static Holder<Enchantment> resolveEnchantment(Level level, ResourceKey<Enchantment> key) {
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return registry.get(key).orElse(null);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>("main", state -> {
            if (isProcessing()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("weaving"));
            }
            if (isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    public boolean isMoving() {
        return entityData.get(DATA_MOVING);
    }

    private void setMoving(boolean moving) {
        entityData.set(DATA_MOVING, moving);
    }

    class SeekLightSourceGoal extends Goal {
        @Nullable
        private BlockPos target;
        private int nextScan;

        SeekLightSourceGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (LightWeaverEntity.this.isProcessing()) {
                return false;
            }
            refreshTarget();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (LightWeaverEntity.this.isProcessing() || target == null) {
                return false;
            }
            return LightEnergyManager.isActiveSource(LightWeaverEntity.this.level(), target)
                    && distanceToTargetSq() > LIGHT_STOP_DISTANCE_SQ;
        }

        @Override
        public void start() {
            nextScan = LIGHT_SCAN_INTERVAL;
            moveToTarget();
        }

        @Override
        public void stop() {
            target = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            double distanceSq = distanceToTargetSq();
            if (distanceSq <= LIGHT_STOP_DISTANCE_SQ) {
                target = null;
                getNavigation().stop();
                return;
            }

            refreshTarget();
            if (target == null) {
                getNavigation().stop();
            } else if (getNavigation().isDone()) {
                moveToTarget();
            }
        }

        private void refreshTarget() {
            if (--nextScan > 0) {
                return;
            }
            nextScan = LIGHT_SCAN_INTERVAL;
            target = findTarget();
        }

        private void moveToTarget() {
            if (target == null) {
                return;
            }
            Vec3 center = Vec3.atCenterOf(target);
            getNavigation().moveTo(center.x, center.y, center.z, 1.0);
        }

        private double distanceToTargetSq() {
            return target == null ? Double.MAX_VALUE : LightWeaverEntity.this.distanceToSqr(Vec3.atCenterOf(target));
        }

        @Nullable
        private BlockPos findTarget() {
            BlockPos base = LightWeaverEntity.this.blockPosition();
            int range = Mth.ceil(LIGHT_SEEK_RANGE);
            BlockPos min = base.offset(-range, -range / 2, -range);
            BlockPos max = base.offset(range, range / 2, range);

            BlockPos best = null;
            double bestDistanceSq = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (!LightEnergyManager.isActiveSource(LightWeaverEntity.this.level(), pos)) {
                    continue;
                }
                double distanceSq = LightWeaverEntity.this.distanceToSqr(Vec3.atCenterOf(pos));
                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    best = pos.immutable();
                }
            }
            if (best != null && bestDistanceSq <= LIGHT_STOP_DISTANCE_SQ) {
                return null;
            }
            return best;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}