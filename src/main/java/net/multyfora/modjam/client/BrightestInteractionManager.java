package net.multyfora.modjam.client;

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
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.multyfora.modjam.network.AcceptDealPayload;
import net.multyfora.modjam.client.cutscene.CutsceneClientController;
import net.multyfora.modjam.client.dialogue.RichText;

import java.util.ArrayList;
import java.util.List;

public class BrightestInteractionManager {

    private static final BrightestInteractionManager INSTANCE = new BrightestInteractionManager();
    private static final int REFUSE_COOLDOWN = 60;
    private static final List<String> DIALOGUE_LINES = List.of(
            "dialogue.modjam.brightest.1",
            "dialogue.modjam.brightest.2",
            "dialogue.modjam.brightest.3",
            "dialogue.modjam.brightest.4",
            "dialogue.modjam.brightest.5"
    );

    private static final int PANEL_WIDTH = 240;
    private static final int ORB_SIZE = 64;
    private static final int RING_SIZE = ORB_SIZE + 24;
    private static final int GLOW_SIZE = ORB_SIZE + 44;
    private static final int ORBIT_SIZE = GLOW_SIZE + 26;

    private static final Identifier ALT_FONT = Identifier.parse("minecraft:alt");
    private static final String[] RUNE_GLYPHS = {"L", "I", "G", "H", "T", "S"};

    private static final int GOLD = 0xFFFFD700;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int AMBER = 0xFFFFA500;
    private static final int DEEP_BG = 0xFF120A04;
    private static final int STONE_BEZEL = 0xFF3A2410;
    private static final int GLOW_BORDER = 0xFFD4A840;
    private static final int TITLE_COLOR = 0xFFFFE0B2;
    private static final int FLAVOR_COLOR = 0x99D4A840;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFF3D6;
    private static final int VIGNETTE = 0xB0000000;

    private ModularUIScreen currentScreen;
    private int cooldown;

    public static BrightestInteractionManager getInstance() {
        return INSTANCE;
    }

    public void tick() {
        if (cooldown > 0) cooldown--;
    }

    public void openMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (FirstContactTransitionState.getInstance().isActive()) return;
        if (cooldown > 0) return;
        if (mc.gui.screen() != null) return;
        if (DialogueSystem.getInstance().isActive()) return;
        currentScreen = new ModularUIScreen(createMenu(), Component.empty());
        mc.setScreenAndShow(currentScreen);
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

        // outer gold bezel
        var bezel = new UIElement()
                .layout(l -> l.width(PANEL_WIDTH).heightAuto().paddingAll(3))
                .style(s -> s.background(
                        SDFRectTexture.of(0xFF6B4A20)
                                .setRadius(10f)
                                .setBorderColor(GLOW_BORDER)
                ));

