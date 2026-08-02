package net.multyfora.modjam.client;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DialogueSystem {
    private static final DialogueSystem INSTANCE = new DialogueSystem();
    private static final int CHARS_PER_TICK = 2;
    private static final int HOLD_TICKS = 80;

    private boolean active;
    private List<Component> queue = List.of();
    private int queueIndex;
    private String fullText;
    private int displayedLength;
    private int ticksSinceComplete;
    private Runnable onComplete;

    private Label label;
    private ModularUI mui;
    private boolean uiBuilt;

    public static DialogueSystem getInstance() {
        return INSTANCE;
    }

    public void showDialogue(String text) {
        playSequence(List.of(Component.literal(text)), null);
    }

    public void playSequence(List<Component> lines, Runnable onComplete) {
        ensureUI();
        this.queue = lines;
        this.queueIndex = 0;
        this.onComplete = onComplete;
        this.fullText = lines.isEmpty() ? "" : lines.get(0).getString();
        this.displayedLength = 0;
        this.ticksSinceComplete = 0;
        this.active = true;
        label.setText(Component.empty());
    }

    public void tick() {
        if (!active) return;

        if (displayedLength < fullText.length()) {
            displayedLength = Math.min(displayedLength + CHARS_PER_TICK, fullText.length());
            label.setText(Component.literal(fullText.substring(0, displayedLength)));
        } else if (++ticksSinceComplete > HOLD_TICKS) {
            advance();
        }
    }

    private void advance() {
        queueIndex++;
        if (queueIndex < queue.size()) {
            fullText = queue.get(queueIndex).getString();
            displayedLength = 0;
            ticksSinceComplete = 0;
            label.setText(Component.empty());
            return;
        }
        var callback = onComplete;
        clear();
        if (callback != null) {
            callback.run();
        }
    }

    public void clear() {
        active = false;
        queue = List.of();
        queueIndex = 0;
        fullText = null;
        displayedLength = 0;
        ticksSinceComplete = 0;
        onComplete = null;
        if (label != null) {
            label.setText(Component.empty());
        }
    }

    public boolean isActive() {
        return active;
    }

    @Nullable
    public ModularUI getModularUI() {
        if (!active) return null;
        return mui;
    }

    private void ensureUI() {
        if (uiBuilt) return;

        label = new Label();
        label.setText(Component.empty());
        label.textStyle(ts -> ts
            .textColor(0xFFFFFFFF)
            .fontSize(14)
            .textWrap(TextWrap.WRAP)
            .adaptiveHeight(true)
        );

        UIElement dialogBox = new UIElement()
            .layout(l -> l
                .widthPercent(70)
                .paddingAll(12)
                .marginBottom(30)
            )
            .style(s -> s
                .background(new ColorRectTexture(0xCC111111))
            )
            .addChild(label);

        UIElement root = new UIElement()
            .layout(l -> l
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.FLEX_END)
                .alignItems(AlignItems.CENTER)
            )
            .addChild(dialogBox);

        mui = ModularUI.of(UI.of(root));
        uiBuilt = true;
    }
}
