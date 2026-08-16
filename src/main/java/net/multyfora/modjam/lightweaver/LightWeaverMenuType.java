package net.multyfora.modjam.lightweaver;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.world.entity.LightWeaverEntity;

public final class LightWeaverMenuType {

    private LightWeaverMenuType() {
    }

    public static boolean openUI(ServerPlayer player, LightWeaverEntity weaver) {
        return player.openMenu(new Holder(weaver, player)).isPresent();
    }

    public static ModularUIContainerMenu create(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        if (inventory.player.level().getEntity(entityId) instanceof LightWeaverEntity weaver) {
            return new ModularUIContainerMenu(menuType(), containerId, inventory, new Holder(weaver, inventory.player));
        }
        throw new IllegalArgumentException("Light weaver entity not found: " + entityId);
    }

    private static MenuType<ModularUIContainerMenu> menuType() {
        return (MenuType<ModularUIContainerMenu>) modjam.LIGHT_WEAVER_MENU.get();
    }

    public record Holder(LightWeaverEntity weaver, Player player) implements MenuProvider, IContainerUIHolder {

        @Override
        public boolean isStillValid(Player player) {
            return weaver.isAlive() && !weaver.isRemoved();
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.modjam.light_weaver");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new ModularUIContainerMenu(menuType(), containerId, inventory, this);
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
            buf.writeInt(weaver.getId());
        }

        @Override
        public ModularUI createUI(Player player) {
            return weaver.createUI(player);
        }
    }
}
