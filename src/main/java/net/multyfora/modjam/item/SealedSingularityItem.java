package net.multyfora.modjam.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.multyfora.modjam.block.AmethystCrystalBlockEntity;
import net.multyfora.modjam.block.MysticBrazierBlockEntity;
import net.multyfora.modjam.block.SoulLightBlockEntity;
import net.multyfora.modjam.client.renderer.SealedSingularityRenderer;
import net.multyfora.modjam.modjam;

import java.util.function.Consumer;

public class SealedSingularityItem extends Item implements GeoItem {
    public static final int MAX_CHARGES = 3;
    public static final DataTicket<Integer> CHARGES_DATA = DataTicket.create("sealed_singularity_charges", Integer.class);
    public static final DataTicket<Integer> MODE_DATA = DataTicket.create("sealed_singularity_mode", Integer.class);
    public static final DataTicket<Boolean> USING_DATA = DataTicket.create("sealed_singularity_using", Boolean.class);

    public static final int MODE_SEALED = 0;
    public static final int MODE_UNCAPPED = 1;
    public static final int MODE_ALT = 2;

    public static final String CONTROLLER_MAIN = "main";
    public static final String ANIM_IDLE = "idle";
    public static final String ANIM_IDLE_UNCAPPED = "idle_uncapped";
    public static final String ANIM_IDLE_ALT = "idle_alt";
    public static final String ANIM_USE_UNCAPPED = "uncapped_use";
    public static final String ANIM_USE_ALT = "idle_alt_use";

