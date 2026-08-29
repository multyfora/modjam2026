package net.multyfora.don.client.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.multyfora.don.item.SealedSingularityItem;
import net.multyfora.don.don;

public class SealedSingularityModel extends GeoModel<SealedSingularityItem> {
    private static boolean charged = false;

    public static void setCharged(boolean isCharged) {
        charged = isCharged;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID,
            charged ? "item/charged_sealed_singularity" : "item/sealed_singularity");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(don.MODID,
            charged ? "textures/item/charged_selaed.png" : "textures/item/sealed_singularity.png");
    }

    @Override
    public Identifier getAnimationResource(SealedSingularityItem animatable) {
        return Identifier.fromNamespaceAndPath(don.MODID,
            charged ? "entity/charged_sealed_singularity" : "entity/sealed_singularity");
    }
}
