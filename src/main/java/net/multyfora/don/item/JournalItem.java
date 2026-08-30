package net.multyfora.don.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JournalItem extends Item {
    public JournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            try {
                Class.forName("net.multyfora.don.client.ClientHooks").getMethod("openJournal", ItemStack.class).invoke(null, player.getItemInHand(hand));
            } catch (Exception ignored) {}
        }
        return InteractionResult.SUCCESS;
    }
}
