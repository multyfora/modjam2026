package net.multyfora.don.lightweaver;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class WeaverPaper {

    public static final String PATTERN_KEY = "don:weaver_pattern";

    private WeaverPaper() {
    }

    public static boolean isPaper(ItemStack stack) {
        return stack.is(Items.PAPER);
    }

    @Nullable
    public static String readPattern(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(PATTERN_KEY)) return null;
        String packed = data.copyTag().getStringOr(PATTERN_KEY, "");
        return LightWeaverShapes.isValidPacked(packed) ? packed : null;
    }

    public static void writePattern(ItemStack stack, String packed) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(PATTERN_KEY, packed));
    }
}