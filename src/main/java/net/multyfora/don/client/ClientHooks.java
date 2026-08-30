package net.multyfora.don.client;

import net.minecraft.world.item.ItemStack;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openJournal(ItemStack stack) {
        JournalGui.open(stack);
    }

    public static void activateBrightest(ItemStack stack) {
        ItemActivationManager.getInstance().activate(stack);
    }
}
