package net.multyfora.modjam.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class LightDrainClientHandler {

    private static final Identifier BLACKOUT_LAYER =
        Identifier.fromNamespaceAndPath(modjam.MODID, "light_drain_blackout");

    public static boolean isActive() {
        var player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(modjam.LIGHT_DRAIN_EFFECT);
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(BLACKOUT_LAYER, LightDrainClientHandler::renderBlackout);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!isActive() || event.getName().equals(BLACKOUT_LAYER)) {
            return;
        }
        event.setCanceled(true);
    }

    private static void renderBlackout(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!isActive()) {
            return;
        }
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFF000000);
    }
}
