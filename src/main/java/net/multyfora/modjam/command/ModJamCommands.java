package net.multyfora.modjam.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.world.dimension.FirstContactUtils;
import net.multyfora.modjam.world.dimension.ModDimensions;

@EventBusSubscriber(modid = modjam.MODID)
public class ModJamCommands {

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
                    FirstContactUtils.leaveDimension(player);
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.modjam.leave.success"), true);
                    return 1;
                })
            )
        );
    }
}
