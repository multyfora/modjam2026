package net.multyfora.don.client.dialogue;

import java.util.List;

/**
 * A single dialogue line composed of styled {@link Segment}s.
 *
 * <p>Effect flags live on segments and render per word; the typewriter reveal
 * is driven by a character offset into the plain concatenated text.
 */
public record RichText(List<Segment> segments) {

    public RichText {
        segments = List.copyOf(segments);
    }

    public static RichText plain(String text) {
        return new RichText(List.of(new Segment(text, SegmentStyle.DEFAULT)));
    }

    public static RichText parse(String text) {
        return DialogueMarkup.parse(text);
    }

    public static final RichText EMPTY = RichText.plain("");

    public String fullText() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append(segment.text());
        }
        return sb.toString();
    }

    public enum Effect { NONE, SHAKE, POP, WAVE }

    public record SegmentStyle(
            int color,
            Effect effect,
            float amount,
            boolean bold,
            boolean italic,
            boolean shadow
    ) {
        public static final SegmentStyle DEFAULT =
            new SegmentStyle(0xFFFFFFFF, Effect.NONE, 0f, false, false, true);

        public SegmentStyle withColor(int color) {
            return new SegmentStyle(color, effect, amount, bold, italic, shadow);
        }

        public SegmentStyle withEffect(Effect effect, float amount) {
            return new SegmentStyle(color, effect, amount, bold, italic, shadow);
        }

        public SegmentStyle withBold(boolean bold) {
            return new SegmentStyle(color, effect, amount, bold, italic, shadow);
        }

        public SegmentStyle withItalic(boolean italic) {
            return new SegmentStyle(color, effect, amount, bold, italic, shadow);
        }
    }

    public record Segment(String text, SegmentStyle style) {
    }
}