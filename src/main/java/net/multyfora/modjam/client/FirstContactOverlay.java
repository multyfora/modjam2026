package net.multyfora.modjam.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.multyfora.modjam.modjam;
import net.minecraft.client.DeltaTracker;

public class FirstContactOverlay {

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact_transition"),
            FirstContactOverlay::renderOverlay
        );
    }

    private static void renderOverlay(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        var state = FirstContactTransitionState.getInstance();
        float intensity = state.getIntensity();
        if (intensity <= 0.0f) return;

        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();

        if (intensity > 0.92f) {
            float flash = (intensity - 0.92f) / 0.08f;
            int fa = (int)(flash * 60) << 24 | 0xFFFFFF;
            guiGraphics.fill(0, 0, w, h, fa);
        }
    }
}
