package net.multyfora.don.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.FlipFrameEvent;
import net.multyfora.don.mixin.GameRendererAccessor;
import net.multyfora.don.mixin.PostChainAccessor;
import net.multyfora.don.mixin.PostPassAccessor;
import net.multyfora.don.don;

import java.nio.ByteBuffer;

@EventBusSubscriber(modid = don.MODID, value = Dist.CLIENT)
public class FirstContactShader {
    private static final Identifier EFFECT_LOCATION =
        Identifier.fromNamespaceAndPath(don.MODID, "first_contact");

    private static boolean loaded = false;
    private static float currentIntensity = 0.0f;
    private static GpuBuffer transitionBuffer = null;
    private static boolean transitionBufferSwapped = false;
    private static GpuBuffer singularityBuffer = null;
    private static boolean singularityBufferSwapped = false;

    public static void setIntensity(float intensity) {
        currentIntensity = intensity;
    }

    @SubscribeEvent
    public static void onFlipFrame(FlipFrameEvent event) {
        SingularityDarknessManager manager = SingularityDarknessManager.getInstance();
        var mc = Minecraft.getInstance();
        CameraRenderState camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        manager.updateScreenData(camera);

        float singularityIntensity = manager.getIntensity();
        if (currentIntensity <= 0.0f && singularityIntensity <= 0.0f) {
            clear();
            return;
        }
        if (!loaded) {
            load();
        }
        updateUniforms();
    }

    private static void load() {
        try {
            var mc = Minecraft.getInstance();
            var accessor = (GameRendererAccessor) mc.gameRenderer;
            accessor.don$setPostEffect(EFFECT_LOCATION);
            loaded = true;
            don.LOGGER.info("Loaded first_contact post shader");
        } catch (Exception e) {
            don.LOGGER.error("Failed to load first_contact post shader", e);
        }
    }

    private static void clear() {
        if (loaded) {
            Minecraft.getInstance().gameRenderer.clearPostEffect();
            loaded = false;
            don.LOGGER.debug("Cleared first_contact post shader");
        }
    }

    private static void updateUniforms() {
        var mc = Minecraft.getInstance();
        var shaderManager = mc.getShaderManager();
        PostChain chain = shaderManager.getPostChain(EFFECT_LOCATION, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) return;

        for (var pass : ((PostChainAccessor) chain).don$getPasses()) {
            var uniforms = ((PostPassAccessor) pass).don$getCustomUniforms();

            if (uniforms.containsKey("FirstContactConfig")) {
                if (!transitionBufferSwapped) {
                    ByteBuffer data = ByteBuffer.allocateDirect(16);
                    data.putFloat(0, currentIntensity);
                    transitionBuffer = RenderSystem.getDevice().createBuffer(
                        () -> EFFECT_LOCATION + "/FirstContactConfig",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_HINT_CLIENT_STORAGE | GpuBuffer.USAGE_MAP_WRITE,
                        data
                    );
                    transitionBufferSwapped = true;
                    don.LOGGER.info("Created mappable FirstContactConfig buffer");
                }

                uniforms.put("FirstContactConfig", transitionBuffer);

                try (var view = transitionBuffer.map(false, true)) {
                    view.data().putFloat(0, currentIntensity);
                }
            }

            if (uniforms.containsKey("SingularityConfig")) {
                var manager = SingularityDarknessManager.getInstance();
                if (!singularityBufferSwapped) {
                    ByteBuffer data = ByteBuffer.allocateDirect(16);
                    SingularityDarknessManager.writeSingularityConfig(data, manager);
                    singularityBuffer = RenderSystem.getDevice().createBuffer(
                        () -> EFFECT_LOCATION + "/SingularityConfig",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_HINT_CLIENT_STORAGE | GpuBuffer.USAGE_MAP_WRITE,
                        data
                    );
                    singularityBufferSwapped = true;
                    don.LOGGER.info("Created mappable SingularityConfig buffer");
                }

                uniforms.put("SingularityConfig", singularityBuffer);

                try (var view = singularityBuffer.map(false, true)) {
                    SingularityDarknessManager.writeSingularityConfig(view.data(), manager);
                }
            }
        }
    }
}