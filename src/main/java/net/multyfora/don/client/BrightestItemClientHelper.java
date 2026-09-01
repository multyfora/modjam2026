package net.multyfora.don.client;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoItemRenderer;
import net.multyfora.don.client.renderer.BrightestItemRenderer;

import java.util.function.Consumer;

public final class BrightestItemClientHelper {
    private BrightestItemClientHelper() {}

    public static void createRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BrightestItemRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new BrightestItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
