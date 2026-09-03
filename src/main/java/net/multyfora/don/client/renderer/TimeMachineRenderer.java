package net.multyfora.don.client.renderer;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.multyfora.don.block.TimeMachineBlockEntity;

public class TimeMachineRenderer extends GeoBlockRenderer<TimeMachineBlockEntity, net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> {
    public TimeMachineRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new TimeMachineModel());
    }
}
