package net.multyfora.modjam.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.multyfora.modjam.modjam;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class FirstContactHandlers {

    @SubscribeEvent
    public static void onFovModifier(ComputeFovModifierEvent event) {
        var state = FirstContactTransitionState.getInstance();
        if (!state.isActive()) return;
        float intensity = state.getIntensity();
        float modified = event.getNewFovModifier() * (1.0f + intensity * 0.5f);
        event.setNewFovModifier(modified);
    }
}
