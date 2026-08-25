package net.multyfora.modjam.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class JournalGui {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("modjam", "textures/gui/journal_gui.png");
    private static final Identifier PAGE_FLIP_LEFT = Identifier.fromNamespaceAndPath("modjam", "textures/gui/page_flip_left_one.png");
    private static final Identifier PAGE_FLIP_LEFT_HOVER = Identifier.fromNamespaceAndPath("modjam", "textures/gui/page_flip_left_two.png");
    private static final Identifier PAGE_FLIP_RIGHT = Identifier.fromNamespaceAndPath("modjam", "textures/gui/page_flip_right_one.png");
    private static final Identifier PAGE_FLIP_RIGHT_HOVER = Identifier.fromNamespaceAndPath("modjam", "textures/gui/page_flip_right_two.png");
    private static final Identifier PAGE_MARKER = Identifier.fromNamespaceAndPath("modjam", "textures/gui/page_marker.png");
    private static final String NOTES_KEY = "modjam_notes";
    private static final float FONT_RATIO = 9f / 480f;
    private static final float NOTE_FONT_RATIO = 4f / 240f;

    private final ItemStack journalStack;
    private final List<Page> pages;
    private final List<List<StickyNote>> pageNotes;
    private int currentPage;

    private Label textLabel;
    private Label pageLabel;
    private UIElement imageBox;
    private UIElement book;
    private UIElement notesLayer;
    private UIElement editorPanel;
    private StickyNoteEditor editor;
    private final List<Label> noteLabels = new ArrayList<>();

    private boolean stickMode;
    private StickyNote draft;
    private UIElement ghost;

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreenAndShow(new ModularUIScreen(new JournalGui(stack).createUI(), Component.empty()));
    }

    private JournalGui(ItemStack journalStack) {
        this.journalStack = journalStack;
        this.pages = List.of(
                new Page(null, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."),
                new Page(null, "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
                new Page(null, "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit.")
        );
        this.pageNotes = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            pageNotes.add(new ArrayList<>());
        }
        this.currentPage = 0;
        loadNotes();
    }

    private record Page(@Nullable Identifier image, String text) {}

    private ModularUI createUI() {
        var root = new UIElement()
                .layout(l -> l
                        .widthPercent(100)
                        .heightPercent(100)
                        .flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER)
                        .alignItems(AlignItems.CENTER)
                );

        book = new UIElement() {
            @Override
            protected void onLayoutChanged() {
                super.onLayoutChanged();
                updateTextScale(getSizeWidth());
            }
        };
        book.layout(l -> l
                .widthPercent(60)
                .aspectRatio(1f)
                .positionType(TaffyPosition.RELATIVE)
        );
        book.style(s -> s
                .background(SpriteTexture.of(BACKGROUND))
        );

        imageBox = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(8.6f)
                        .topPercent(21.5f)
                        .widthPercent(38.3f)
                        .heightPercent(60.5f)
                );
        book.addChild(imageBox);

        textLabel = new Label();
        textLabel.setText(Component.literal(pages.get(0).text()));
        textLabel.textStyle(ts -> ts
                .textColor(0xFF2B1A0D)
                .textShadow(false)
                .fontSize(9)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
        );

        var textArea = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(53.1f)
                        .topPercent(21.5f)
                        .widthPercent(40.6f)
                        .heightPercent(60.5f)
                        .paddingLeftPercent(3f)
                );
        textArea.addChild(textLabel);
        book.addChild(textArea);

        pageLabel = new Label();
        pageLabel.setText(Component.literal(pageText()));
        pageLabel.textStyle(ts -> ts.textColor(0xFF2B1A0D).textShadow(false).fontSize(8));

        var marker = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(38f)
                        .bottomPercent(18f)
                        .heightPercent(3.5f)
                        .aspectRatio(1f)
                )
                .style(s -> s.background(SpriteTexture.of(PAGE_MARKER)));
        book.addChild(marker);

        var indicatorContainer = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(40f)
                        .bottomPercent(17f)
                        .widthPercent(20f)
                        .heightAuto()
                        .alignItems(AlignItems.CENTER)
                );
        indicatorContainer.addChild(pageLabel);
        book.addChild(indicatorContainer);

        notesLayer = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(0f)
                        .topPercent(0f)
                        .widthPercent(100f)
                        .heightPercent(100f)
                );
        book.addChild(notesLayer);

        var prevButton = createFlipButton(PAGE_FLIP_LEFT, PAGE_FLIP_LEFT_HOVER, e -> previousPage());
        prevButton.layout(l -> l.leftPercent(4.75f));
        prevButton.layout(b -> b.bottomPercent(19f));
        book.addChild(prevButton);

        var nextButton = createFlipButton(PAGE_FLIP_RIGHT, PAGE_FLIP_RIGHT_HOVER, e -> nextPage());
        nextButton.layout(l -> l.rightPercent(4.5f));
        nextButton.layout(b -> b.bottomPercent(19f));
        book.addChild(nextButton);


        book.addEventListener(UIEvents.MOUSE_MOVE, this::onBookMouseMove, true);
        book.addEventListener(UIEvents.CLICK, this::onBookClick, true);

        root.addChild(book);

        editor = new StickyNoteEditor(this::enterStickMode);
        editorPanel = editor.build();
        editorPanel.layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .right(52)
                .top(8)
        );
        editorPanel.setVisible(false);
        root.addChild(editorPanel);

        var sidebar = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .right(0)
                        .top(0)
                        .heightPercent(100)
                        .width(48)
                        .flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER)
                        .alignItems(AlignItems.CENTER)
                );
        var tab = new Button().setText("Note");
        tab.layout(l -> l.width(40).height(18));
        tab.textStyle(ts -> ts.textShadow(false));
        tab.addEventListener(UIEvents.CLICK, e -> toggleEditor());
        sidebar.addChild(tab);
        root.addChild(sidebar);

        renderNotes();
        updateActiveZones(prevButton, nextButton);

        return ModularUI.of(UI.of(root));
    }

    private static UIElement createFlipButton(Identifier base, Identifier hover, UIEventListener onClick) {
        var button = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .bottomPercent(18f)
                        .widthPercent(10f)
                        .aspectRatio(1f)
                )
                .style(s -> s.background(SpriteTexture.of(base)));
        button.addEventListener(UIEvents.MOUSE_ENTER,
                e -> button.style(s -> s.background(SpriteTexture.of(hover))));
        button.addEventListener(UIEvents.MOUSE_LEAVE,
                e -> button.style(s -> s.background(SpriteTexture.of(base))));
        button.addEventListener(UIEvents.CLICK, onClick);
        return button;
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateContent();
            playPageTurnSound();
        }
    }

    private void nextPage() {
        if (currentPage < pages.size() - 1) {
            currentPage++;
            updateContent();
            playPageTurnSound();
        }
    }

    private void toggleEditor() {
        if (stickMode) {
            exitStickMode();
            return;
        }
        boolean visible = !editorPanel.isVisible();
        editorPanel.setVisible(visible);
        if (visible) {
            editor.reset();
        }
    }


    private void enterStickMode(StickyNote note) {
        stickMode = true;
        draft = note;
        editorPanel.setVisible(false);
        ghost = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(40f)
                        .topPercent(40f)
                        .widthPercent(StickyNote.NOTE_WIDTH * 100f)
                        .aspectRatio(note.skewed() ? StickyNote.SKEWED_ASPECT : 1f)
                )
                .style(s -> s
                        .opacity(0.7f)
                        .background(SpriteTexture.of(NoteTextures.get(note.color(), note.skewed())))
                );
        ghost.setFocusable(true);
        ghost.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) exitStickMode();
        });
        book.addChild(ghost);
        ghost.focus();
    }

    private void exitStickMode() {
        stickMode = false;
        draft = null;
        if (ghost != null) {
            ghost.removeSelf();
            ghost = null;
        }
    }

    private void onBookMouseMove(UIEvent event) {
        if (!stickMode || ghost == null) return;
        ghost.setVisible(true);
        float bookWidth = book.getSizeWidth();
        float bookHeight = book.getSizeHeight();
        if (bookWidth <= 0 || bookHeight <= 0) return;
        float noteWidth = StickyNote.NOTE_WIDTH;
        float noteHeight = noteWidth * (draft.skewed() ? StickyNote.SKEWED_ASPECT : 1f);
        float x = (event.x - book.getPositionX()) / bookWidth;
        float y = (event.y - book.getPositionY()) / bookHeight;
        x = Math.max(0f, Math.min(1f - noteWidth, x));
        y = Math.max(0f, Math.min(1f - noteHeight, y));
        float finalX = x;
        float finalY = y;
        ghost.layout(l -> l.leftPercent(finalX * 100f).topPercent(finalY * 100f));
    }

    private void onBookClick(UIEvent event) {
        if (!stickMode || ghost == null) return;
        event.stopPropagation();
        if (event.button == 0) {
            var layout = ghost.getTaffyLayout();
            float x = layout.location().x / book.getSizeWidth();
            float y = layout.location().y / book.getSizeHeight();
            boolean skewed = ThreadLocalRandom.current().nextBoolean();
            placeNote(new StickyNote(x, y, draft.color(), draft.text(), draft.strokes(), skewed));
        }
        exitStickMode();
    }



    private void placeNote(StickyNote note) {
        pageNotes.get(currentPage).add(note);
        renderNotes();
        saveNotes();
        playPageTurnSound();
    }

    private void removeNote(StickyNote note, UIElement element) {
        pageNotes.get(currentPage).remove(note);
        notesLayer.removeChild(element);
        saveNotes();
    }

    private void renderNotes() {
        notesLayer.clearAllChildren();
        noteLabels.clear();
        for (var note : pageNotes.get(currentPage)) {
            notesLayer.addChild(createNoteElement(note));
        }
        updateNoteFonts();
    }

    private UIElement createNoteElement(StickyNote note) {
        var element = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(note.x() * 100f)
                        .topPercent(note.y() * 100f)
                        .widthPercent(StickyNote.NOTE_WIDTH * 100f)
                        .aspectRatio(note.skewed() ? StickyNote.SKEWED_ASPECT : 1f)
                )
                .style(s -> s.background(SpriteTexture.of(NoteTextures.get(note.color(), note.skewed()))));

        var label = new Label();
        label.setText(Component.literal(note.text()));
        label.textStyle(ts -> ts
                .textColor(0xFF3E2723)
                .textShadow(false)
                .fontSize(6)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
        );
        noteLabels.add(label);

        var textWrap = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(16f)
                        .topPercent(28f)
                        .widthPercent(68f)
                        .heightPercent(65f)
                )
                .setOverflowVisible(false);
        textWrap.addChild(label);
        element.addChild(textWrap);

        var canvas = StrokeCanvas.display(note.strokes());
        canvas.layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .leftPercent(10f)
                .topPercent(12f)
                .widthPercent(80f)
                .heightPercent(80f)
        );
        element.addChild(canvas);

        element.addEventListener(UIEvents.CLICK, e -> {
            if (e.button == 1) {
                e.stopPropagation();
                removeNote(note, element);
            }
        }, true);
        return element;
    }

    private void updateNoteFonts() {
        float size = book.getSizeWidth() * NOTE_FONT_RATIO;
        for (var label : noteLabels) {
            if (Math.abs(size - label.getTextStyle().fontSize()) > 0.01f) {
                label.textStyle(ts -> ts.fontSize(size));
            }
        }
    }


    private void loadNotes() {
        var custom = journalStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var pagesTag = custom.copyTag().getListOrEmpty(NOTES_KEY);
        for (int i = 0; i < pagesTag.size() && i < pageNotes.size(); i++) {
            var pageTag = pagesTag.getListOrEmpty(i);
            for (int j = 0; j < pageTag.size(); j++) {
                pageNotes.get(i).add(StickyNote.load(pageTag.getCompoundOrEmpty(j)));
            }
        }
    }

    private void saveNotes() {
        var pagesTag = new ListTag();
        for (var page : pageNotes) {
            var pageTag = new ListTag();
            for (var note : page) {
                pageTag.add(note.save());
            }
            pagesTag.add(pageTag);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, journalStack, tag -> tag.put(NOTES_KEY, pagesTag));
    }


    private void updateTextScale(float bookWidth) {
        float fontSize = bookWidth * FONT_RATIO;
        if (Math.abs(fontSize - textLabel.getTextStyle().fontSize()) > 0.01f) {
            textLabel.textStyle(ts -> ts.fontSize(fontSize));
        }
        updateNoteFonts();
    }

    private void updateContent() {
        Page page = pages.get(currentPage);
        textLabel.setText(Component.literal(page.text()));
        if (page.image() != null) {
            imageBox.style(s -> s.background(SpriteTexture.of(page.image())));
        }
        pageLabel.setText(Component.literal(pageText()));
        renderNotes();
    }

    private void updateActiveZones(UIElement prevZone, UIElement nextZone) {
        prevZone.setActive(currentPage > 0);
        nextZone.setActive(currentPage < pages.size() - 1);
    }

    private String pageText() {
        return "Page " + (currentPage + 1) + " / " + pages.size();
    }

    private void playPageTurnSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}
