package net.multyfora.modjam.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.network.FirstContactLeavePayload;
import net.multyfora.modjam.world.dimension.FirstContactUtils;
import net.multyfora.modjam.world.dimension.ModDimensions;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = modjam.MODID)
public class ModJamCommands {

    private static final int LEAVE_DELAY_TICKS = 40;
    private static final Map<ServerPlayer, Integer> pendingTeleports = new HashMap<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("modjam")
            .then(Commands.literal("leave")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    if (player.level().dimension() != ModDimensions.FIRST_CONTACT_LEVEL_KEY) {
                        context.getSource().sendFailure(
                            Component.translatable("command.modjam.leave.not_in_dimension"));
                        return 0;
                    }
                    PacketDistributor.sendToPlayer(player, new FirstContactLeavePayload());
                    pendingTeleports.put(player, LEAVE_DELAY_TICKS);
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.modjam.leave.success"), true);
                    return 1;
                })
            )
        );
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