package net.multyfora.don.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("postEffectId")
    Identifier don$getPostEffectId();

    @Accessor("postEffectId")
    void don$setPostEffectId(Identifier id);

    @Invoker("setPostEffect")
    void don$setPostEffect(Identifier id);
}
