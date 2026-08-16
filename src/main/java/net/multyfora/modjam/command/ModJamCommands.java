package net.multyfora.modjam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.dialogue.DialogueEventManager;
import net.multyfora.modjam.world.dimension.FirstContactLeaveFlow;
import net.multyfora.modjam.world.dimension.ModDimensions;

import java.util.Set;

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
                    FirstContactLeaveFlow.startLeaveSequence(player);
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.modjam.leave.success"), true);
                    return 1;
                })
            )
            .then(Commands.literal("resetdialogues")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    DialogueEventManager.resetPlayerProgress(player);
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.modjam.resetdialogues.success"), true);
                    return 1;
                })
            )
            .then(Commands.literal("playdialogue")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    Set<Identifier> events = DialogueEventManager.registeredEvents();
                    if (events.isEmpty()) {
                        context.getSource().sendFailure(
                            Component.translatable("command.modjam.playdialogue.none"));
                        return 0;
                    }
                    for (Identifier id : events) {
                        DialogueEventManager.runEvent(player, id);
                    }
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.modjam.playdialogue.all", events.size()), true);
                    return events.size();
                })
                .then(Commands.argument("event", StringArgumentType.greedyString())
                    .suggests((context, builder) -> {
                        for (Identifier id : DialogueEventManager.registeredEvents()) {
                            builder.suggest(id.toString());
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var player = (ServerPlayer) context.getSource().getEntity();
                        String name = StringArgumentType.getString(context, "event");
                        Identifier id;
                        try {
                            id = Identifier.parse(name);
                        } catch (Exception e) {
                            context.getSource().sendFailure(
                                Component.translatable("command.modjam.playdialogue.invalid", name));
                            return 0;
                        }
                        if (!DialogueEventManager.runEvent(player, id)) {
                            context.getSource().sendFailure(
                                Component.translatable("command.modjam.playdialogue.unknown", name));
                            return 0;
                        }
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.modjam.playdialogue.single", name), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("cheat")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    PlayerUIMenuType.openUI(player, modjam.CHEAT_SHEET_ID);
                    return 1;
                })
            )
        );
    }
}
