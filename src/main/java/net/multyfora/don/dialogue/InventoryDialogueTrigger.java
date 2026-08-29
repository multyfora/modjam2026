package net.multyfora.don.dialogue;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record InventoryDialogueTrigger(List<ItemPredicate> items) implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("don", "inventory");

    public static final MapCodec<InventoryDialogueTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemPredicate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(InventoryDialogueTrigger::items)
    ).apply(instance, InventoryDialogueTrigger::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public boolean matches(ServerPlayer player) {
        if (items.isEmpty()) return false;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (matches(inventory.getItem(i))) return true;
        }
        return false;
    }

    private boolean matches(ItemStack stack) {
        for (ItemPredicate predicate : items) {
            if (predicate.test(stack)) return true;
        }
        return false;
    }
}