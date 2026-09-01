package net.multyfora.don.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.don;

public class WeaverGlyphItem extends Item {
    public WeaverGlyphItem(Properties properties) {
        super(properties);
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
        var entity = don.WEAVER_GLYPH_ENTITY.get().create(sl, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null) return InteractionResult.FAIL;
        var glyph = (net.multyfora.don.world.entity.WeaverGlyphEntity) entity;
        glyph.setWallData(wallPos, face, "");
        Vec3 center = Vec3.atCenterOf(wallPos).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.55));
        glyph.setPos(center.x, center.y, center.z);
        if (!sl.noCollision(glyph, glyph.getBoundingBox())) return InteractionResult.FAIL;
        sl.addFreshEntity(glyph);
        var player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
