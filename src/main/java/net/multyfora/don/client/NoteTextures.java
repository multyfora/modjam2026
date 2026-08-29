package net.multyfora.don.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class NoteTextures {

    public static final Identifier SQUARE = Identifier.fromNamespaceAndPath("don", "textures/gui/grey_sticknote.png");
    public static final Identifier SKEWED = Identifier.fromNamespaceAndPath("don", "textures/gui/grey_sticknote_skewed.png");

    private static final float BRIGHTEN = 2.6f;
    private static final Map<CacheKey, Identifier> CACHE = new HashMap<>();

    private NoteTextures() {
    }

    private record CacheKey(int color, boolean skewed) {
    }

    public static Identifier get(int color, boolean skewed) {
        return CACHE.computeIfAbsent(new CacheKey(color, skewed), NoteTextures::create);
    }

    private static Identifier create(CacheKey key) {
        var source = key.skewed() ? SKEWED : SQUARE;
        try (var stream = Minecraft.getInstance().getResourceManager().getResource(source)
                .orElseThrow(() -> new IOException("Missing texture " + source)).open();
             var image = NativeImage.read(stream)) {
            var out = new NativeImage(NativeImage.Format.RGBA, image.getWidth(), image.getHeight(), true);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int argb = image.getPixel(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha == 0) continue;
                    int gray = argb & 0xFF;
                    int r = (int) Math.min(255f, gray * BRIGHTEN * (((key.color() >>> 16) & 0xFF) / 255f));
                    int g = (int) Math.min(255f, gray * BRIGHTEN * (((key.color() >>> 8) & 0xFF) / 255f));
                    int b = (int) Math.min(255f, gray * BRIGHTEN * ((key.color() & 0xFF) / 255f));
                    out.setPixel(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
                }
            }
            var identifier = Identifier.fromNamespaceAndPath("don",
                    "sticknote_" + Integer.toHexString(key.color()) + (key.skewed() ? "_skewed" : ""));
            Minecraft.getInstance().getTextureManager().register(identifier, new DynamicTexture(() -> "don_sticknote", out));
            return identifier;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create tinted sticky note texture", e);
        }
    }
}
