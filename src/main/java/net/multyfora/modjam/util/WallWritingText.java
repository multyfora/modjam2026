package net.multyfora.modjam.util;

import java.util.ArrayList;
import java.util.List;

public final class WallWritingText {
    private WallWritingText() {}

    public static String toSga(String plain) {
        if (plain == null) return "";
        StringBuilder sb = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c >= 'a' && c <= 'z') sb.append((char) (c - 32));
            else if (c >= 'A' && c <= 'Z') sb.append(c);
            else if (c == ' ') sb.append(' ');
            else if (c == '\n') sb.append('\n');
        }
        return sb.toString();
    }

    public static List<String> wrap(String sga, int maxCharsPerLine) {
        if (sga == null || sga.isEmpty()) return List.of("");
        List<String> lines = new ArrayList<>();
        String[] words = sga.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (w.length() > maxCharsPerLine) {
                if (cur.length() > 0) { lines.add(cur.toString()); cur.setLength(0); }
                for (int i = 0; i < w.length(); i += maxCharsPerLine) {
                    lines.add(w.substring(i, Math.min(w.length(), i + maxCharsPerLine)));
                }
                continue;
            }
            int need = cur.length() == 0 ? w.length() : cur.length() + 1 + w.length();
            if (need > maxCharsPerLine) {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(w);
            } else {
                if (cur.length() > 0) cur.append(' ');
                cur.append(w);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }
}
