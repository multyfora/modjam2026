package net.multyfora.modjam.client.godray;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public final class GodrayPipelines {

    private static final Identifier BEAM_LOCATION = Identifier.parse("modjam:godray/beam");

    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation(BEAM_LOCATION)
            .withVertexShader(Identifier.parse("modjam:core/godray_beam"))
            .withFragmentShader(Identifier.parse("modjam:core/godray_beam"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(false)
            .build();

    public static final RenderType BEAM = RenderType.create("modjam_godray_beam", RenderSetup.builder(PIPELINE).createRenderSetup());

    @SubscribeEvent
    public static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    private GodrayPipelines() {}
}