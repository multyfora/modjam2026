package net.multyfora.modjam.client;

import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.multyfora.modjam.modjam;
import org.joml.Matrix4fc;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class FirstContactEffects {

    @SubscribeEvent
    public static void onRegisterCustomEffects(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(
            Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact"),
            new CustomSkyboxRenderer() {
                @Override
                public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState,
                                         Matrix4fc matrix4fc, Runnable runDefault) {
                    return true;
                }
            }
        );
    }
}
