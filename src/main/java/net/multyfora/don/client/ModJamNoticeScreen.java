package net.multyfora.don.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class ModJamNoticeScreen {

    private static boolean shownThisSession = false;

    private static final int PANEL_WIDTH = 280;

    private static final Identifier ALT_FONT = Identifier.parse("minecraft:alt");

    private static final int GOLD = 0xFFFFD700;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int DEEP_BG = 0xFF120A04;
    private static final int STONE_BEZEL = 0xFF3A2410;
    private static final int GLOW_BORDER = 0xFFD4A840;
    private static final int TITLE_COLOR = 0xFFFFE0B2;
    private static final int FLAVOR_COLOR = 0x99D4A840;
    private static final int BODY_COLOR = 0xFFE8D9B8;
    private static final int SIGNATURE_COLOR = 0x88D4A840;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFF3D6;
    private static final int VIGNETTE = 0xB0000000;

    private static final List<String> NOTICE_LINES = List.of(
            "This mod was created as an entry for the",
            "CurseForge ModJam 2026: Echoes of the Past.",
            "It is currently in early beta, several",
            "features may not yet function as intended,",
            "and the story is not fully complete. Please",
            "expect bugs and inconsistencies.",
            "",
            "I am very excited to present",
            "my first entry in this ModJam!",
            "There is still much more to come",
            "so please look forward to future releases."
    );


    private ModJamNoticeScreen() {}

    public static boolean hasBeenShown() {
        return shownThisSession;
    }

    public static void showOnce() {
        if (shownThisSession) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        shownThisSession = true;
        mc.setScreenAndShow(new ModularUIScreen(createMenu(), Component.empty()));
    }

    public static void forceShow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.setScreenAndShow(new ModularUIScreen(createMenu(), Component.empty()));
    }

    private static ModularUI createMenu() {
        var root = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .heightPercent(100)
                        .flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER)
                        .alignItems(AlignItems.CENTER)
                )
                .style(s -> s.background(new ColorRectTexture(VIGNETTE)));

        var bezel = new UIElement()
                .layout(l -> l.width(PANEL_WIDTH).heightAuto().paddingAll(3))
                .style(s -> s.background(
                        SDFRectTexture.of(0xFF6B4A20)
                                .setRadius(10f)
                                .setBorderColor(GLOW_BORDER)
                ));

        var innerBezel = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto().paddingAll(2))
                .style(s -> s.background(
                        SDFRectTexture.of(STONE_BEZEL)
                                .setRadius(8f)
                                .setBorderColor(DARK_GOLD)
                ));

        var panel = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .heightAuto()
                        .flexDirection(FlexDirection.COLUMN)
                        .alignItems(AlignItems.CENTER)
                        .paddingAll(18)
                        .gapAll(6)
                )
                .style(s -> s.background(
                        SDFRectTexture.of(DEEP_BG)
                                .setRadius(6f)
                ));

        panel.addChild(headerFlourish());
        panel.addChild(title());
        panel.addChild(flavorLine());
        panel.addChild(ornateSeparator());

        var body = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .heightAuto()
                        .flexDirection(FlexDirection.COLUMN)
                        .alignItems(AlignItems.CENTER)
                        .gapAll(2)
                );
        for (String line : NOTICE_LINES) {
            body.addChild(bodyLine(line));
        }
        panel.addChild(body);

        panel.addChild(ornateSeparator());
        panel.addChild(signatureLine());
        panel.addChild(dismissButton());

        innerBezel.addChild(panel);
        bezel.addChild(innerBezel);
        root.addChild(bezel);
        return ModularUI.of(UI.of(root));
    }

    private static UIElement headerFlourish() {
        var wrap = new UIElement()
                .layout(l -> l.widthAuto().heightAuto()
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .gapAll(8));

        var leftGlyph = runeGlyph("love");
        var diamond = new UIElement()
                .layout(l -> l.width(8).height(8))
                .style(s -> s.background(SDFRectTexture.of(GOLD).setRadius(1.5f).setBorderColor(GLOW_BORDER)));
        diamond.transform(t -> t.rotation(45f));
        var rightGlyph = runeGlyph("love");

        wrap.addChildren(leftGlyph, diamond, rightGlyph);
        return wrap;
    }

    private static Label runeGlyph(String glyph) {
        var label = new Label();
        label.setText(Component.literal(glyph));
        label.textStyle(ts -> ts
                .font(ALT_FONT)
                .textColor(GOLD)
                .textShadow(true)
                .fontSize(12)
                .adaptiveWidth(true)
        );
        return label;
    }

    private static Label title() {
        var label = new Label();
        label.setText(Component.literal("A Note From The Developer").withStyle(Style.EMPTY.withBold(true)));
        label.textStyle(ts -> ts
                .textColor(TITLE_COLOR)
                .textShadow(true)
                .fontSize(14)
                .adaptiveWidth(true)
        );
        return label;
    }

    private static Label flavorLine() {
        var label = new Label();
        label.setText(Component.literal("Echoes of the Past CurseForge ModJam"));
        label.textStyle(ts -> ts
                .font(ALT_FONT)
                .textColor(FLAVOR_COLOR)
                .textShadow(false)
                .fontSize(9)
                .adaptiveWidth(true)
        );
        return label;
    }

    private static Label bodyLine(String text) {
        var label = new Label();
        if (text.isEmpty()) {
            label.setText(Component.literal(" "));
            label.textStyle(ts -> ts.fontSize(4).adaptiveWidth(true));
            return label;
        }
        label.setText(Component.literal(text));
        label.textStyle(ts -> ts
                .textColor(BODY_COLOR)
                .textShadow(false)
                .fontSize(10)
                .adaptiveWidth(true)
        );
        return label;
    }

    private static Label signatureLine() {
        var label = new Label();
        label.setText(Component.literal("thanks Wanii for helping me").withStyle(Style.EMPTY.withItalic(true)));
        label.textStyle(ts -> ts
                .font(ALT_FONT)
                .textColor(SIGNATURE_COLOR)
                .textShadow(false)
                .fontSize(9)
                .adaptiveWidth(true)
        );
        return label;
    }

    private static UIElement ornateSeparator() {
        var wrap = new UIElement()
                .layout(l -> l.width(PANEL_WIDTH - 70).height(10)
                        .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));

        var lineLeft = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        var diamond = new UIElement()
                .layout(l -> l.width(6).height(6))
                .style(s -> s.background(SDFRectTexture.of(GOLD).setRadius(1f)));
        diamond.transform(t -> t.rotation(45f));

        var lineRight = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        wrap.addChildren(lineLeft, diamond, lineRight);
        return wrap;
    }

    private static Button dismissButton() {
        var btn = new Button().setText(Component.literal("Understood"));
        btn.layout(l -> l.width(170).height(22));
        btn.textStyle(ts -> ts
                .textColor(BUTTON_TEXT_COLOR)
                .textShadow(true)
                .fontSize(11)
        );
        btn.style(s -> s.background(
                SDFRectTexture.of(0xCC3A2410).setRadius(6f).setBorderColor(DARK_GOLD)
        ));
        btn.buttonStyle(s -> s
                .baseTexture(SDFRectTexture.of(0xCC3A2410).setRadius(6f).setBorderColor(DARK_GOLD))
                .hoverTexture(SDFRectTexture.of(0xCC8B6914).setRadius(6f).setBorderColor(GOLD))
                .pressedTexture(SDFRectTexture.of(0xCC5C3A00).setRadius(6f).setBorderColor(GLOW_BORDER)));
        btn.addEventListener(UIEvents.CLICK, e -> Minecraft.getInstance().gui.setScreen(null));
        return btn;
    }
}