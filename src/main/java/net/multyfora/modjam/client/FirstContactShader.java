package net.multyfora.modjam.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.FlipFrameEvent;
import net.multyfora.modjam.mixin.GameRendererAccessor;
import net.multyfora.modjam.mixin.PostChainAccessor;
import net.multyfora.modjam.mixin.PostPassAccessor;
import net.multyfora.modjam.modjam;

import java.nio.ByteBuffer;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class FirstContactShader {
    private static final Identifier EFFECT_LOCATION =
        Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact");

    private static boolean loaded = false;
    private static float currentIntensity = 0.0f;
    private static GpuBuffer mappableBuffer = null;
    private static boolean bufferSwapped = false;

    public static void setIntensity(float intensity) {
        currentIntensity = intensity;
    }

    @SubscribeEvent
    public static void onFlipFrame(FlipFrameEvent event) {
        if (currentIntensity <= 0.0f) {
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
            accessor.modjam$setPostEffect(EFFECT_LOCATION);
            loaded = true;
            modjam.LOGGER.info("Loaded first_contact post shader");
        } catch (Exception e) {
            modjam.LOGGER.error("Failed to load first_contact post shader", e);
        }
    }

    private static void clear() {
        if (loaded) {
            Minecraft.getInstance().gameRenderer.clearPostEffect();
            loaded = false;
            modjam.LOGGER.debug("Cleared first_contact post shader");
        }
    }

    private static void updateUniforms() {
        var mc = Minecraft.getInstance();
        var shaderManager = mc.getShaderManager();
        PostChain chain = shaderManager.getPostChain(EFFECT_LOCATION, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) return;

        for (var pass : ((PostChainAccessor) chain).modjam$getPasses()) {
            var uniforms = ((PostPassAccessor) pass).modjam$getCustomUniforms();
            if (!uniforms.containsKey("FirstContactConfig")) continue;

            if (!bufferSwapped) {
                ByteBuffer data = ByteBuffer.allocateDirect(16);
                data.putFloat(0, currentIntensity);
                mappableBuffer = RenderSystem.getDevice().createBuffer(
                    () -> EFFECT_LOCATION + "/FirstContactConfig",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_HINT_CLIENT_STORAGE | GpuBuffer.USAGE_MAP_WRITE,
                    data
                );
                bufferSwapped = true;
                modjam.LOGGER.info("Created mappable FirstContactConfig buffer");
            }

            uniforms.put("FirstContactConfig", mappableBuffer);

            try (var view = mappableBuffer.map(false, true)) {
                view.data().putFloat(0, currentIntensity);
            }
        }
    }
}
