package net.multyfora.don.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.multyfora.don.client.ItemActivationManager;

public class BrightestItem extends Item {
    public BrightestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemActivationManager.getInstance().activate(player.getItemInHand(hand).copy());
        }
        return InteractionResult.SUCCESS;
    }
}
