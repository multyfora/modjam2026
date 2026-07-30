package net.multyfora.modjam.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("postEffectId")
    Identifier modjam$getPostEffectId();

    @Accessor("postEffectId")
    void modjam$setPostEffectId(Identifier id);

    @Invoker("setPostEffect")
    void modjam$setPostEffect(Identifier id);
}
