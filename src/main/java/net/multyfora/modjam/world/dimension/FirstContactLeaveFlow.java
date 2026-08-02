package net.multyfora.modjam.world.dimension;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.network.FirstContactLeavePayload;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = modjam.MODID)
public class FirstContactLeaveFlow {

    private static final int LEAVE_DELAY_TICKS = 40;
    private static final Map<ServerPlayer, Integer> pendingTeleports = new HashMap<>();

    public static void startLeaveSequence(ServerPlayer player) {
        if (player.level().dimension() != ModDimensions.FIRST_CONTACT_LEVEL_KEY) return;
        PacketDistributor.sendToPlayer(player, new FirstContactLeavePayload());
        pendingTeleports.put(player, LEAVE_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        pendingTeleports.entrySet().removeIf(entry -> {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                var player = entry.getKey();
                if (player.level().dimension() == ModDimensions.FIRST_CONTACT_LEVEL_KEY) {
                    FirstContactUtils.leaveDimension(player);
                    PacketDistributor.sendToPlayer(player, new FirstContactEnterPayload());
                }
                return true;
            }
            entry.setValue(remaining);
            return false;
        });
    }
}
