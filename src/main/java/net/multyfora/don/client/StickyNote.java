package net.multyfora.don.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public record StickyNote(float x, float y, int color, String text, List<Stroke> strokes, boolean skewed) {

    public static final float NOTE_WIDTH = 0.16f;
    public static final float SKEWED_ASPECT = 1.2f;

    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_COLOR = "color";
    private static final String KEY_TEXT = "text";
    private static final String KEY_SKEWED = "skewed";
    private static final String KEY_STROKES = "strokes";

    public StickyNote {
        strokes = List.copyOf(strokes);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putFloat(KEY_X, x);
        tag.putFloat(KEY_Y, y);
        tag.putInt(KEY_COLOR, color);
        tag.putString(KEY_TEXT, text);
        tag.putBoolean(KEY_SKEWED, skewed);
        var strokesTag = new ListTag();
        for (var stroke : strokes) {
            strokesTag.add(stroke.save());
        }
        tag.put(KEY_STROKES, strokesTag);
        return tag;
    }

    public static StickyNote load(CompoundTag tag) {
        var strokes = new ArrayList<Stroke>();
        for (Tag entry : tag.getListOrEmpty(KEY_STROKES)) {
            strokes.add(Stroke.load((CompoundTag) entry));
        }
        return new StickyNote(
                tag.getFloatOr(KEY_X, 0f),
                tag.getFloatOr(KEY_Y, 0f),
                tag.getIntOr(KEY_COLOR, 0),
                tag.getStringOr(KEY_TEXT, ""),
                strokes,
                tag.getBooleanOr(KEY_SKEWED, false)
        );
    }

    public record Stroke(List<Vector2f> points, int color, float width) {

        private static final String KEY_POINTS = "points";
        private static final String KEY_COLOR = "color";
        private static final String KEY_WIDTH = "width";

        public Stroke {
            points = List.copyOf(points);
        }

        public CompoundTag save() {
            var tag = new CompoundTag();
            tag.putInt(KEY_COLOR, color);
            tag.putFloat(KEY_WIDTH, width);
            var pointsTag = new ListTag();
            for (var point : points) {
                var pointTag = new CompoundTag();
                pointTag.putFloat("px", point.x);
                pointTag.putFloat("py", point.y);
                pointsTag.add(pointTag);
            }
            tag.put(KEY_POINTS, pointsTag);
            return tag;
        }

        public static Stroke load(CompoundTag tag) {
            var points = new ArrayList<Vector2f>();
            for (Tag entry : tag.getListOrEmpty(KEY_POINTS)) {
                var pointTag = (CompoundTag) entry;
                points.add(new Vector2f(pointTag.getFloatOr("px", 0f), pointTag.getFloatOr("py", 0f)));
            }
            return new Stroke(points, tag.getIntOr(KEY_COLOR, 0), tag.getFloatOr(KEY_WIDTH, 0f));
        }
    }
}
