package net.multyfora.modjam.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class LightWeaverRenderState extends LivingEntityRenderState {
    public final ItemStackRenderState orbitItem = new ItemStackRenderState();
    public final ItemStackRenderState orbitPaper = new ItemStackRenderState();
    public int orbitCount;
    public int paperCount;
}
