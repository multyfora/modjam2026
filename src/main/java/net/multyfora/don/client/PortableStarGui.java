package net.multyfora.don.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.multyfora.don.block.PortableStarBlockEntity;
import net.multyfora.don.network.SetStarMysticalPayload;
import org.lwjgl.glfw.GLFW;

public final class PortableStarGui {

    private static final int OUTER_GOLD = 0xFF6B4A20;
    private static final int GOLD_BORDER = 0xFFD4A840;
    private static final int INNER_STONE = 0xFF3A2410;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int DEEP_BG = 0xFF120A04;
    private static final int BUTTON_TEXT = 0xFFFFF3D6;
    private static final int VIGNETTE = 0xB0000000;
    private static final int PANEL_WIDTH = 240;

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
        var field = new TextField();
        field.setNumbersOnlyDouble(PortableStarBlockEntity.MIN_MYSTICAL, PortableStarBlockEntity.MAX_MYSTICAL);
        field.setText(format(value));
        field.setTextResponder(s -> {
            try {
                value = Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) { }
        });
        field.layout(l -> l.width(170).height(20));
        field.style(s -> s.background(SDFRectTexture.of(0xFF1A0A03).setRadius(4f).setBorderColor(DARK_GOLD)));

        var confirm = new Button().setText(Component.literal("Confirm"));
        confirm.layout(l -> l.width(170).height(22));
        confirm.textStyle(ts -> ts.textColor(BUTTON_TEXT).textShadow(true).fontSize(11));
        confirm.style(s -> s.background(SDFRectTexture.of(0xCC3A2410).setRadius(6f).setBorderColor(DARK_GOLD)));
        confirm.buttonStyle(s -> s
                .baseTexture(SDFRectTexture.of(0xCC3A2410).setRadius(6f).setBorderColor(DARK_GOLD))
                .hoverTexture(SDFRectTexture.of(0xCC8B6914).setRadius(6f).setBorderColor(0xFFFFD700))
                .pressedTexture(SDFRectTexture.of(0xCC5C3A00).setRadius(6f).setBorderColor(GOLD_BORDER)));
        confirm.addEventListener(UIEvents.CLICK, e -> apply());

        var panel = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto().flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER).paddingAll(16).gapAll(10))
                .style(s -> s.background(SDFRectTexture.of(DEEP_BG).setRadius(6f)))
                .addChildren(field, confirm);

        var innerBezel = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto().paddingAll(2))
                .style(s -> s.background(SDFRectTexture.of(INNER_STONE).setRadius(8f).setBorderColor(DARK_GOLD)))
                .addChild(panel);

        var bezel = new UIElement()
                .layout(l -> l.width(PANEL_WIDTH).heightAuto().paddingAll(3))
                .style(s -> s.background(SDFRectTexture.of(OUTER_GOLD).setRadius(10f).setBorderColor(GOLD_BORDER)))
                .addChild(innerBezel);

        var root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN).justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .style(s -> s.background(new ColorRectTexture(VIGNETTE)))
                .addChild(bezel);

        root.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                Minecraft.getInstance().gui.setScreen(null);
                e.stopPropagation();
            } else if (e.keyCode == GLFW.GLFW_KEY_E || e.keyCode == GLFW.GLFW_KEY_ENTER || e.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                apply();
                e.stopPropagation();
            }
        }, true);

        boolean[] focusedOnce = {false};
        root.addEventListener(UIEvents.TICK, e -> {
            if (!focusedOnce[0]) {
                field.focus();
                focusedOnce[0] = true;
            }
        });

        var mui = ModularUI.of(UI.of(root));
        mui.shouldCloseOnEsc(false);
        mui.shouldCloseOnKeyInventory(false);
        return mui;
    }

    private void apply() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            double clamped = Math.clamp(value, PortableStarBlockEntity.MIN_MYSTICAL, PortableStarBlockEntity.MAX_MYSTICAL);
            connection.send(new SetStarMysticalPayload(pos.getX(), pos.getY(), pos.getZ(), clamped).toVanillaServerbound());
        }
        Minecraft.getInstance().gui.setScreen(null);
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
