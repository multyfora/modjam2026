package net.multyfora.modjam.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.item.SealedSingularityItem;
import net.multyfora.modjam.modjam;

public class SealedSingularityModel extends GeoModel<SealedSingularityItem> {
    private static boolean charged = false;

    public static void setCharged(boolean isCharged) {
        charged = isCharged;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(modjam.MODID,
            charged ? "item/charged_sealed_singularity" : "item/sealed_singularity");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(modjam.MODID,
            charged ? "textures/item/charged_selaed.png" : "textures/item/sealed_singularity.png");
    }

    @Override
    public Identifier getAnimationResource(SealedSingularityItem animatable) {
        return Identifier.fromNamespaceAndPath(modjam.MODID,
            charged ? "entity/charged_sealed_singularity" : "entity/sealed_singularity");
    }
}
