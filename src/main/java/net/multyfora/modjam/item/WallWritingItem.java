package net.multyfora.modjam.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.util.WallWritingText;

import java.util.function.Consumer;

public class WallWritingItem extends Item {
    private static final String KEY_PLAIN = "wall_plain";

    public WallWritingItem(Properties properties) {
        super(properties);
    }

    public static String getPlain(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(KEY_PLAIN, "");
    }

    public static void setPlain(ItemStack stack, String plain) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY_PLAIN, plain == null ? "" : plain));
    }

    public static ItemStack create(String plain) {
        ItemStack s = new ItemStack(modjam.WALL_WRITING_ITEM.get());
        setPlain(s, plain == null ? "" : plain);
        return s;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        String plain = getPlain(stack);
        if (!plain.isEmpty()) {
            builder.accept(Component.literal(WallWritingText.toSga(plain)).withStyle(s -> s.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.parse("minecraft:alt")))));
            builder.accept(Component.literal(plain).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        if (face == null || face.getAxis() == Direction.Axis.Y) return InteractionResult.FAIL;
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.isAir()) return InteractionResult.FAIL;
        BlockPos wallPos = clicked;
        BlockPos front = wallPos.relative(face);
        if (!level.getBlockState(front).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.FAIL;
        ItemStack stack = ctx.getItemInHand();
        String plain = getPlain(stack);
        var entity = modjam.WALL_WRITING_ENTITY.get().create(sl, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null) return InteractionResult.FAIL;
        var we = (net.multyfora.modjam.world.entity.WallWritingEntity) entity;
        we.setWallData(wallPos, face, plain);
        Vec3 center = Vec3.atCenterOf(wallPos).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.55));
        we.setPos(center.x, center.y, center.z);
        if (!sl.noCollision(we, we.getBoundingBox())) return InteractionResult.FAIL;
        sl.addFreshEntity(we);
        Player p = ctx.getPlayer();
        if (p == null || !p.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
