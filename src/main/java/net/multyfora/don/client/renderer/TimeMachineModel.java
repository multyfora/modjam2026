package net.multyfora.don.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.don.block.TimeMachineBlockEntity;
import net.multyfora.don.don;

public class TimeMachineModel extends GeoModel<TimeMachineBlockEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "block/portable_star");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "textures/block/portable_star.png");
    }

    @Override
    public Identifier getAnimationResource(TimeMachineBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(don.MODID, "block/portable_star");
    }
}
