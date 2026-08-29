package net.multyfora.don.command;

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
import net.multyfora.don.don;
import net.multyfora.don.dialogue.DialogueEventManager;
import net.multyfora.don.cutscene.CutsceneManager;
import net.multyfora.don.journal.JournalEntryManager;
import net.multyfora.don.world.dimension.FirstContactLeaveFlow;
import net.multyfora.don.world.dimension.ModDimensions;

import java.util.Set;

@EventBusSubscriber(modid = don.MODID)
public class DonCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("don")
            .then(Commands.literal("leave")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    if (player.level().dimension() != ModDimensions.FIRST_CONTACT_LEVEL_KEY) {
                        context.getSource().sendFailure(
                            Component.translatable("command.don.leave.not_in_dimension"));
                        return 0;
                    }
                    FirstContactLeaveFlow.startLeaveSequence(player);
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.don.leave.success"), true);
                    return 1;
                })
            )
            .then(Commands.literal("resetdialogues")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    DialogueEventManager.resetPlayerProgress(player);
                    CutsceneManager.resetPlayerProgress(player);
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.don.resetdialogues.success"), true);
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
                            Component.translatable("command.don.playdialogue.none"));
                        return 0;
                    }
                    for (Identifier id : events) {
                        DialogueEventManager.runEvent(player, id);
                    }
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.don.playdialogue.all", events.size()), true);
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
                                Component.translatable("command.don.playdialogue.invalid", name));
                            return 0;
                        }
                        if (!DialogueEventManager.runEvent(player, id)) {
                            context.getSource().sendFailure(
                                Component.translatable("command.don.playdialogue.unknown", name));
                            return 0;
                        }
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.don.playdialogue.single", name), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("playcutscene")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    Set<Identifier> cutscenes = CutsceneManager.registeredCutscenes();
                    if (cutscenes.isEmpty()) {
                        context.getSource().sendFailure(
                            Component.translatable("command.don.playcutscene.none"));
                        return 0;
                    }
                    for (Identifier id : cutscenes) {
                        CutsceneManager.runEvent(player, id);
                    }
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.don.playcutscene.all", cutscenes.size()), true);
                    return cutscenes.size();
                })
                .then(Commands.argument("cutscene", StringArgumentType.greedyString())
                    .suggests((context, builder) -> {
                        for (Identifier id : CutsceneManager.registeredCutscenes()) {
                            builder.suggest(id.toString());
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var player = (ServerPlayer) context.getSource().getEntity();
                        String name = StringArgumentType.getString(context, "cutscene");
                        Identifier id;
                        try {
                            id = Identifier.parse(name);
                        } catch (Exception e) {
                            context.getSource().sendFailure(
                                Component.translatable("command.don.playcutscene.invalid", name));
                            return 0;
                        }
                        if (!CutsceneManager.runEvent(player, id)) {
                            context.getSource().sendFailure(
                                Component.translatable("command.don.playcutscene.unknown", name));
                            return 0;
                        }
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.don.playcutscene.single", name), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("cheat")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> {
                    var player = (ServerPlayer) context.getSource().getEntity();
                    PlayerUIMenuType.openUI(player, don.CHEAT_SHEET_ID);
                    return 1;
                })
            )
            .then(Commands.literal("givewallwriting")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .requires(source -> source.getEntity() instanceof ServerPlayer)
                    .executes(context -> {
                        var player = (ServerPlayer) context.getSource().getEntity();
                        String raw = StringArgumentType.getString(context, "text");
                        String text = raw.length() > 300 ? raw.substring(0, 300) : raw;
                        var stack = net.multyfora.don.item.WallWritingItem.create(text);
                        if (!player.getInventory().add(stack)) {
                            player.drop(stack, false);
                        }
                        String msg = text;
                        context.getSource().sendSuccess(() -> Component.translatable("command.don.givewallwriting.success", msg), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("journal")
                .then(Commands.literal("reset")
                    .requires(source -> source.getEntity() instanceof ServerPlayer)
                    .executes(context -> {
                        var player = (ServerPlayer) context.getSource().getEntity();
                        JournalEntryManager.resetPlayerProgress(player);
                        context.getSource().sendSuccess(() -> Component.translatable("command.don.journal.reset.success"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("unlock")
                    .then(Commands.argument("entry", StringArgumentType.greedyString())
                        .suggests((c, b) -> {
                            for (Identifier id : JournalEntryManager.registeredEntries()) b.suggest(id.toString());
                            return b.buildFuture();
                        })
                        .executes(context -> {
                            var player = (ServerPlayer) context.getSource().getEntity();
                            String name = StringArgumentType.getString(context, "entry");
                            Identifier id;
                            try { id = Identifier.parse(name); } catch (Exception e) {
                                context.getSource().sendFailure(Component.translatable("command.don.journal.unlock.unknown", name));
                                return 0;
                            }
                            if (JournalEntryManager.getDefinition(id) == null) {
                                context.getSource().sendFailure(Component.translatable("command.don.journal.unlock.unknown", name));
                                return 0;
                            }
                            if (!JournalEntryManager.tryDiscover(player, id)) {
                                context.getSource().sendFailure(Component.translatable("command.don.journal.unlock.already", name));
                                return 0;
                            }
                            JournalEntryManager.syncToPlayer(player);
                            context.getSource().sendSuccess(() -> Component.translatable("command.don.journal.unlock.success", name), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("unlockall")
                    .requires(source -> source.getEntity() instanceof ServerPlayer)
                    .executes(context -> {
                        var player = (ServerPlayer) context.getSource().getEntity();
                        int count = 0;
                        for (Identifier id : JournalEntryManager.registeredEntries()) {
                            if (JournalEntryManager.tryDiscover(player, id)) count++;
                        }
                        if (count > 0) JournalEntryManager.syncToPlayer(player);
                        int finalCount = count;
                        context.getSource().sendSuccess(() -> Component.literal("Unlocked " + finalCount + " entries"), true);
                        return count;
                    })
                )
            )
        );
    }
}
