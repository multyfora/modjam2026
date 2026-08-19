package net.multyfora.modjam.world.entity;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.stateless.StatelessGeoEntity;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.light.LightEnergy;
import net.multyfora.modjam.light.LightEnergyManager;
import net.multyfora.modjam.lightweaver.LightWeaverShapes;
import net.multyfora.modjam.lightweaver.LightWeaverShapes.WeaverShape;
import net.multyfora.modjam.lightweaver.WeaverPaper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

public class LightWeaverEntity extends Entity implements StatelessGeoEntity {
    private static final int TICKS_PER_TIER = 20;

    private static final double LIGHT_MIN = 3.4;
    private static final double LIGHT_MAX = 3.6;

    private static final EntityDataAccessor<Boolean> DATA_PROCESSING = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PROGRESS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_PROGRESS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_PAPER = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.ITEM_STACK);

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(1);

    private Holder<Enchantment> pendingEnchantment;
    private int pendingLevel;
    private String pendingPattern;

    private final AnimatableInstanceCache animatableCache = new InstancedAnimatableInstanceCache(this);

    public LightWeaverEntity(EntityType<? extends LightWeaverEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PROCESSING, false);
        builder.define(DATA_PROGRESS, 0);
        builder.define(DATA_MAX_PROGRESS, 1);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_PAPER, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("armor", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> {
            if (!stack.isEmpty()) {
                try (Transaction transaction = Transaction.openRoot()) {
                    itemHandler.insert(0, ItemResource.of(stack), stack.getCount(), transaction);
                    transaction.commit();
                }
                entityData.set(DATA_ITEM, stack);
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
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ItemResource resource = itemHandler.getResource(0);
        if (!resource.isEmpty()) {
            output.store("armor", ItemStack.OPTIONAL_CODEC, resource.toStack(itemHandler.getAmountAsInt(0)));
        }
        output.storeNullable("pattern", Codec.STRING, pendingPattern);
        if (!getPendingPaper().isEmpty()) {
            output.store("paper", ItemStack.OPTIONAL_CODEC, getPendingPaper());
        }
    }

    @Override
    public void tick() {
        super.tick();
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
        WeaverShape shape = LightWeaverShapes.match(cells);
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
        if (level().isClientSide()) {
            return WeaverPaper.isPaper(held) || EnchantmentHelper.canStoreEnchantments(held)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
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
        if (isProcessing()) {
            playFail();
            return InteractionResult.SUCCESS;
        }

        String packed = WeaverPaper.readPattern(paper);
        boolean[] cells = packed == null ? null : LightWeaverShapes.unpack(packed);
        if (cells == null || LightWeaverShapes.isEmpty(cells) || LightWeaverShapes.match(cells) == null) {
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptPaper FAIL packed={} valid={}", packed, packed != null && LightWeaverShapes.isValidPacked(packed));
            playFail();
            return InteractionResult.SUCCESS;
        }

        pendingPattern = packed;
        ItemStack shownPaper = paper.copyWithCount(1);
        paper.shrink(1);
        entityData.set(DATA_PAPER, shownPaper);
        level().playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.4f);
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptPaper OK shape={}", LightWeaverShapes.match(cells));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult acceptItem(ItemStack held) {
        if (pendingPattern == null || isProcessing()) {
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptItem FAIL pendingPattern={} processing={}", pendingPattern != null, isProcessing());
            playFail();
            return InteractionResult.SUCCESS;
        }

        boolean[] cells = LightWeaverShapes.unpack(pendingPattern);
        WeaverShape shape = LightWeaverShapes.match(cells);
        if (shape == null) {
            pendingPattern = null;
            entityData.set(DATA_PAPER, ItemStack.EMPTY);
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptItem FAIL shape=null");
            playFail();
            return InteractionResult.SUCCESS;
        }

        Holder<Enchantment> enchantment = resolveEnchantment(level(), shape.enchantment());
        if (enchantment == null || (!enchantment.value().isSupportedItem(held) && !held.is(Items.BOOK))) {
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptItem FAIL unsupported item {} for {}", held, shape.id());
            playFail();
            return InteractionResult.SUCCESS;
        }

        if (held.getEnchantments().getLevel(enchantment) >= 1) {
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptItem FAIL duplicate enchant");
            playFail();
            return InteractionResult.SUCCESS;
        }

        if (!itemHandler.getResource(0).isEmpty()) {
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptItem FAIL handler not empty");
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
net.multyfora.modjam.modjam.LOGGER.info("DBG acceptItem OK stored {} count={} waiting-for-light", orbitCopy, orbitCopy.getCount());
        playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.7f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    private void finishInfuse() {
        setProcessing(false);

        if (pendingEnchantment == null) {
            pendingPattern = null;
            entityData.set(DATA_PAPER, ItemStack.EMPTY);
            if (itemHandler.getResource(0).isEmpty()) {
                entityData.set(DATA_ITEM, ItemStack.EMPTY);
            }
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return true;
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
        registrar.add(new AnimationController<>("controller", state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}