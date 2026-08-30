package net.multyfora.don.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.don.block.MysticBrazierBlockEntity;
import net.multyfora.don.don;

public class MysticBrazierModel extends GeoModel<MysticBrazierBlockEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "block/mystical_brazier");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID, "textures/block/basin.png");
    }

    @Override
    public Identifier getAnimationResource(MysticBrazierBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(don.MODID, "block/mystical_brazier");
    }
}
