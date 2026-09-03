package net.multyfora.don;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.common.NeoForge;
import net.multyfora.don.client.BrightestInteractionManager;
import net.multyfora.don.client.DialogueEventClientHandler;
import net.multyfora.don.client.DialogueSystem;
import net.multyfora.don.client.FirstContactOverlay;
import net.multyfora.don.client.LightDrainClientHandler;
import net.multyfora.don.client.ModJamNoticeScreen;
import net.multyfora.don.client.MonocleHud;
import net.multyfora.don.client.FirstContactTransitionState;
import net.multyfora.don.client.ShotBeamImpactOverlay;
import net.multyfora.don.client.ShotBeamRenderer;
import net.multyfora.don.client.model.MonocleWornLayer;
import net.multyfora.don.client.renderer.BrightestEntityRenderer;
import net.multyfora.don.client.renderer.DisplayBlockRenderer;
import net.multyfora.don.client.renderer.LightWeaverRenderer;
import net.multyfora.don.client.renderer.MysticBrazierRenderer;
import net.multyfora.don.client.renderer.PortableStarRenderer;
import net.multyfora.don.client.renderer.TimeMachineRenderer;
import net.multyfora.don.client.renderer.WallWritingRenderer;
import net.multyfora.don.client.renderer.WeaverGlyphRenderer;
import net.multyfora.don.network.WallWritingReadPayload;
import net.multyfora.don.client.BetrayedClientState;
import net.multyfora.don.client.BrightestVisitationManager;
import net.multyfora.don.client.ClientJournalState;
import net.multyfora.don.network.BetrayedPayload;
import net.multyfora.don.network.DialogueEventStartPayload;
import net.multyfora.don.network.FirstContactEnterPayload;
import net.multyfora.don.network.FirstContactLeavePayload;
import net.multyfora.don.network.JournalSyncPayload;
import net.multyfora.don.network.LabMusicPayload;
import net.multyfora.don.network.LightBeamPayload;
import net.multyfora.don.network.OpenBrightestMenuPayload;
import net.multyfora.don.network.StartCutscenePayload;
import net.multyfora.don.client.LabMusicManager;
import net.multyfora.don.client.cutscene.CutsceneClientController;

@Mod(value = don.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = don.MODID, value = Dist.CLIENT)
public class donClient {
    public donClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.getEventBus().addListener(donClient::onRegisterGuiLayers);
        container.getEventBus().addListener(donClient::onRegisterClientPayloadHandlers);
        container.getEventBus().addListener(donClient::onRegisterEntityRenderers);
        container.getEventBus().addListener(donClient::onRegisterStandaloneModels);
        container.getEventBus().addListener(donClient::onAddLayers);
        NeoForge.EVENT_BUS.addListener(donClient::onScreenOpen);
    }

    private static void onRegisterStandaloneModels(ModelEvent.RegisterStandalone event) {
        event.register(MonocleWornLayer.MODEL_KEY,
            SimpleUnbakedStandaloneModel.quadCollection(
                Identifier.fromNamespaceAndPath(don.MODID, "entity/monocle_worn")));
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
        event.registerEntityRenderer(don.BRIGHTEST_ENTITY.get(), BrightestEntityRenderer::new);
        event.registerEntityRenderer(don.LIGHT_WEAVER_ENTITY.get(), LightWeaverRenderer::new);
        event.registerEntityRenderer(don.WALL_WRITING_ENTITY.get(), WallWritingRenderer::new);
        event.registerEntityRenderer(don.DISPLAY_BLOCK_ENTITY.get(), DisplayBlockRenderer::new);
        event.registerEntityRenderer(don.WEAVER_GLYPH_ENTITY.get(), WeaverGlyphRenderer::new);
        event.registerBlockEntityRenderer(don.PORTABLE_STAR_BLOCK_ENTITY.get(), PortableStarRenderer::new);
        event.registerBlockEntityRenderer(don.TIME_MACHINE_BLOCK_ENTITY.get(), TimeMachineRenderer::new);
        event.registerBlockEntityRenderer(don.MYSTIC_BRAZIER_BLOCK_ENTITY.get(), MysticBrazierRenderer::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(don.MODID, "dialogue"),
            (ModularHudLayer) () -> DialogueSystem.getInstance().getModularUI()
        );
        FirstContactOverlay.register(event);
        ShotBeamImpactOverlay.register(event);
        LightDrainClientHandler.register(event);
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(don.MODID, "monocle_hud"),
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
        event.register(JournalSyncPayload.TYPE,
            (payload, context) -> ClientJournalState.getInstance().handle(payload));
        event.register(WallWritingReadPayload.TYPE,
            (payload, context) -> DialogueSystem.getInstance().playMarkup(java.util.List.of(payload.plain()), null));
        event.register(LabMusicPayload.TYPE,
            (payload, context) -> LabMusicManager.getInstance().handlePayload(payload.play()));
        event.register(BetrayedPayload.TYPE,
            (payload, context) -> {
                BetrayedClientState.setBetrayed(true);
                LabMusicManager.getInstance().handlePayload(false);
                DialogueEventClientHandler.getInstance().clear();
                DialogueSystem.getInstance().clear();
                BrightestVisitationManager.getInstance().end();
            });
    }

    private static void onScreenOpen(ScreenEvent.Opening event) {
        if (ModJamNoticeScreen.hasBeenShown()) return;
        if (!(event.getNewScreen() instanceof TitleScreen)) return;
        ModJamNoticeScreen.showOnce();
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        don.LOGGER.info("HELLO FROM CLIENT SETUP");
        don.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}