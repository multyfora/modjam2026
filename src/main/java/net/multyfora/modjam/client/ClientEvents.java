package net.multyfora.modjam.client;

import net.multyfora.modjam.modjam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ItemActivationManager.getInstance().tick();
        BrightestVisitationManager.getInstance().tick();
        DialogueSystem.getInstance().tick();
        DialogueEventClientHandler.getInstance().tick();
        FirstContactTransitionState.getInstance().tick();
        BrightestInteractionManager.getInstance().tick();
        FirstContactMusicManager.getInstance().tick();
        SingularityDarknessManager.getInstance().tick();
        FirstContactShader.setIntensity(FirstContactTransitionState.getInstance().getIntensity());
    }
}
