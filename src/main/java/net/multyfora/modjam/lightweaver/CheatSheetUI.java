package net.multyfora.modjam.lightweaver;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.world.entity.player.Player;
import net.multyfora.modjam.lightweaver.LightWeaverShapes.WeaverShape;

public final class CheatSheetUI {

    private static final int OUTER_GOLD = 0xFF6B4A20;
    private static final int GOLD_BORDER = 0xFFD4A840;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int INNER_STONE = 0xFF3A2410;
    private static final int DIALOG_BG = 0xE2170803;
    private static final int CELL_FILLED = 0xFFD4A840;
    private static final int LABEL_TEXT = 0xFFB0A088;

    public static ModularUI create(Player player) {
        UIElement header = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(6).alignItems(AlignItems.CENTER))
                .addChild(new Label().setText("Infusion Patterns").textStyle(s -> s.textColor(GOLD_BORDER).adaptiveWidth(true)));

        UIElement sheet = new UIElement()
                .layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).wrap(FlexWrap.WRAP)
                        .justifyContent(AlignContent.CENTER).gapAll(4));
        for (WeaverShape shape : LightWeaverShapes.SHAPES) {
            sheet.addChild(entry(shape));
        }

        UIElement panel = new UIElement()
                .layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(10).paddingAll(14).alignItems(AlignItems.CENTER))
                .style(s -> s.background(SDFRectTexture.of(DIALOG_BG).setRadius(10f).setBorderColor(0x55D4A840)))
                .addChild(header)
                .addChild(ornament())
                .addChild(sheet);

        UIElement inner = new UIElement()
                .layout(l -> l.widthPercent(100).paddingAll(2))
                .style(s -> s.background(SDFRectTexture.of(INNER_STONE).setRadius(12f).setBorderColor(DARK_GOLD)))
                .addChild(panel);

        UIElement dialogBox = new UIElement()
                .layout(l -> l.widthPercent(60).flexDirection(FlexDirection.COLUMN))
                .addChild(inner);

        UIElement root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .addChild(dialogBox);

        return new ModularUI(UI.of(root), player);
    }

    private static UIElement entry(WeaverShape shape) {
        UIElement entry = new UIElement()
                .layout(l -> l.width(52).flexDirection(FlexDirection.COLUMN).gapAll(2).alignItems(AlignItems.CENTER))
                .style(s -> s.background(SDFRectTexture.of(INNER_STONE).setRadius(3f).setBorderColor(DARK_GOLD)))
                .addChild(preview(shape.pattern()));
        entry.addChild(new Label()
                .setText(shape.displayName() + " (T" + shape.tier() + ")")
                .textStyle(s -> s.fontSize(8).lineSpacing(0).textColor(LABEL_TEXT)
                        .textWrap(TextWrap.WRAP).adaptiveHeight(true).textAlignHorizontal(Horizontal.CENTER))
                .layout(l -> l.width(48)));
        return entry;
    }

    private static UIElement preview(boolean[] pattern) {
        UIElement preview = new UIElement().layout(l -> l.flexDirection(FlexDirection.COLUMN).paddingAll(3));
        for (int row = 0; row < LightWeaverShapes.GRID_SIZE; row++) {
            UIElement line = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW));
            for (int col = 0; col < LightWeaverShapes.GRID_SIZE; col++) {
                UIElement cell = new UIElement().layout(l -> l.width(3).height(3));
                if (pattern[row * LightWeaverShapes.GRID_SIZE + col]) {
                    cell.style(s -> s.background(SDFRectTexture.of(CELL_FILLED).setRadius(0f)));
                }
                line.addChild(cell);
            }
            preview.addChild(line);
        }
        return preview;
    }

    private static UIElement ornament() {
        var wrap = new UIElement()
                .layout(l -> l.widthPercent(100).height(8)
                        .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));

        var lineLeft = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        var diamond = new UIElement()
                .layout(l -> l.width(5).height(5))
                .style(s -> s.background(SDFRectTexture.of(GOLD_BORDER).setRadius(1f)));
        diamond.transform(t -> t.rotation(45f));

        var lineRight = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        wrap.addChildren(lineLeft, diamond, lineRight);
        return wrap;
    }

    private CheatSheetUI() {
    }
}
