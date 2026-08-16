package net.multyfora.modjam.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.multyfora.modjam.modjam;

public class LightWeaverItem extends Item {

    public LightWeaverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            BlockPos spawnBlock = context.getClickedPos().relative(context.getClickedFace());
            var weaver = modjam.LIGHT_WEAVER_ENTITY.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            if (weaver != null) {
                weaver.setPos(spawnBlock.getX() + 0.5, spawnBlock.getY() + 0.5, spawnBlock.getZ() + 0.5);
                serverLevel.addFreshEntity(weaver);
                Player player = context.getPlayer();
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.CONSUME;
    }
}
