package net.multyfora.modjam.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
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
import org.joml.Vector4f;

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
                    var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                    RenderSystem.getDevice().createCommandEncoder()
                        .clearColorTexture(target.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
                    return true;
                }
            }
        );
    }
}
