package net.multyfora.don.client;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class StickyNoteEditor {

    public static final int MAX_NOTE_CHARS = 32;

    public static final int YELLOW = 0xFFFFD54F;
    public static final int PINK = 0xFFF48FB1;
    public static final int GREEN = 0xFFA5D6A7;
    public static final int BLUE = 0xFF90CAF9;

    public static final int INK_BROWN = 0xFF3E2723;
    public static final int INK_BLACK = 0xFF1A1A1A;
    public static final int INK_RED = 0xFFC62828;
    public static final int INK_BLUE = 0xFF1565C0;

    private static final List<Integer> PRESETS = List.of(YELLOW, PINK, GREEN, BLUE);
    private static final List<Integer> INK_PRESETS = List.of(INK_BROWN, INK_BLACK, INK_RED, INK_BLUE);
    private static final float THIN = 0.02f;
    private static final float NORMAL = 0.03f;
    private static final float THICK = 0.045f;

    private final Consumer<StickyNote> onStick;
    private final StrokeCanvas canvas = StrokeCanvas.editable();
    private final TextArea textArea = new LimitedTextArea(MAX_NOTE_CHARS);
    private final UIElement preview = new UIElement();
    private int color = YELLOW;

    public StickyNoteEditor(Consumer<StickyNote> onStick) {
        this.onStick = onStick;
    }

    public UIElement build() {
        preview.layout(l -> l
                .width(90)
                .height(90)
                .positionType(TaffyPosition.RELATIVE)
        );
        updatePreviewTexture();

        canvas.layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .leftPercent(12)
                .topPercent(12)
                .widthPercent(76)
                .heightPercent(76)
        );
        preview.addChild(canvas);

        var previewWrap = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER)
                );
        previewWrap.addChild(preview);

        var title = new Label();
        title.setText(Component.literal("Sticky Note"));
        title.textStyle(ts -> ts.textColor(0xFFFFE0B2).textShadow(false).fontSize(10));

        textArea.layout(l -> l.widthPercent(100).height(64));
        textArea.textAreaStyle(ts -> ts.fontSize(7).textColor(0xFF3E2723).textShadow(false));

        var colorRow = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(3)
                        .alignItems(AlignItems.CENTER)
                );
        for (int preset : PRESETS) {
            colorRow.addChild(colorButton(preset));
        }

        var inkLabel = smallLabel("Ink");
        var inkRow = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(3)
                        .alignItems(AlignItems.CENTER)
                );
        for (int preset : INK_PRESETS) {
            inkRow.addChild(inkButton(preset));
        }

        var widthLabel = smallLabel("Width");
        var widthRow = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(3)
                        .alignItems(AlignItems.CENTER)
                );
        widthRow.addChild(widthButton("S", THIN));
        widthRow.addChild(widthButton("M", NORMAL));
        widthRow.addChild(widthButton("L", THICK));

        var clearButton = new Button().setText("Clear");
        clearButton.layout(l -> l.flex(1));
        clearButton.textStyle(ts -> ts.textShadow(false));
        clearButton.addEventListener(UIEvents.CLICK, e -> canvas.clearStrokes());

        var stickButton = new Button().setText("Stick it!");
        stickButton.layout(l -> l.flex(1));
        stickButton.textStyle(ts -> ts.textShadow(false));
        stickButton.addEventListener(UIEvents.CLICK, e -> onStick.accept(draft()));

        var buttonRow = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(4)
                );
        buttonRow.addChildren(clearButton, stickButton);

        var panel = new UIElement()
                .layout(l -> l
                        .width(120)
                        .heightAuto()
                        .flexDirection(FlexDirection.COLUMN)
                        .gapAll(4)
                        .paddingAll(6)
                )
                .style(s -> s.background(new ColorRectTexture(0xE62B1A0D)));
        panel.addChildren(title, previewWrap, textArea, colorRow, inkLabel, inkRow, widthLabel, widthRow, buttonRow);
        return panel;
    }

    public void reset() {
        canvas.clearStrokes();
        textArea.setValue(new String[]{""});
        color = YELLOW;
        updatePreviewTexture();
    }

    private Button colorButton(int preset) {
        var button = new Button().noText();
        button.layout(l -> l.width(18).height(18));
        button.buttonStyle(bs -> bs
                .baseTexture(new ColorRectTexture(preset))
                .hoverTexture(new ColorRectTexture(lighter(preset)))
        );
        button.addEventListener(UIEvents.CLICK, e -> {
            color = preset;
            updatePreviewTexture();
        });
        return button;
    }

    private Button inkButton(int preset) {
        var button = new Button().noText();
        button.layout(l -> l.width(18).height(18));
        button.buttonStyle(bs -> bs
                .baseTexture(new ColorRectTexture(preset))
                .hoverTexture(new ColorRectTexture(lighter(preset)))
        );
        button.addEventListener(UIEvents.CLICK, e -> canvas.setColor(preset));
        return button;
    }

    private Button widthButton(String text, float width) {
        var button = new Button().setText(text);
        button.layout(l -> l.width(30).height(18));
        button.textStyle(ts -> ts.textShadow(false));
        button.addEventListener(UIEvents.CLICK, e -> canvas.setWidth(width));
        return button;
    }

    private static Label smallLabel(String text) {
        var label = new Label();
        label.setText(Component.literal(text));
        label.textStyle(ts -> ts.textColor(0xFFFFE0B2).textShadow(false).fontSize(7));
        return label;
    }

    private void updatePreviewTexture() {
        preview.style(s -> s.background(SpriteTexture.of(NoteTextures.get(color, false))));
    }

    private StickyNote draft() {
        return new StickyNote(0, 0, color, String.join("\n", textArea.getValue()),
                new ArrayList<>(canvas.getStrokes()), false);
    }

    private static int lighter(int color) {
        int r = Math.min(255, ((color >>> 16) & 0xFF) + 40);
        int g = Math.min(255, ((color >>> 8) & 0xFF) + 40);
        int b = Math.min(255, (color & 0xFF) + 40);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static final class LimitedTextArea extends TextArea {

        private final int maxChars;

        private LimitedTextArea(int maxChars) {
            this.maxChars = maxChars;
        }

        @Override
        protected void replaceSelectionWith(String text) {
            if (text == null) return;
            int current = charCount();
            if (hasSelection()) {
                current -= charsBetween(getSelStartLine(), getSelStartCol(), getSelEndLine(), getSelEndCol());
            }
            int room = maxChars - current;
            if (room <= 0) return;
            if (text.length() > room) {
                text = text.substring(0, room);
            }
            super.replaceSelectionWith(text);
        }

        private int charCount() {
            var lines = getLines();
            int count = 0;
            for (int i = 0; i < lines.size(); i++) {
                count += lines.get(i).length();
                if (i < lines.size() - 1) count++;
            }
            return count;
        }

        private int charsBetween(int startLine, int startCol, int endLine, int endCol) {
            var lines = getLines();
            if (startLine == endLine) {
                return endCol - startCol;
            }
            int count = 0;
            for (int line = startLine; line <= endLine; line++) {
                int from = (line == startLine) ? startCol : 0;
                int to = (line == endLine) ? endCol : lines.get(line).length();
                count += to - from;
                if (line < endLine) count++;
            }
            return count;
        }
    }
}
