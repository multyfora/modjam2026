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
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.multyfora.modjam.client.BrightestInteractionManager;
import net.multyfora.modjam.client.DialogueEventClientHandler;
import net.multyfora.modjam.client.DialogueSystem;
import net.multyfora.modjam.client.FirstContactOverlay;
import net.multyfora.modjam.client.FirstContactTransitionState;
import net.multyfora.modjam.client.renderer.BrightestEntityRenderer;
import net.multyfora.modjam.client.renderer.LightWeaverRenderer;
import net.multyfora.modjam.network.DialogueEventStartPayload;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.network.FirstContactLeavePayload;
import net.multyfora.modjam.network.OpenBrightestMenuPayload;

@Mod(value = modjam.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class modjamClient {
    public modjamClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.getEventBus().addListener(modjamClient::onRegisterGuiLayers);
        container.getEventBus().addListener(modjamClient::onRegisterClientPayloadHandlers);
        container.getEventBus().addListener(modjamClient::onRegisterEntityRenderers);
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(modjam.BRIGHTEST_ENTITY.get(), BrightestEntityRenderer::new);
        event.registerEntityRenderer(modjam.LIGHT_WEAVER_ENTITY.get(), LightWeaverRenderer::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(modjam.MODID, "dialogue"),
            (ModularHudLayer) () -> DialogueSystem.getInstance().getModularUI()
        );
        FirstContactOverlay.register(event);
    }

    private static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(FirstContactLeavePayload.TYPE,
            (payload, context) -> {
                DialogueEventClientHandler.getInstance().clear();
                DialogueSystem.getInstance().clear();
                FirstContactTransitionState.getInstance().startLeaving();
            });
        event.register(FirstContactEnterPayload.TYPE,
            (payload, context) -> FirstContactTransitionState.getInstance().startEntering());
        event.register(OpenBrightestMenuPayload.TYPE,
            (payload, context) -> BrightestInteractionManager.getInstance().openMenu());
        event.register(DialogueEventStartPayload.TYPE,
            (payload, context) -> DialogueEventClientHandler.getInstance().handle(payload));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        modjam.LOGGER.info("HELLO FROM CLIENT SETUP");
        modjam.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
