package net.multyfora.modjam.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.block.PortableStarBlockEntity;
import net.multyfora.modjam.modjam;

public class PortableStarModel extends GeoModel<PortableStarBlockEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(modjam.MODID, "block/portable_star");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(modjam.MODID, "textures/block/portable_star.png");
    }

    @Override
    public Identifier getAnimationResource(PortableStarBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(modjam.MODID, "block/portable_star");
    }
}
