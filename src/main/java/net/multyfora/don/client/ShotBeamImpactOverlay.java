package net.multyfora.don.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = don.MODID, value = Dist.CLIENT)
public class ShotBeamImpactOverlay {

    private static final float PEAK_ALPHA = 0.9f;
    private static final float DECAY_PER_TICK = 0.82f;

    private static float strength = 0.0f;

    private ShotBeamImpactOverlay() {
    }

    public static void trigger(float amount) {
        strength = Math.max(strength, amount);
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(don.MODID, "beam_impact_flash"),
            ShotBeamImpactOverlay::renderOverlay
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        strength *= DECAY_PER_TICK;
        if (strength < 0.01f) {
            strength = 0.0f;
        }
    }

    private static void renderOverlay(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (strength <= 0.0f) {
            return;
        }
        int alpha = Math.min(255, Math.round(255 * PEAK_ALPHA * strength));
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), alpha << 24 | 0x00FFF3C0);
    }
}