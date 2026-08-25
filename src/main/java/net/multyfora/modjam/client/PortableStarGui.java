package net.multyfora.modjam.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.multyfora.modjam.block.PortableStarBlockEntity;
import net.multyfora.modjam.network.SetStarMysticalPayload;

public final class PortableStarGui {

    private final BlockPos pos;
    private double value;

    private PortableStarGui(BlockPos pos, double value) {
        this.pos = pos;
        this.value = value;
    }

    public static void open(BlockPos pos, double current) {
        PortableStarGui gui = new PortableStarGui(pos, current);
        Minecraft.getInstance().setScreenAndShow(new ModularUIScreen(gui.createUI(), Component.empty()));
    }

    private ModularUI createUI() {
        var title = new Label();
        title.setText(Component.literal("Mystical Tuning"));
        title.textStyle(ts -> ts.textColor(0xFFFFD700).textShadow(false).fontSize(12));

        var field = new TextField();
        field.setNumbersOnlyDouble(PortableStarBlockEntity.MIN_MYSTICAL, PortableStarBlockEntity.MAX_MYSTICAL);
        field.setText(format(value));
        field.setTextResponder(s -> {
            try {
                value = Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) { }
        });
        field.layout(l -> l.width(150).height(20));

        var adjustRow = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(4).alignItems(AlignItems.CENTER));
        for (double step : new double[] {-1.0, -0.1, 0.1, 1.0}) {
            var button = new Button().setText((step > 0 ? "+" : "") + trimNumber(step));
            button.layout(l -> l.width(34).height(18));
            button.textStyle(ts -> ts.textShadow(false).fontSize(9));
            button.addEventListener(UIEvents.CLICK, e -> adjust(step, field));
            adjustRow.addChild(button);
        }

        var apply = new Button().setText("Apply");
        apply.layout(l -> l.width(150).height(20));
        apply.textStyle(ts -> ts.textShadow(false));
        apply.addEventListener(UIEvents.CLICK, e -> apply());

        var panel = new UIElement()
                .layout(l -> l.width(190).flexDirection(FlexDirection.COLUMN)
                        .alignItems(AlignItems.CENTER).gapAll(8).paddingAll(10))
                .style(s -> s.background(SDFRectTexture.of(0xE2170803).setRadius(8f).setBorderColor(0xFFD4A840)))
                .addChildren(title, field, adjustRow, apply);

        var root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .addChild(panel);

        return ModularUI.of(UI.of(root));
    }

    private void adjust(double step, TextField field) {
        value = Math.clamp(value + step, PortableStarBlockEntity.MIN_MYSTICAL, PortableStarBlockEntity.MAX_MYSTICAL);
        field.setText(format(value));
    }

    private void apply() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new SetStarMysticalPayload(pos.getX(), pos.getY(), pos.getZ(), value).toVanillaServerbound());
        }
        Minecraft.getInstance().player.closeContainer();
    }

    private static String format(double value) {
        return trimNumber(value);
    }

    private static String trimNumber(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        if (rounded == Math.floor(rounded) && !Double.isInfinite(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.valueOf(rounded);
    }
}
