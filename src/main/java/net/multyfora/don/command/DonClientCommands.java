package net.multyfora.don.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.multyfora.don.client.DialogueSystem;
import net.multyfora.don.client.ModJamNoticeScreen;
import net.multyfora.don.don;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
@EventBusSubscriber(modid = don.MODID, value = Dist.CLIENT)
public class DonClientCommands {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("don")
                .then(Commands.literal("dialogue")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String text = StringArgumentType.getString(context, "text");
                                    DialogueSystem.getInstance().showDialogue(text);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("modjam")
                        .executes(context -> {
                            ModJamNoticeScreen.forceShow();
                            return 1;
                        })
                )
        );
    }
}