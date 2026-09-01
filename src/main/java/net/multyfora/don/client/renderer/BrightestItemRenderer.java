package net.multyfora.don.client.renderer;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;

import net.multyfora.don.item.BrightestItem;

public class BrightestItemRenderer extends GeoItemRenderer<BrightestItem> {
    public BrightestItemRenderer() {
        super(new BrightestItemModel());
        withScale(1.5f);
    }

    @Override
    public void captureDefaultRenderState(BrightestItem animatable, GeoItemRenderer.RenderData renderData, GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, renderData, renderState, partialTick);
        var level = Minecraft.getInstance().level;
        double time = level != null ? level.getGameTime() + partialTick : System.currentTimeMillis() / 50.0;
        renderState.addGeckolibData(DataTickets.TICK, time);
    }
}
