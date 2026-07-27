package net.multyfora.modjam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.multyfora.modjam.client.DialogueSystem;
import net.multyfora.modjam.modjam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class ModJamClientCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("modjam")
            .then(Commands.literal("dialogue")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(context -> {
                        String text = StringArgumentType.getString(context, "text");
                        DialogueSystem.getInstance().showDialogue(text);
                        return 1;
                    })
                )
            )
        );
    }
}
