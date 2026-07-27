package net.multyfora.modjam;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.multyfora.modjam.client.DialogueSystem;

@Mod(value = modjam.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class modjamClient {
    public modjamClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.getEventBus().addListener(modjamClient::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.parse(modjam.MODID + ":dialogue"),
            (ModularHudLayer) () -> DialogueSystem.getInstance().getModularUI()
        );
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        modjam.LOGGER.info("HELLO FROM CLIENT SETUP");
        modjam.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
