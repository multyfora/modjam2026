package net.multyfora.don.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.multyfora.don.network.SealedSunChoicePayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class SealedSunChoiceGui {
    private static final int OUTER_GOLD = 0xFF6B4A20;
    private static final int GOLD_BORDER = 0xFFD4A840;
    private static final int INNER_STONE = 0xFF3A2410;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int DEEP_BG = 0xFF120A04;
    private static final int BUTTON_TEXT = 0xFFFFF3D6;
    private static final int VIGNETTE = 0xB0000000;
    private static final int PANEL_WIDTH = 320;

    private final BlockPos pos;

    private SealedSunChoiceGui(BlockPos pos) {
        this.pos = pos;
    }

    public static void open(BlockPos pos) {
        SealedSunChoiceGui gui = new SealedSunChoiceGui(pos);
        Minecraft.getInstance().setScreenAndShow(new ModularUIScreen(gui.createUI(), Component.empty()));
    }

    private ModularUI createUI() {
        var title = new Label();
        title.setText(Component.literal("What is your choice"));
        title.textStyle(s -> s.textColor(BUTTON_TEXT).textShadow(true).fontSize(14));
        title.layout(l -> l.heightAuto().marginBottom(8));

        var helpBtn = new Button().setText(Component.literal("help Nayir and leave this world"));
        helpBtn.layout(l -> l.width(280).height(22));
        helpBtn.textStyle(ts -> ts.textColor(BUTTON_TEXT).textShadow(true).fontSize(10));
        helpBtn.style(s -> s.background(SDFRectTexture.of(0xCC3A2410).setRadius(6f).setBorderColor(DARK_GOLD)));
        helpBtn.buttonStyle(s -> s
                .baseTexture(SDFRectTexture.of(0xCC3A2410).setRadius(6f).setBorderColor(DARK_GOLD))
                .hoverTexture(SDFRectTexture.of(0xCC8B6914).setRadius(6f).setBorderColor(0xFFFFD700))
                .pressedTexture(SDFRectTexture.of(0xCC5C3A00).setRadius(6f).setBorderColor(GOLD_BORDER)));
        helpBtn.addEventListener(UIEvents.CLICK, e -> choose(true));

        var betrayBtn = new Button().setText(Component.literal("betray the brightest and live on"));
        betrayBtn.layout(l -> l.width(280).height(22));
        betrayBtn.textStyle(ts -> ts.textColor(BUTTON_TEXT).textShadow(true).fontSize(10));
        betrayBtn.style(s -> s.background(SDFRectTexture.of(0xFF2A1A1A).setRadius(6f).setBorderColor(0xFF8B0000)));
        betrayBtn.buttonStyle(s -> s
                .baseTexture(SDFRectTexture.of(0xFF2A1A1A).setRadius(6f).setBorderColor(0xFF8B0000))
                .hoverTexture(SDFRectTexture.of(0xFF5C1A1A).setRadius(6f).setBorderColor(0xFFFF4444))
                .pressedTexture(SDFRectTexture.of(0xFF3A0A0A).setRadius(6f).setBorderColor(0xFFAA0000)));
        betrayBtn.addEventListener(UIEvents.CLICK, e -> choose(false));

        var panel = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto().flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER).paddingAll(16).gapAll(10))
                .style(s -> s.background(SDFRectTexture.of(DEEP_BG).setRadius(6f)))
                .addChildren(title, helpBtn, betrayBtn);

        var innerBezel = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto().paddingAll(2))
                .style(s -> s.background(SDFRectTexture.of(INNER_STONE).setRadius(8f).setBorderColor(DARK_GOLD)))
                .addChild(panel);

        var bezel = new UIElement()
                .layout(l -> l.width(PANEL_WIDTH).heightAuto().paddingAll(3))
                .style(s -> s.background(SDFRectTexture.of(OUTER_GOLD).setRadius(10f).setBorderColor(GOLD_BORDER)))
                .addChild(innerBezel);

        var root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN).justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .style(s -> s.background(new ColorRectTexture(VIGNETTE)))
                .addChild(bezel);

        root.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                Minecraft.getInstance().gui.setScreen(null);
                e.stopPropagation();
            }
        }, true);

        var mui = ModularUI.of(UI.of(root));
        mui.shouldCloseOnEsc(true);
        mui.shouldCloseOnKeyInventory(false);
        return mui;
    }

    private void choose(boolean help) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new SealedSunChoicePayload(pos, help)));
        }
        Minecraft.getInstance().gui.setScreen(null);
    }
}