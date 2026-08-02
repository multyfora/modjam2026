package net.multyfora.modjam.client;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.multyfora.modjam.client.dialogue.RichText;
import net.multyfora.modjam.client.dialogue.RichTextElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class DialogueSystem {
    private static final DialogueSystem INSTANCE = new DialogueSystem();
    private static final int CHARS_PER_TICK = 1;
    private static final int HOLD_TICKS = 80;

    private boolean active;
    private List<RichText> queue = List.of();
    private int queueIndex;
    private String fullText;
    private int displayedLength;
    private int ticksSinceComplete;
    private Runnable onComplete;

    private RichTextElement label;
    private ModularUI mui;
    private boolean uiBuilt;

    public static DialogueSystem getInstance() {
        return INSTANCE;
    }

    public void showDialogue(String text) {
        playMarkup(List.of(text), null);
    }

    public void showMarkup(String text) {
        playMarkup(List.of(text), null);
    }

    public void playMarkup(List<String> lines, Runnable onComplete) {
        List<RichText> richLines = lines.stream().map(RichText::parse).collect(Collectors.toList());
        playRich(richLines, onComplete);
    }

    public void playSequence(List<Component> lines, Runnable onComplete) {
        List<RichText> richLines = lines.stream()
                .map(component -> RichText.parse(component.getString()))
                .collect(Collectors.toList());
        playRich(richLines, onComplete);
    }

    public void playRich(List<RichText> lines, Runnable onComplete) {
        ensureUI();
        this.queue = lines == null ? List.of() : List.copyOf(lines);
        this.queueIndex = 0;
        this.onComplete = onComplete;
        startRich();
    }

    private void startRich() {
        this.fullText = queue.isEmpty() ? "" : queue.get(0).fullText();
        this.displayedLength = 0;
        this.ticksSinceComplete = 0;
        this.active = true;
        label.setText(queue.isEmpty() ? RichText.EMPTY : queue.get(0));
        label.setRevealChars(0);
    }

    public void tick() {
        if (!active) return;

        if (displayedLength < fullText.length()) {
            int next = Math.min(fullText.length(), displayedLength + CHARS_PER_TICK);
            for (int i = displayedLength; i < next; i++) {
                if (!Character.isWhitespace(fullText.charAt(i))) {
                    playTickSound(i);
                }
            }
            displayedLength = next;
            label.setRevealChars(displayedLength);
        } else if (++ticksSinceComplete > HOLD_TICKS) {
            advance();
        }
    }

    private void advance() {
        queueIndex++;
        if (queueIndex < queue.size()) {
            fullText = queue.get(queueIndex).fullText();
            displayedLength = 0;
            ticksSinceComplete = 0;
            label.setText(queue.get(queueIndex));
            label.setRevealChars(0);
            return;
        }
        var callback = onComplete;
        clear();
        if (callback != null) {
            callback.run();
        }
    }

    private void playTickSound(int index) {
        float pitch = 1.2f + (index % 5) * 0.12f;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
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
            label.setText(RichText.EMPTY);
            label.setRevealChars(0);
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

        label = new RichTextElement();
        label.setText(RichText.EMPTY);
        label.setFontSize(14);
        label.setLineSpacing(2);

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