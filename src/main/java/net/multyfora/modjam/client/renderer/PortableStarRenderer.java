package net.multyfora.modjam.client.renderer;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.multyfora.modjam.block.PortableStarBlockEntity;

public class PortableStarRenderer extends GeoBlockRenderer<PortableStarBlockEntity, net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> {
    public PortableStarRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new PortableStarModel());
    }
}
