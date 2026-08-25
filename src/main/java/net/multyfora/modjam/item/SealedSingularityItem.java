package net.multyfora.modjam.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import net.multyfora.modjam.block.AmethystCrystalBlockEntity;
import net.multyfora.modjam.block.MysticBrazierBlockEntity;
import net.multyfora.modjam.block.SoulLightBlockEntity;
import net.multyfora.modjam.modjam;

import java.util.function.Consumer;

public class SealedSingularityItem extends Item {
    public static final int MAX_CHARGES = 3;
    private static final String CHARGES_KEY = "charges";

    public SealedSingularityItem(Properties properties) {
        super(properties);
    }

    public static int getCharges(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getIntOr(CHARGES_KEY, 0), 0, MAX_CHARGES);
    }

    private static void setCharges(ItemStack stack, int charges) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putInt(CHARGES_KEY, Math.clamp(charges, 0, MAX_CHARGES)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        int charges = getCharges(stack);
        builder.accept(Component.translatable("tooltip.modjam.sealed_singularity.charges", charges, MAX_CHARGES)
            .withStyle(charges > 0 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        boolean taking = player.isShiftKeyDown();

        if (level.getBlockEntity(pos) instanceof AmethystCrystalBlockEntity crystal) {
            return taking ? takeFrom(serverLevel, crystal::useLight, player, stack)
                          : depositInto(serverLevel, crystal::addCharge, player, stack);
        }
        if (!taking && state.is(Blocks.AMETHYST_CLUSTER)) {
            return InteractionResult.PASS;
        }
        if (!taking && state.is(modjam.SINGULARITY_CRYSTAL_BLOCK.get())) {
            return relightSingularityCrystal(serverLevel, pos, player, stack);
        }
        if (taking && level.getBlockEntity(pos) instanceof SoulLightBlockEntity soul) {
            return takeFrom(serverLevel, soul::useLight, player, stack);
        }
        if (level.getBlockEntity(pos) instanceof MysticBrazierBlockEntity brazier) {
            return taking ? takeFrom(serverLevel, brazier::useCharge, player, stack)
                          : depositInto(serverLevel, brazier::addCharge, player, stack);
        }
        if (!taking) {
            Block soulVariant = soulVariantOf(state);
            if (soulVariant != null) {
                return soulify(serverLevel, pos, state, soulVariant, player, stack);
            }
        }
        return InteractionResult.PASS;
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

    private static InteractionResult takeFrom(ServerLevel level, java.util.function.BooleanSupplier take,
                                              Player player, ItemStack stack) {
        if (getCharges(stack) >= MAX_CHARGES || !take.getAsBoolean()) {
            playFail(level, player);
            return InteractionResult.FAIL;
        }
        setCharges(stack, getCharges(stack) + 1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult relightSingularityCrystal(ServerLevel level, BlockPos pos,
                                                               Player player, ItemStack stack) {
        if (getCharges(stack) <= 0) {
            playFail(level, player);
            return InteractionResult.FAIL;
        }
        BlockState cluster = Blocks.AMETHYST_CLUSTER.defaultBlockState()
            .setValue(AmethystClusterBlock.FACING, net.minecraft.core.Direction.UP);
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