    private static final String CHARGES_KEY = "charges";
    private static final String MODE_KEY = "mode";
    private static final int DRAIN_RADIUS = 4;
    private static final int CHANNEL_CYCLE = 40;
    private static final int USE_DURATION_MAX = 72000;
    private static final double CHANNEL_RANGE = 5.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SealedSingularityItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(CONTROLLER_MAIN, state -> {
            Integer mode = state.getData(MODE_DATA);
            int m = mode == null ? MODE_SEALED : mode;
            Boolean using = state.getData(USING_DATA);
            String anim = using != null && using && m != MODE_SEALED
                ? (m == MODE_ALT ? ANIM_USE_ALT : ANIM_USE_UNCAPPED)
                : idleFor(m);
            if (!anim.equals(lastLoggedAnim)) {
                lastLoggedAnim = anim;
                modjam.LOGGER.info("[SSDebug] anim -> {} (mode={}, using={})", anim, m, using);
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
        }));
    }

    private static String lastLoggedAnim = "";

    private static String idleFor(int mode) {
        return switch (mode) {
            case MODE_UNCAPPED -> ANIM_IDLE_UNCAPPED;
            case MODE_ALT -> ANIM_IDLE_ALT;
            default -> ANIM_IDLE;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SealedSingularityRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new SealedSingularityRenderer();
                }
                return this.renderer;
            }
        });
    }

    public static int getCharges(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getIntOr(CHARGES_KEY, 0), 0, MAX_CHARGES);
    }

    private static void setCharges(ItemStack stack, int charges) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putInt(CHARGES_KEY, Math.clamp(charges, 0, MAX_CHARGES)));
    }

    public static int getMode(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getIntOr(MODE_KEY, MODE_SEALED), MODE_SEALED, MODE_ALT);
    }

    private static void setMode(ItemStack stack, int mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putInt(MODE_KEY, Math.clamp(mode, MODE_SEALED, MODE_ALT)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        int charges = getCharges(stack);
        builder.accept(Component.translatable("tooltip.modjam.sealed_singularity.charges", charges, MAX_CHARGES)
            .withStyle(charges > 0 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));
        String modeKey = switch (getMode(stack)) {
            case MODE_UNCAPPED -> "uncapped";
            case MODE_ALT -> "alt";
            default -> "sealed";
        };
        builder.accept(Component.translatable("tooltip.modjam.sealed_singularity.mode",
            Component.translatable("tooltip.modjam.sealed_singularity.mode." + modeKey))
            .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int mode = getMode(stack);

        if (player.isShiftKeyDown()) {
            setMode(stack, mode == MODE_ALT ? MODE_SEALED : MODE_ALT);
        } else {
            setMode(stack, mode == MODE_SEALED ? MODE_UNCAPPED : MODE_SEALED);
        }

        if (level instanceof ServerLevel serverLevel) {
            int newMode = getMode(stack);
            var sound = newMode == MODE_ALT ? SoundEvents.UI_BUTTON_CLICK.value() : SoundEvents.AMETHYST_BLOCK_RESONATE;
            float pitch = switch (newMode) {
                case MODE_UNCAPPED -> 1.6f;
                case MODE_ALT -> 1.4f;
                default -> 0.7f;
            };
            serverLevel.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS,
                newMode == MODE_ALT ? 0.6f : 1.0f, pitch);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            int mode = getMode(stack);
            setMode(stack, mode == MODE_ALT ? MODE_SEALED : MODE_ALT);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        int mode = getMode(stack);
        if (mode == MODE_SEALED || !isChannelValid(level, pos, stack)) {
            return mode == MODE_SEALED ? InteractionResult.PASS : fail(level, player);
        }
        player.startUsingItem(context.getHand());
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return getMode(stack) == MODE_SEALED ? 0 : USE_DURATION_MAX;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        BlockPos target = channelTarget(player);
        int mode = getMode(stack);
        if (target == null || !isChannelValid(serverLevel, target, stack)) {
            playFail(serverLevel, player);
            player.stopUsingItem();
            return;
        }

        int elapsed = USE_DURATION_MAX - ticksRemaining;
        if (elapsed > 0 && elapsed % CHANNEL_CYCLE == 0) {
            InteractionResult result = mode == MODE_UNCAPPED
                ? drainOneAround(serverLevel, target, player, stack)
                : spendOn(serverLevel, target, player, stack);
            if (result.consumesAction()
                && (target = channelTarget(player)) != null
                && isChannelValid(serverLevel, target, stack)) {
                return;
            }
            player.stopUsingItem();
            return;
        }

        int phaseTick = elapsed % CHANNEL_CYCLE;
        if (phaseTick % 10 == 0 && phaseTick > 0) {
            float progress = (float) phaseTick / CHANNEL_CYCLE;
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.5f, 0.7f + progress * progress * 1.2f);
        }
    }

    private static BlockPos channelTarget(Player player) {
        HitResult hit = player.pick(CHANNEL_RANGE, 1.0f, false);
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
            ? blockHit.getBlockPos()
            : null;
    }

    private boolean isChannelValid(Level level, BlockPos center, ItemStack stack) {
        int mode = getMode(stack);
        return switch (mode) {
            case MODE_UNCAPPED -> canDrainAround(level, center, stack);
            case MODE_ALT -> getCharges(stack) > 0 && isFillable(level, center);
            default -> false;
        };
    }

    private static boolean canDrainAround(Level level, BlockPos center, ItemStack stack) {
        if (getCharges(stack) >= MAX_CHARGES) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-DRAIN_RADIUS, -DRAIN_RADIUS, -DRAIN_RADIUS),
                center.offset(DRAIN_RADIUS, DRAIN_RADIUS, DRAIN_RADIUS))) {
            if (level.getBlockEntity(p) instanceof AmethystCrystalBlockEntity crystal && crystal.getCharges() > 0) {
                return true;
            }
            if (level.getBlockEntity(p) instanceof SoulLightBlockEntity soul && soul.getCharges() > 0) {
                return true;
            }
            if (level.getBlockEntity(p) instanceof MysticBrazierBlockEntity brazier && brazier.getCharges() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFillable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(modjam.SINGULARITY_CRYSTAL_BLOCK.get())) {
            return true;
        }
        if (soulVariantOf(state) != null) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof AmethystCrystalBlockEntity crystal) {
            return crystal.getCharges() < 9;
        }
        if (level.getBlockEntity(pos) instanceof MysticBrazierBlockEntity brazier) {
            return brazier.getCharges() < MysticBrazierBlockEntity.MAX_CHARGES;
        }
        return false;
    }

    private InteractionResult drainOneAround(ServerLevel level, BlockPos center, Player player, ItemStack stack) {
        if (getCharges(stack) >= MAX_CHARGES) {
            return InteractionResult.FAIL;
        }
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-DRAIN_RADIUS, -DRAIN_RADIUS, -DRAIN_RADIUS),
                center.offset(DRAIN_RADIUS, DRAIN_RADIUS, DRAIN_RADIUS))) {
            boolean took = false;
            if (level.getBlockEntity(p) instanceof AmethystCrystalBlockEntity crystal && crystal.getCharges() > 0) {
                took = crystal.useLight();
            } else if (level.getBlockEntity(p) instanceof SoulLightBlockEntity soul && soul.getCharges() > 0) {
                took = soul.useLight();
            } else if (level.getBlockEntity(p) instanceof MysticBrazierBlockEntity brazier && brazier.getCharges() > 0) {
                took = brazier.useCharge();
            }
            if (took) {
                setCharges(stack, getCharges(stack) + 1);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0f, 0.7f);
                return InteractionResult.SUCCESS;
            }
        }
        playFail(level, player);
        return InteractionResult.FAIL;
    }

    private InteractionResult spendOn(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof AmethystCrystalBlockEntity crystal) {
            return depositInto(level, crystal::addCharge, player, stack);
        }
        if (state.is(modjam.SINGULARITY_CRYSTAL_BLOCK.get())) {
            return relightSingularityCrystal(level, pos, player, stack);
        }
        if (level.getBlockEntity(pos) instanceof MysticBrazierBlockEntity brazier) {
            return depositInto(level, brazier::addCharge, player, stack);
        }
        Block soulVariant = soulVariantOf(state);
        if (soulVariant != null) {
            return soulify(level, pos, state, soulVariant, player, stack);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult fail(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            playFail(serverLevel, player);
        }
        return InteractionResult.FAIL;
    }

    private static InteractionResult depositInto(ServerLevel level, java.util.function.BooleanSupplier deposit,
                                                 Player player, ItemStack stack) {
        if (getCharges(stack) <= 0 || !deposit.getAsBoolean()) {
            playFail(level, player);
            return InteractionResult.FAIL;
        }
        setCharges(stack, getCharges(stack) - 1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult relightSingularityCrystal(ServerLevel level, BlockPos pos,
                                                               Player player, ItemStack stack) {
        if (getCharges(stack) <= 0) {
            playFail(level, player);
            return InteractionResult.FAIL;
        }
        BlockState cluster = Blocks.AMETHYST_CLUSTER.defaultBlockState()
            .setValue(AmethystClusterBlock.FACING, Direction.UP);
        level.setBlock(pos, cluster, 3);
        if (level.getBlockEntity(pos) instanceof AmethystCrystalBlockEntity crystal) {
            crystal.setCharges(1);
        }
        setCharges(stack, getCharges(stack) - 1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult soulify(ServerLevel level, BlockPos pos, BlockState state, Block soulVariant,
                                             Player player, ItemStack stack) {
        if (getCharges(stack) <= 0) {
            playFail(level, player);
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, soulVariant.withPropertiesOf(state), 3);
        if (level.getBlockEntity(pos) instanceof SoulLightBlockEntity soul) {
            soul.addCharge();
        }
        setCharges(stack, getCharges(stack) - 1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.1f);
        return InteractionResult.SUCCESS;
    }

    private static Block soulVariantOf(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.LANTERN) return Blocks.SOUL_LANTERN;
        if (block == Blocks.TORCH) return Blocks.SOUL_TORCH;
        if (block == Blocks.WALL_TORCH) return Blocks.SOUL_WALL_TORCH;
        if (block == Blocks.CAMPFIRE) return Blocks.SOUL_CAMPFIRE;
        return null;
    }

    private static void playFail(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.4f);
    }
}
