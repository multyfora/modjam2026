package net.multyfora.don.client;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.multyfora.don.client.BetrayedClientState;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.multyfora.don.client.dialogue.RichText;
import net.multyfora.don.client.dialogue.RichTextElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class DialogueSystem {
    private static final DialogueSystem INSTANCE = new DialogueSystem();
    private static final int CHARS_PER_TICK = 1;
    private static final int HOLD_TICKS = 80;
    private static final int LEAD_TICKS = 12;
    private static final int FADE_TICKS = 8;

    private static final int OUTER_GOLD = 0xFF6B4A20;
    private static final int GOLD_BORDER = 0xFFD4A840;
    private static final int INNER_STONE = 0xFF3A2410;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int DIALOG_BG = 0xE2170803;
    private static final int DIAMOND = 0xFFFFD700;

    private boolean active;
    private List<RichText> queue = List.of();
    private int queueIndex;
    private String fullText;
    private int displayedLength;
    private int ticksSinceComplete;
    private boolean leading;
    private boolean labelCentered;
    private int leadTicks;
    private int fadeTicks;
    private Runnable onComplete;
    private Runnable onLineChange;

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
        playMarkup(lines, null, onComplete);
    }

    public void playMarkup(List<String> lines, boolean centered, Runnable onComplete) {
        this.labelCentered = centered;
        playMarkup(lines, null, onComplete);
    }

    public void playMarkup(List<String> lines, Runnable onLineChange, Runnable onComplete) {
        List<RichText> richLines = lines.stream().map(RichText::parse).collect(Collectors.toList());
        playRich(richLines, onLineChange, onComplete);
    }

    public void playSequence(List<Component> lines, Runnable onComplete) {
        List<RichText> richLines = lines.stream()
                .map(component -> RichText.parse(component.getString()))
                .collect(Collectors.toList());
        playRich(richLines, null, onComplete);
    }

    public void playRich(List<RichText> lines, Runnable onComplete) {
        playRich(lines, null, onComplete);
    }

    public void playRich(List<RichText> lines, Runnable onLineChange, Runnable onComplete) {
        if (BetrayedClientState.isBetrayed()) return;
        ensureUI();
        this.queue = lines == null ? List.of() : List.copyOf(lines);
        this.queueIndex = 0;
        this.onComplete = onComplete;
        this.onLineChange = onLineChange;
        startRich();
    }

    private void startRich() {
        this.fullText = queue.isEmpty() ? "" : queue.get(0).fullText();
        this.displayedLength = 0;
        this.ticksSinceComplete = 0;
        this.fadeTicks = 0;
        this.active = true;
        label.setCentered(labelCentered);
        label.setText(queue.isEmpty() ? RichText.EMPTY : queue.get(0));
        label.setRevealChars(0);
    }

    public void tick() {
        if (!active) return;
        if (fadeTicks < FADE_TICKS) {
            fadeTicks++;
        }

        if (displayedLength < fullText.length()) {
            int next = Math.min(fullText.length(), displayedLength + CHARS_PER_TICK);
            for (int i = displayedLength; i < next; i++) {
                if (!Character.isWhitespace(fullText.charAt(i))) {
                    playTickSound(i);
                }
            }
            displayedLength = next;
            label.setRevealChars(displayedLength);
        } else if (!leading && ++ticksSinceComplete > HOLD_TICKS) {
            if (queueIndex + 1 < queue.size() && onLineChange != null) {
                leading = true;
                leadTicks = 0;
                onLineChange.run();
            } else {
                advance();
            }
        }
        if (leading && ++leadTicks > LEAD_TICKS) {
            leading = false;
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
        leading = false;
        labelCentered = false;
        leadTicks = 0;
        onComplete = null;
        onLineChange = null;
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

        UIElement panel = new UIElement()
            .layout(l -> l
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(7)
                .paddingAll(12)
            )
            .style(s -> s.background(
                SDFRectTexture.of(DIALOG_BG).setRadius(6f).setBorderColor(0x55D4A840)
            ))
            .addChild(ornament())
            .addChild(label);

        UIElement inner = new UIElement()
            .layout(l -> l.widthPercent(100).paddingAll(2))
            .style(s -> s.background(
                SDFRectTexture.of(INNER_STONE).setRadius(8f).setBorderColor(DARK_GOLD)
            ))
            .addChild(panel);

        UIElement halo = new UIElement()
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(-16).top(-16).right(-16).bottom(-16))
            .style(s -> s.background(SDFRectTexture.of(0x38FFD700).setRadius(28f)));

        UIElement bezel = new UIElement()
            .layout(l -> l.widthPercent(100).paddingAll(3))
            .style(s -> s.background(
                SDFRectTexture.of(OUTER_GOLD).setRadius(12f).setBorderColor(GOLD_BORDER)
            ))
            .addChild(inner);

        UIElement dialogBox = new UIElement()
            .layout(l -> l
                .widthPercent(70)
                .marginBottom(30)
                .flexDirection(FlexDirection.COLUMN)
            )
            .addChildren(halo, bezel);

        float[] time = {0f};
        dialogBox.addEventListener(UIEvents.TICK, e -> {
            time[0] += 0.04f;
            float t = Mth.clamp((float) fadeTicks / FADE_TICKS, 0f, 1f);
            float fade = 1f - (1f - t) * (1f - t) * (1f - t);
            dialogBox.style(s -> s.opacity(fade));
            halo.style(s -> s.opacity(fade * (0.55f + 0.3f * (float) Math.sin(time[0] * 1.2f))));
        });

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

    private static UIElement ornament() {
        var wrap = new UIElement()
                .layout(l -> l.widthPercent(100).height(8)
                        .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));

        var lineLeft = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        var diamond = new UIElement()
                .layout(l -> l.width(5).height(5))
                .style(s -> s.background(SDFRectTexture.of(DIAMOND).setRadius(1f)));
        diamond.transform(t -> t.rotation(45f));

        var lineRight = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        wrap.addChildren(lineLeft, diamond, lineRight);
        return wrap;
    }
}