package net.multyfora.modjam.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.world.entity.LightWeaverEntity;

public class LightWeaverModel extends GeoModel<LightWeaverEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(modjam.MODID, "entity/light_weaver");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(modjam.MODID, "textures/entity/light_weaver.png");
    }

    @Override
    public Identifier getAnimationResource(LightWeaverEntity animatable) {
        return null;
    }
}
