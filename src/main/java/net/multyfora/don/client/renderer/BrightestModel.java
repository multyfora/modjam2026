package net.multyfora.don.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;
import net.multyfora.don.world.entity.BrightestEntity;

public class BrightestModel extends GeoModel<BrightestEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "entity/brightest");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "textures/entity/brightest.png");
    }

    @Override
    public Identifier getAnimationResource(BrightestEntity animatable) {
        return Identifier.fromNamespaceAndPath(don.MODID, "entity/brightest");
    }
}
