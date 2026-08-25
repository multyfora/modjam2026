package net.multyfora.modjam;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.multyfora.modjam.client.BrightestInteractionManager;
import net.multyfora.modjam.client.DialogueEventClientHandler;
import net.multyfora.modjam.client.DialogueSystem;
import net.multyfora.modjam.client.FirstContactOverlay;
import net.multyfora.modjam.client.LightDrainClientHandler;
import net.multyfora.modjam.client.MonocleHud;
import net.multyfora.modjam.client.FirstContactTransitionState;
import net.multyfora.modjam.client.ShotBeamImpactOverlay;
import net.multyfora.modjam.client.ShotBeamRenderer;
import net.multyfora.modjam.client.model.MonocleWornLayer;
import net.multyfora.modjam.client.renderer.BrightestEntityRenderer;
import net.multyfora.modjam.client.renderer.LightWeaverRenderer;
import net.multyfora.modjam.network.DialogueEventStartPayload;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.network.FirstContactLeavePayload;
import net.multyfora.modjam.network.LightBeamPayload;
import net.multyfora.modjam.network.OpenBrightestMenuPayload;
import net.multyfora.modjam.network.StartCutscenePayload;
import net.multyfora.modjam.client.cutscene.CutsceneClientController;

@Mod(value = modjam.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class modjamClient {
    public modjamClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.getEventBus().addListener(modjamClient::onRegisterGuiLayers);
        container.getEventBus().addListener(modjamClient::onRegisterClientPayloadHandlers);
        container.getEventBus().addListener(modjamClient::onRegisterEntityRenderers);
        container.getEventBus().addListener(modjamClient::onRegisterStandaloneModels);
        container.getEventBus().addListener(modjamClient::onAddLayers);
    }

    private static void onRegisterStandaloneModels(ModelEvent.RegisterStandalone event) {
        event.register(MonocleWornLayer.MODEL_KEY,
            SimpleUnbakedStandaloneModel.quadCollection(
                Identifier.fromNamespaceAndPath(modjam.MODID, "entity/monocle_worn")));
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            var playerRenderer = event.getPlayerRenderer(skin);
            if (playerRenderer != null) {
                playerRenderer.addLayer(new MonocleWornLayer(playerRenderer));
            }
            var mannequinRenderer = event.getMannequinRenderer(skin);
            if (mannequinRenderer != null) {
                mannequinRenderer.addLayer(new MonocleWornLayer(mannequinRenderer));
            }
        }
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
        ShotBeamImpactOverlay.register(event);
        LightDrainClientHandler.register(event);
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(modjam.MODID, "monocle_hud"),
            (ModularHudLayer) () -> MonocleHud.getInstance().getModularUI()
        );
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
        event.register(StartCutscenePayload.TYPE,
            (payload, context) -> CutsceneClientController.getInstance().start(payload));
        event.register(DialogueEventStartPayload.TYPE,
            (payload, context) -> DialogueEventClientHandler.getInstance().handle(payload));
        event.register(LightBeamPayload.TYPE,
            (payload, context) -> {
                Vec3 start = new Vec3(payload.startX(), payload.startY(), payload.startZ());
                Vec3 dir = new Vec3(payload.dirX(), payload.dirY(), payload.dirZ()).normalize();
                Vec3 muzzle = start.add(0, -ShotBeamRenderer.MUZZLE_DROP, 0);
                ShotBeamRenderer.spawnShot(muzzle, start.add(dir.scale(payload.range())),
                    ShotBeamRenderer.BEAM_WIDTH, ShotBeamRenderer.BEAM_COLOR);
            });
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        modjam.LOGGER.info("HELLO FROM CLIENT SETUP");
        modjam.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
