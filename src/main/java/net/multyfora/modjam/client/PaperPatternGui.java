package net.multyfora.modjam.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.multyfora.modjam.lightweaver.LightWeaverShapes;
import net.multyfora.modjam.lightweaver.WeaverPaper;
import net.multyfora.modjam.network.SavePaperPatternPayload;

public final class PaperPatternGui {

    private static final Identifier PAPER_TEXTURE =
        Identifier.fromNamespaceAndPath("modjam", "textures/gui/paper_page.png");

    private static final int CELL_EMPTY = 0x257B5327;
    private static final int CELL_FILLED = 0xFF7B5327;

    private final int hand;
    private final boolean[] cells;

    private PaperPatternGui(int hand, boolean[] cells) {
        this.hand = hand;
        this.cells = cells;
    }

    public static void open(int hand, ItemStack paper) {
        boolean[] cells = new boolean[LightWeaverShapes.GRID_SIZE * LightWeaverShapes.GRID_SIZE];
        String packed = WeaverPaper.readPattern(paper);
        if (packed != null) {
            cells = LightWeaverShapes.unpack(packed);
        }
        PaperPatternGui gui = new PaperPatternGui(hand, cells);
        Minecraft.getInstance().setScreenAndShow(new Screen(gui.createUI(), Component.empty(), gui));
    }

    private ModularUI createUI() {
        int[] heldButton = {-1};

        UIElement grid = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.COLUMN));
        for (int row = 0; row < LightWeaverShapes.GRID_SIZE; row++) {
            UIElement rowElement = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW));
            for (int col = 0; col < LightWeaverShapes.GRID_SIZE; col++) {
                final int r = row;
                final int c = col;
                UIElement cell = new UIElement()
                        .layout(l -> l.width(24).height(24))
                        .style(s -> s.background(cellTexture(cells, r, c)));
                cell.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                    if (heldButton[0] >= 0) {
                        setCell(cell, cells, r, c, heldButton[0] == 0);
                    }
                });
                cell.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    heldButton[0] = e.button;
                    setCell(cell, cells, r, c, e.button == 0);
                    e.stopPropagation();
                });
                rowElement.addChild(cell);
            }
            grid.addChild(rowElement);
        }

        UIElement panel = new UIElement()
                .layout(l -> l.width(250).height(310).flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .style(s -> s.background(SpriteTexture.of(PAPER_TEXTURE)))
                .addChild(grid);

        UIElement root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .addChild(panel);

        root.addEventListener(UIEvents.MOUSE_UP, e -> heldButton[0] = -1);

        return ModularUI.of(UI.of(root));
    }

    private void save() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new SavePaperPatternPayload(hand, LightWeaverShapes.pack(cells)).toVanillaServerbound());
        }
    }

    private static ColorRectTexture cellTexture(boolean[] cells, int row, int col) {
        return new ColorRectTexture(cells[row * LightWeaverShapes.GRID_SIZE + col] ? CELL_FILLED : CELL_EMPTY);
    }

    private static void setCell(UIElement cell, boolean[] cells, int row, int col, boolean filled) {
        cells[row * LightWeaverShapes.GRID_SIZE + col] = filled;
        cell.style(s -> s.background(cellTexture(cells, row, col)));
    }

    private static final class Screen extends ModularUIScreen {

        private final PaperPatternGui gui;

        private Screen(ModularUI modularUI, Component component, PaperPatternGui gui) {
            super(modularUI, component);
            this.gui = gui;
        }

        @Override
        public void onClose() {
            gui.save();
            super.onClose();
        }
    }
}