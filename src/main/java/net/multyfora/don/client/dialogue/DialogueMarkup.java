package net.multyfora.don.client.dialogue;

import java.util.ArrayList;
import java.util.List;

import net.multyfora.don.client.dialogue.RichText.Effect;
import net.multyfora.don.client.dialogue.RichText.Segment;
import net.multyfora.don.client.dialogue.RichText.SegmentStyle;

/**
 * Parses a compact dialogue markup into a {@link RichText}.
 *
 * <pre>
 *   {#FFD700}gold{/}        - solid color (3 or 6 hex digits, RGB)
 *   {shake}word{/}          - word jitter (optional {shake:2} amplitude)
 *   {pop}word{/}            - one-shot scale-in on reveal
 *   {wave}word{/}           - per-character sine bob
 *   {b} / {i}               - bold / italic
 *   {/}                     - reset all modifiers to the default style
 *   {{                     - escapes a literal '{'
 * </pre>
 *
 * <p>Unknown or unterminated tags are kept as literal text so ordinary
 * dialogue strings never break.
 */
public final class DialogueMarkup {

    private DialogueMarkup() {
    }

    public static RichText parse(String text) {
        if (text == null || text.isEmpty()) {
            return RichText.EMPTY;
        }

        List<Segment> segments = new ArrayList<>();
        SegmentStyle current = SegmentStyle.DEFAULT;
        StringBuilder buffer = new StringBuilder();

        int i = 0;
        int until = text.length();

        while (i < until) {
            char c = text.charAt(i);

            if (c == '{') {
                if (i + 1 < until && text.charAt(i + 1) == '{') {
                    buffer.append('{');
                    i += 2;
                    continue;
                }
                int close = text.indexOf('}', i + 1);
                if (close < 0) {
                    buffer.append(c);
                    i++;
                    continue;
                }
                SegmentStyle next = parseTag(text.substring(i + 1, close), current);
                if (next == null) {
                    buffer.append(c);
                    i++;
                    continue;
                }
                commit(segments, buffer, current);
                current = next;
                i = close + 1;
                continue;
            }

            buffer.append(c);
            i++;
        }

        commit(segments, buffer, current);
        return new RichText(segments);
    }

    private static void commit(List<Segment> segments, StringBuilder buffer, SegmentStyle style) {
        if (buffer.length() == 0) return;
        segments.add(new Segment(buffer.toString(), style));
        buffer.setLength(0);
    }

    /**
     * @return the newly applied style, or {@code null} if the tag is unknown.
     */
    private static SegmentStyle parseTag(String tag, SegmentStyle style) {
        if (tag.isEmpty() || tag.equals("/")) {
            return tag.equals("/") ? SegmentStyle.DEFAULT : null;
        }

        if (tag.startsWith("#")) {
            var color = parseColor(tag);
            return color == null ? null : style.withColor(color);
        }

        int colon = tag.indexOf(':');
        String name = (colon >= 0) ? tag.substring(0, colon) : tag;
        String value = (colon >= 0) ? tag.substring(colon + 1) : null;

        return switch (name) {
            case "b" -> style.withBold(true);
            case "i" -> style.withItalic(true);
            case "shake" -> style.withEffect(Effect.SHAKE, parseFloat(value, 2.0f));
            case "pop" -> style.withEffect(Effect.POP, parseFloat(value, 0.3f));
            case "wave" -> style.withEffect(Effect.WAVE, parseFloat(value, 1.0f));
            case "color" -> {
                var color = parseColor(value);
                yield color == null ? null : style.withColor(color);
            }
            default -> null;
        };
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Integer parseColor(String value) {
        if (value == null) return null;
        String v = value.startsWith("#") ? value.substring(1) : value;
        if (v.length() != 3 && v.length() != 6) return null;
        try {
            return switch (v.length()) {
                case 3 -> {
                    int r = Integer.parseInt(v.substring(0, 1), 16);
                    int g = Integer.parseInt(v.substring(1, 2), 16);
                    int b = Integer.parseInt(v.substring(2, 3), 16);
                    yield ((r * 17) << 16) | ((g * 17) << 8) | (b * 17) | 0xFF000000;
                }
                default -> (Integer.parseInt(v, 16) | 0xFF000000);
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}