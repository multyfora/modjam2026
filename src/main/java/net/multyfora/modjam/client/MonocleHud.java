package net.multyfora.modjam.client;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.multyfora.modjam.client.dialogue.RichText;
import net.multyfora.modjam.client.dialogue.RichTextElement;
import net.multyfora.modjam.light.LightDrainField;
import net.multyfora.modjam.light.LightEnergy;
import net.multyfora.modjam.light.LightEnergyManager;
import net.multyfora.modjam.modjam;

public class MonocleHud {
    private static final MonocleHud INSTANCE = new MonocleHud();

    private static final int REFRESH_INTERVAL_TICKS = 10;

    private static final int OUTER_GOLD = 0xFF6B4A20;
    private static final int GOLD_BORDER = 0xFFD4A840;
    private static final int INNER_STONE = 0xFF3A2410;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int DIALOG_BG = 0xE2170803;
    private static final int DIAMOND = 0xFFFFD700;

    private ModularUI mui;
    private RichTextElement label;
    private int refreshTimer;

    public static MonocleHud getInstance() {
        return INSTANCE;
    }

    public void tick() {
        var player = Minecraft.getInstance().player;
        boolean wearing = player != null
            && player.getItemBySlot(EquipmentSlot.HEAD).is(modjam.MYSTICAL_MONOCLE.get());

        if (!wearing) {
            if (mui != null) hideUI();
            return;
        }

        ensureUI();
        if (++refreshTimer < REFRESH_INTERVAL_TICKS) return;
        refreshTimer = 0;
        updateReadings();
    }

    public ModularUI getModularUI() {
        return mui;
    }

    private void updateReadings() {
        var player = Minecraft.getInstance().player;
        if (player == null || label == null) return;

        var pos = player.blockPosition();
        LightEnergy energy = LightEnergyManager.compute(player.level(), pos);

        String line;
        if (LightDrainField.isDrained(pos)) {
            line = String.format("{#FF5555}[DRAINED] Intensity: %.1f | Mystical: %.1f{/}",
                energy.intensity(), energy.mysticalComponent());
        } else {
            line = String.format("Intensity: {#FFAA00}%.1f{/}{#AAAAAA} |{/} Mystical: {#55FFFF}%.1f{/}",
                energy.intensity(), energy.mysticalComponent());
        }
        label.setText(RichText.parse(line));
        // bypass the typewriter reveal — show the full reading immediately
        label.setRevealChars(Integer.MAX_VALUE);
    }

    private void hideUI() {
        mui = null;
        label = null;
    }

    private void ensureUI() {
        if (mui != null) return;

        label = new RichTextElement();
        label.setText(RichText.EMPTY);
        label.setFontSize(12);
        label.setLineSpacing(2);
        label.setCentered(true);

        UIElement panel = new UIElement()
            .layout(l -> l
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5)
                .paddingAll(8)
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

        UIElement bezel = new UIElement()
            .layout(l -> l.widthPercent(100).paddingAll(3))
            .style(s -> s.background(
                SDFRectTexture.of(OUTER_GOLD).setRadius(12f).setBorderColor(GOLD_BORDER)
            ))
            .addChild(inner);

        UIElement dialogBox = new UIElement()
            .layout(l -> l
                .width(190)
                .marginTop(30)
                .marginRight(30)
                .flexDirection(FlexDirection.COLUMN)
            )
            .addChildren(bezel);

        UIElement root = new UIElement()
            .layout(l -> l
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.FLEX_START)
                .alignItems(AlignItems.FLEX_END)
            )
            .addChild(dialogBox);

        mui = ModularUI.of(UI.of(root));
        refreshTimer = REFRESH_INTERVAL_TICKS - 1;
    }

    private static UIElement ornament() {
        var wrap = new UIElement()
                .layout(l -> l.widthPercent(100).height(6)
                        .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));

        var lineLeft = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        var diamond = new UIElement()
                .layout(l -> l.width(4).height(4))
                .style(s -> s.background(SDFRectTexture.of(DIAMOND).setRadius(1f)));
        diamond.transform(t -> t.rotation(45f));

        var lineRight = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        wrap.addChildren(lineLeft, diamond, lineRight);
        return wrap;
    }
}
