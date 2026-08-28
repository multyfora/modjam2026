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
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.mojang.blaze3d.platform.NativeImage;
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
import net.multyfora.modjam.network.JournalOpenPayload;
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
    private static final String NOTES_KEY = "modjam_notes";
    private static final float FONT_RATIO = 9f / 480f;
    private static final float NOTE_FONT_RATIO = 4f / 240f;

    private final ItemStack journalStack;
    private final List<Page> pages;
    private final List<List<StickyNote>> pageNotes;
    private int currentPage;

    private Label textLabel;
    private UIElement imageBox;
    private UIElement imagePanel;
    private Label titleLabel;
    private UIElement book;
    private UIElement notesLayer;
    private UIElement editorPanel;
    private UIElement prevButton;
    private UIElement nextButton;
    private StickyNoteEditor editor;
    private final List<Label> noteLabels = new ArrayList<>();

    private boolean stickMode;
    private StickyNote draft;
    private UIElement ghost;

    public static void open(ItemStack stack) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new JournalOpenPayload().toVanillaServerbound());
        }
        Minecraft.getInstance().setScreenAndShow(new ModularUIScreen(new JournalGui(stack).createUI(), Component.empty()));
    }

    private JournalGui(ItemStack journalStack) {
        this.journalStack = journalStack;
        this.pages = buildPages();
        this.pageNotes = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            pageNotes.add(new ArrayList<>());
        }
        this.currentPage = 0;
        loadNotes();
    }

    private static List<Page> buildPages() {
        var entries = ClientJournalState.getInstance().getEntries();
        if (entries.isEmpty()) {
            return List.of(new Page(null, Component.translatable("journal.modjam.empty"), Component.empty()));
        }
        List<Page> list = new ArrayList<>();
        for (var e : entries) {
            Identifier img = null;
            if (e.image() != null) {
                try {
                    img = Identifier.parse(e.image());
                } catch (Exception ignored) {}
            }
            Component desc = e.descriptionIsKey() ? Component.translatable(e.description()) : Component.literal(e.description());
            Component title;
            if (e.title() != null) {
                title = e.titleIsKey() ? Component.translatable(e.title()) : Component.literal(e.title());
            } else {
                title = Component.literal(prettyId(e.id()));
            }
            list.add(new Page(img, desc, title));
        }
        return List.copyOf(list);
    }

    private static String prettyId(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == '_' || c == '-') {
                sb.append(' ');
                cap = true;
            } else {
                sb.append(cap ? Character.toUpperCase(c) : c);
                cap = false;
            }
        }
        return sb.toString();
    }

    private record Page(@Nullable Identifier image, Component text, Component title) {}

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

        imagePanel = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(3f)
                        .topPercent(15f)
                        .widthPercent(47f)
                        .bottomPercent(15f)
                        .flexDirection(FlexDirection.COLUMN)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER)
                        .gapAll(8f)
                );
        book.addChild(imagePanel);

        titleLabel = new Label();
        titleLabel.textStyle(ts -> ts
                .textColor(0xFF2B1A0D)
                .textShadow(false)
                .fontSize(16)
        );
        titleLabel.layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .leftPercent(7f)
                .topPercent(21.5f)
                .widthAuto()
                .heightAuto()
        );
        book.addChild(titleLabel);

        imageBox = new UIElement()
                .layout(l -> l
                        .widthPercent(72f)
                        .alignSelf(AlignItems.CENTER)
                );
        imagePanel.addChild(imageBox);

        textLabel = new Label();
        textLabel.setText(pages.get(0).text());
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

        notesLayer = new UIElement()
                .layout(l -> l
                        .positionType(TaffyPosition.ABSOLUTE)
                        .leftPercent(0f)
                        .topPercent(0f)
                        .widthPercent(100f)
                        .heightPercent(100f)
                );
        book.addChild(notesLayer);

        prevButton = createFlipButton(PAGE_FLIP_LEFT, PAGE_FLIP_LEFT_HOVER, e -> previousPage());
        prevButton.layout(l -> l.leftPercent(4.75f));
        prevButton.layout(b -> b.bottomPercent(19f));
        book.addChild(prevButton);

        nextButton = createFlipButton(PAGE_FLIP_RIGHT, PAGE_FLIP_RIGHT_HOVER, e -> nextPage());
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

        updateContent();

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



    private static float getImageAspectRatio(Identifier location) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) return -1f;
            try (var stream = resource.get().open()) {
                var image = NativeImage.read(stream);
                int w = image.getWidth();
                int h = image.getHeight();
                image.close();
                return h > 0 ? (float) w / h : -1f;
            }
        } catch (Exception ignored) {
            return -1f;
        }
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
        textLabel.setText(page.text());
        titleLabel.setText(page.title().copy().withStyle(net.minecraft.ChatFormatting.BOLD));
        if (page.image() != null) {
            var sprite = SpriteTexture.of(page.image());
            imageBox.style(s -> s.background(sprite));
            float ratio = getImageAspectRatio(page.image());
            if (ratio > 0) {
                imageBox.layout(l -> l.aspectRatio(ratio));
            }
            imageBox.setVisible(true);
        } else {
            imageBox.setVisible(false);
        }
        renderNotes();
        updateTextScale(book != null ? book.getSizeWidth() : 0);
        updateActiveZones();
    }

    private void updateActiveZones() {
        if (prevButton != null) prevButton.setVisible(currentPage > 0);
        if (nextButton != null) nextButton.setVisible(currentPage < pages.size() - 1);
    }

    private void playPageTurnSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}