package net.multyfora.don.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BrightestItem extends Item {
    public BrightestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemStack copy = player.getItemInHand(hand).copy();
            try {
                Class.forName("net.multyfora.don.client.ClientHooks").getMethod("activateBrightest", ItemStack.class).invoke(null, copy);
            } catch (Exception ignored) {}
        }
        return InteractionResult.SUCCESS;
    }
}
