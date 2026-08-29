package net.multyfora.don.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.don.don;
import net.multyfora.don.world.entity.LightWeaverEntity;

public class LightWeaverModel extends GeoModel<LightWeaverEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "entity/light_weaver");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "textures/entity/light_weaver.png");
    }

    @Override
    public Identifier getAnimationResource(LightWeaverEntity animatable) {
        return Identifier.fromNamespaceAndPath(don.MODID, "entity/light_weaver");
    }
}
