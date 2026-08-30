package net.multyfora.don.client;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoItemRenderer;
import net.multyfora.don.client.renderer.SealedSingularityRenderer;

import java.util.function.Consumer;

public final class SealedSingularityClientHelper {
    private SealedSingularityClientHelper() {}

    public static void createRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SealedSingularityRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new SealedSingularityRenderer();
                }
                return this.renderer;
            }
        });
    }
}
