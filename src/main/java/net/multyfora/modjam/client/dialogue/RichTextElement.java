package net.multyfora.modjam.client.dialogue;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.multyfora.modjam.client.dialogue.RichText.Effect;
import net.multyfora.modjam.client.dialogue.RichText.Segment;
import net.multyfora.modjam.client.dialogue.RichText.SegmentStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * A typewriter-driven rich text element.
 *
 * <p>Words are laid out left-aligned and wrapped; a character-reveal offset
 * hides everything after it. Per-word {@link Effect}s (shake / pop / wave)
 * are animated against a clock that starts the moment the word's last
 * glyph is revealed, so highlighted words visually "arrive" for emphasis.
 */
public class RichTextElement extends UIElement {

    static {
        UIElementRendererRegistry.register(RichTextElement.class, new RichTextElementRenderer());
    }

    private static final float DEFAULT_FONT_SIZE = 14f;
    private static final float DEFAULT_LINE_SPACING = 2f;

    private List<Segment> segments = List.of();
    private List<Word> words = new ArrayList<>();
    private List<Integer> breaksBefore = new ArrayList<>();
    private List<Line> lines = List.of();
    private long[] wordRevealAt = new long[0];

    private float fontSize = DEFAULT_FONT_SIZE;
    private float lineSpacing = DEFAULT_LINE_SPACING;
    private float layoutHeight = Float.NaN;
    private int revealChars;
    private boolean centered;
    private boolean recomputing;

    public RichTextElement() {
    }

    public RichTextElement setText(RichText text) {
        return setSegments(text == null ? List.of() : text.segments());
    }

    public RichTextElement setSegments(List<Segment> segments) {
        this.segments = segments == null ? List.of() : List.copyOf(segments);
        tokenize();
        recomputeLayout();
        return this;
    }

    public RichTextElement setFontSize(float fontSize) {
        this.fontSize = Math.max(1.0f, fontSize);
        recomputeLayout();
        return this;
    }

    public RichTextElement setLineSpacing(float lineSpacing) {
        this.lineSpacing = Math.max(0.0f, lineSpacing);
        recomputeLayout();
        return this;
    }

    public RichTextElement setRevealChars(int revealChars) {
        this.revealChars = Math.max(0, revealChars);
        return this;
    }

    public RichTextElement setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public int getRevealChars() {
        return revealChars;
    }

    public float getFontSize() {
        return fontSize;
    }

    public float getLineSpacing() {
        return lineSpacing;
    }

    public boolean isFullyRevealed() {
        int total = 0;
        for (Segment segment : segments) {
            total += segment.text().length();
        }
        return revealChars >= total;
    }

    public int lineCount() {
        return lines.size();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        recomputeLayout();
    }



    private void tokenize() {
        words.clear();
        breaksBefore.clear();

        String full = fullText();
        int length = full.length();

        StringBuilder buffer = new StringBuilder();
        int bufferStart = 0;
        int newlines = 0;

        for (int i = 0; i <= length; i++) {
            char c = (i < length) ? full.charAt(i) : ' ';
            if (c == '\n') {
                if (buffer.length() > 0) {
                    addWord(buffer.toString(), bufferStart, newlines);
                    buffer.setLength(0);
                    newlines = 0;
                }
                newlines++;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (buffer.length() > 0) {
                    addWord(buffer.toString(), bufferStart, newlines);
                    buffer.setLength(0);
                }
                continue;
            }
            if (buffer.length() == 0) {
                bufferStart = i;
            }
            buffer.append(c);
        }

        wordRevealAt = new long[words.size()];
        lines = List.of();
        revealChars = 0;
    }

    private void addWord(String text, int start, int newlines) {
        words.add(new Word(text, start, start + text.length(), styleAt(start)));
        breaksBefore.add(newlines);
    }

    private SegmentStyle styleAt(int index) {
        int cursor = 0;
        for (Segment segment : segments) {
            cursor += segment.text().length();
            if (index < cursor) {
                return segment.style();
            }
        }
        return SegmentStyle.DEFAULT;
    }

