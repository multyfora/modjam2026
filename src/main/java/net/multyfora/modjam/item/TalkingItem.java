package net.multyfora.modjam.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.multyfora.modjam.client.ItemActivationManager;

import java.util.function.Function;

public class TalkingItem extends Item {
    private final Function<Player, Component> messageProvider;

    public TalkingItem(Properties properties, Function<Player, Component> messageProvider) {
        super(properties);
        this.messageProvider = messageProvider;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemActivationManager.getInstance().activate(player.getItemInHand(hand).copy());
        }
        return InteractionResult.SUCCESS;
    }
}