        // inner stone bezel
        var innerBezel = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto().paddingAll(2))
                .style(s -> s.background(
                        SDFRectTexture.of(STONE_BEZEL)
                                .setRadius(8f)
                                .setBorderColor(DARK_GOLD)
                ));

        // content panel
        var panel = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .heightAuto()
                        .flexDirection(FlexDirection.COLUMN)
                        .alignItems(AlignItems.CENTER)
                        .paddingAll(16)
                        .gapAll(6)
                )
                .style(s -> s.background(
                        SDFRectTexture.of(DEEP_BG)
                                .setRadius(6f)
                ));

        panel.addChild(arcaneOrb());
        panel.addChild(title());
        panel.addChild(flavorLine());
        panel.addChild(ornateSeparator());
        panel.addChild(button("menu.modjam.brightest.talk", true));
        panel.addChild(button("menu.modjam.brightest.refuse", false));

        innerBezel.addChild(panel);
        bezel.addChild(innerBezel);
        root.addChild(bezel);
        return ModularUI.of(UI.of(root));
    }

    private static UIElement arcaneOrb() {
        var orbit = new UIElement().layout(l -> l.width(ORBIT_SIZE).height(ORBIT_SIZE));

        var glow = new UIElement()
                .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).width(GLOW_SIZE).height(GLOW_SIZE)
                        .left((ORBIT_SIZE - GLOW_SIZE) / 2f).top((ORBIT_SIZE - GLOW_SIZE) / 2f))
                .style(s -> s.background(
                        SDFRectTexture.of(0x40FFD700).setRadius(GLOW_SIZE / 2f)
                ));

        var ring = new UIElement()
                .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).width(RING_SIZE).height(RING_SIZE)
                        .left((ORBIT_SIZE - RING_SIZE) / 2f).top((ORBIT_SIZE - RING_SIZE) / 2f))
                .style(s -> s.background(
                        SDFRectTexture.of(0x00000000).setRadius(6f).setBorderColor(0xAAFFD700)
                ));

        var orb = new UIElement()
                .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).width(ORB_SIZE).height(ORB_SIZE)
                        .left((ORBIT_SIZE - ORB_SIZE) / 2f).top((ORBIT_SIZE - ORB_SIZE) / 2f))
                .style(s -> s.background(
                        SDFRectTexture.of(0xFFD4A840).setRadius(ORB_SIZE / 2f).setBorderColor(GLOW_BORDER)
                ));

        int coreSize = ORB_SIZE - 20;
        var core = new UIElement()
                .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).width(coreSize).height(coreSize)
                        .left((ORBIT_SIZE - coreSize) / 2f).top((ORBIT_SIZE - coreSize) / 2f))
                .style(s -> s.background(
                        SDFRectTexture.of(0xFFFFF3C0).setRadius(coreSize / 2f)
                ));

        orbit.addChildren(glow, ring, orb, core);

        // orbiting rune glyphs
        List<UIElement> runes = new ArrayList<>();
        float orbitRadius = ORBIT_SIZE / 2f - 6f;
        for (String glyph : RUNE_GLYPHS) {
            Label rune = new Label();
            rune.setText(Component.literal(glyph));
            rune.textStyle(ts -> ts.font(ALT_FONT).textColor(AMBER).fontSize(7).textShadow(false));
            rune.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).width(8).height(8));
            orbit.addChild(rune);
            runes.add(rune);
        }

        float[] time = {0f};
        orbit.addEventListener(UIEvents.TICK, e -> {
            time[0] += 0.05f;
            ring.transform(t -> t.rotation(time[0] * -14f));
            glow.style(s -> s.opacity(0.6f + 0.25f * (float) Math.sin(time[0] * 1.5f)));
            for (int i = 0; i < runes.size(); i++) {
                double angle = Math.toRadians(i * (360.0 / runes.size()) + time[0] * 10f);
                float x = ORBIT_SIZE / 2f + (float) (Math.cos(angle) * orbitRadius) - 4f;
                float y = ORBIT_SIZE / 2f + (float) (Math.sin(angle) * orbitRadius) - 4f;
                runes.get(i).layout(l -> l.left(x).top(y));
            }
        });

        return orbit;
    }

    private static Label title() {
        var label = new Label();
        label.setText(Component.translatable("menu.modjam.brightest.title").withStyle(Style.EMPTY.withBold(true)));
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
        label.setText(Component.literal("The Last Remaining Light"));
        label.textStyle(ts -> ts
                .font(ALT_FONT)
                .textColor(FLAVOR_COLOR)
                .textShadow(false)
                .fontSize(9)
                .adaptiveWidth(true)
        );
        return label;
    }

    private static UIElement ornateSeparator() {
        var wrap = new UIElement()
                .layout(l -> l.width(PANEL_WIDTH - 60).height(10)
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

    private static Button button(String key, boolean talk) {
        var btn = new Button().setText(Component.translatable(key));
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
        btn.addEventListener(UIEvents.CLICK, e -> choose(talk));
        return btn;
    }

    private static void choose(boolean talk) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(null);
        getInstance().currentScreen = null;
        if (talk) {
            startDialogue();
        } else {
            getInstance().cooldown = REFUSE_COOLDOWN;
        }
    }

    private static void startDialogue() {
        Minecraft mc = Minecraft.getInstance();
        var connection = mc.getConnection();
        if (connection != null) {
            connection.send(AcceptDealPayload.INSTANCE.toVanillaServerbound());
        }
        var controller = CutsceneClientController.getInstance();
        List<RichText> richLines = DIALOGUE_LINES.stream()
            .map(s -> RichText.parse(Component.translatable(s).getString()))
            .toList();
        DialogueSystem.getInstance().playRich(richLines,
            controller::advanceSegment,
            controller::onDialogueFinished);
    }
}