    private String fullText() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append(segment.text());
        }
        return sb.toString();
    }

    private void recomputeLayout() {
        if (recomputing) return;
        recomputing = true;
        try {
            if (words.isEmpty()) {
                lines = List.of();
                return;
            }

            Font font = font();
            float scale = fontSize / font.lineHeight;
            float maxWidth = getContentWidth();

            List<Line> built = new ArrayList<>();
            List<Word> current = new ArrayList<>();
            float cursor = 0f;

            for (int i = 0; i < words.size(); i++) {
                Word word = words.get(i);

                if (breaksBefore.get(i) > 0 && !current.isEmpty()) {
                    built.add(new Line(current, cursor));
                    current = new ArrayList<>();
                    cursor = 0f;
                }

                float wordWidth = scaledWidth(font, word, scale);
                float gap = current.isEmpty() ? 0f : font.width(" ") * scale;
                if (!current.isEmpty() && cursor + gap + wordWidth > maxWidth) {
                    built.add(new Line(current, cursor));
                    current = new ArrayList<>();
                    cursor = 0f;
                    gap = 0f;
                }

                word.x = cursor + gap;
                word.width = wordWidth;
                cursor += gap + wordWidth;
                current.add(word);
            }
            if (!current.isEmpty()) {
                built.add(new Line(current, cursor));
            }

            this.lines = built;
            float height = Math.max(0f, lines.size() * (fontSize + lineSpacing) - lineSpacing);
            if (Float.isNaN(layoutHeight) || Math.abs(layoutHeight - height) > 0.05f) {
                layoutHeight = height;
                getLayout().height(height);
            }
        } finally {
            recomputing = false;
        }
    }

    private static float scaledWidth(Font font, Word word, float scale) {
        return font.getSplitter().stringWidth(shapeSequence(word.text, word.style)) * scale;
    }


    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static Style mcStyle(SegmentStyle style) {
        Style mc = Style.EMPTY;
        if (style.color() != SegmentStyle.DEFAULT.color()) {
            mc = mc.withColor(TextColor.fromRgb(style.color() & 0xFFFFFF));
        }
        if (style.bold()) mc = mc.withBold(true);
        if (style.italic()) mc = mc.withItalic(true);
        return mc;
    }

    private static FormattedCharSequence shapeSequence(String text, SegmentStyle style) {
        var component = Component.literal(text).withStyle(mcStyle(style));
        int shaper = (int) font().getSplitter().stringWidth(component) + 8;
        shaper = Math.max(shaper, 16);
        return font().split(component, shaper).get(0);
    }

    private static void render(GUIContext c, RichTextElement element) {
        var lines = element.lines;
        if (lines.isEmpty()) return;

        Font font = font();
        float scale = element.fontSize / font.lineHeight;
        float baseX = element.getContentX();
        float baseY = element.getContentY();
        int reveal = element.revealChars;
        long now = System.currentTimeMillis();
        int wordIndex = 0;

        for (int li = 0; li < lines.size(); li++) {
            var line = lines.get(li);
            float y = baseY + li * (element.fontSize + element.lineSpacing);
            float lineOffset = element.centered ? Math.max(0f, (element.getContentWidth() - line.width()) / 2f) : 0f;
            for (var word : line.words()) {
                String text = word.text;
                int a = word.absStart;
                int b = word.absEnd;
                if (a >= reveal) break;

                int shownLength = Math.min(b, reveal) - a;
                if (shownLength <= 0) continue;

                boolean fully = b <= reveal;
                String shown = shownLength >= text.length() ? text : text.substring(0, shownLength);

                float t = -1f;
                if (fully) {
                    long revealedAt = element.wordRevealAt[wordIndex];
                    if (revealedAt == 0L) {
                        element.wordRevealAt[wordIndex] = now;
                        revealedAt = now;
                    }
                    t = clamp((now - revealedAt) / 1000f, 0f, 10f);
                }

                float x = baseX + lineOffset + word.x;
                drawWord(c, font, word, shown, fully, x, y, scale, t);
                wordIndex++;
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    private static void drawWord(GUIContext c, Font font, Word word, String shown, boolean fully, float x, float y, float scale, float t) {
        SegmentStyle style = word.style;
        Effect effect = fully ? style.effect() : Effect.NONE;

        if (effect == Effect.WAVE) {
            drawWave(c, font, shown, x, y, scale, t, style);
            return;
        }

        float originX = x;
        float originY = y;
        float glyphScale = scale;

        if (effect == Effect.SHAKE) {
            float amp = Math.max(0f, style.amount());
            originX += ((Math.random() * 2.0) - 1.0) * amp;
            originY += ((Math.random() * 2.0) - 1.0) * amp;
        } else if (effect == Effect.POP) {
            glyphScale = scale * popScale(t, style.amount());
        }

        drawSequence(c, font, shapeSequence(shown, style), originX, originY, glyphScale, style.color(), style.shadow());
    }

    private static void drawWave(GUIContext c, Font font, String shown, float x, float y, float scale, float t, SegmentStyle style) {
        float cursor = 0f;
        float amplitude = Math.max(0f, style.amount());
        for (int i = 0; i < shown.length(); i++) {
            String charText = String.valueOf(shown.charAt(i));
            FormattedCharSequence sequence = shapeSequence(charText, style);
            float charWidth = font.getSplitter().stringWidth(sequence) * scale;
            float bob = (float) Math.sin(t * Math.PI * 2.5 + i * 0.8) * amplitude;
            drawSequence(c, font, sequence, x + cursor, y + bob, scale, style.color(), style.shadow());
            cursor += charWidth + 0.5f;
        }
    }

    private static void drawSequence(GUIContext c, Font font, FormattedCharSequence sequence, float x, float y, float scale, int color, boolean shadow) {
        c.pose.pushPose();
        c.pose.translate(x, y);
        c.pose.scale(scale, scale);
        c.graphics.text(font, sequence, 0, 0, color, shadow);
        c.pose.popPose();
    }

    private static float popScale(float t, float amount) {
        if (t < 0f) return 1f;
        float peak = Math.max(0f, amount);
        float decay = (float) Math.exp(-6.0 * t);
        float oscillation = (float) Math.cos(11.0 * t);
        return Math.max(0.3f, 1f + peak * decay * oscillation);
    }


    private static final class Word {
        final String text;
        final int absStart;
        final int absEnd;
        SegmentStyle style;
        float x;
        float width;

        Word(String text, int absStart, int absEnd, SegmentStyle style) {
            this.text = text;
            this.absStart = absStart;
            this.absEnd = absEnd;
            this.style = style;
        }
    }

    private record Line(List<Word> words, float width) {
    }

    public static final class RichTextElementRenderer extends DelegatingUIElementRenderer<RichTextElement, RichTextElementRenderer> {
        @Override
        public Class<RichTextElement> type() {
            return RichTextElement.class;
        }

        @Override
        public void drawBackgroundAdditional(RichTextElement element, IGUIContext context) {
            if (!(context instanceof GUIContext c)) {
                drawParentBackgroundAdditional(element, context);
                return;
            }
            render(c, element);
        }
    }
}