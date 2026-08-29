package net.multyfora.don.client;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class StrokeCanvas extends UIElement {

    public static final int PEN_COLOR = 0xFF3E2723;
    public static final float PEN_WIDTH = 0.03f;

    static {
        UIElementRendererRegistry.register(StrokeCanvas.class, new StrokeCanvasRenderer());
    }

    private final List<StickyNote.Stroke> strokes = new ArrayList<>();
    private final List<Vector2f> currentStroke = new ArrayList<>();
    private int currentColor = PEN_COLOR;
    private float currentWidth = PEN_WIDTH;

    private StrokeCanvas(boolean editable) {
        if (editable) {
            addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragUpdate, true);
            addEventListener(UIEvents.DRAG_END, this::onDragEnd, true);
        }
    }

    public static StrokeCanvas editable() {
        return new StrokeCanvas(true);
    }

    public static StrokeCanvas display(List<StickyNote.Stroke> strokes) {
        var canvas = new StrokeCanvas(false);
        canvas.strokes.addAll(strokes);
        return canvas;
    }

    public List<StickyNote.Stroke> getStrokes() {
        return strokes;
    }

    public void clearStrokes() {
        strokes.clear();
    }

    public void setColor(int color) {
        this.currentColor = color;
    }

    public void setWidth(float width) {
        this.currentWidth = width;
    }

    private void onMouseDown(UIEvent event) {
        if (event.button != 0) return;
        var modularUI = getModularUI();
        if (modularUI == null || modularUI.getDragHandler().isDragging()) return;
        currentStroke.clear();
        currentStroke.add(toLocal(event.x, event.y));
        modularUI.getDragHandler().startDrag(this, null, this);
    }

    private void onDragUpdate(UIEvent event) {
        if (currentStroke.isEmpty()) return;
        var point = toLocal(event.x, event.y);
        var last = currentStroke.get(currentStroke.size() - 1);
        if (point.distanceSquared(last) > 0.00005f) {
            currentStroke.add(point);
        }
    }

    private void onDragEnd(UIEvent event) {
        if (currentStroke.isEmpty()) return;
        strokes.add(new StickyNote.Stroke(new ArrayList<>(currentStroke), currentColor, currentWidth));
        currentStroke.clear();
    }

    private Vector2f toLocal(float mouseX, float mouseY) {
        float width = getSizeWidth();
        float height = getSizeHeight();
        if (width <= 0 || height <= 0) return new Vector2f(0, 0);
        float x = (mouseX - getPositionX()) / width;
        float y = (mouseY - getPositionY()) / height;
        return new Vector2f(Math.max(0f, Math.min(1f, x)), Math.max(0f, Math.min(1f, y)));
    }

    public static final class StrokeCanvasRenderer extends DelegatingUIElementRenderer<StrokeCanvas, StrokeCanvasRenderer> {

        @Override
        public Class<StrokeCanvas> type() {
            return StrokeCanvas.class;
        }

        @Override
        public void drawBackgroundAdditional(StrokeCanvas canvas, IGUIContext context) {
            if (!(context instanceof GUIContext guiContext)) {
                drawParentBackgroundAdditional(canvas, context);
                return;
            }
            float width = canvas.getSizeWidth();
            float height = canvas.getSizeHeight();
            if (width <= 0 || height <= 0) return;
            guiContext.pose.pushPose();
            guiContext.pose.translate(canvas.getPositionX(), canvas.getPositionY());
            for (var stroke : canvas.getStrokes()) {
                drawStroke(guiContext, stroke, width, height);
            }
            guiContext.pose.popPose();
        }

        private static void drawStroke(GUIContext context, StickyNote.Stroke stroke, float width, float height) {
            var points = stroke.points();
            if (points.isEmpty()) return;
            float lineWidth = Math.max(1f, stroke.width() * width);
            if (points.size() == 1) {
                var point = points.get(0);
                DrawerHelperClient.drawSolidRect(context, point.x * width - lineWidth / 2, point.y * height - lineWidth / 2, lineWidth, lineWidth, stroke.color());
                return;
            }
            var scaled = new ArrayList<Vector2f>(points.size());
            for (var point : points) {
                scaled.add(new Vector2f(point.x * width, point.y * height));
            }
            DrawerHelperClient.drawLines(context, scaled, stroke.color(), stroke.color(), lineWidth);
        }
    }
}
