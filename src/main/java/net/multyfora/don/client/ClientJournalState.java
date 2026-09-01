package net.multyfora.don.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.multyfora.don.network.JournalSyncPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClientJournalState {
    private static final ClientJournalState INSTANCE = new ClientJournalState();
    private volatile List<JournalSyncPayload.Entry> entries = List.of();
    private volatile boolean initialized = false;

    private ClientJournalState() {}

    public static ClientJournalState getInstance() {
        return INSTANCE;
    }

    public void handle(JournalSyncPayload payload) {
        List<JournalSyncPayload.Entry> newEntries = List.copyOf(payload.entries());
        Set<String> oldIds = entries.stream().map(JournalSyncPayload.Entry::id).collect(Collectors.toSet());
        List<JournalSyncPayload.Entry> added = newEntries.stream().filter(e -> !oldIds.contains(e.id())).toList();
        this.entries = newEntries;
        if (!initialized) {
            initialized = true;
            return;
        }
        if (added.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            var toastManager = mc.gui.toastManager();
            for (var e : added) {
                Component title = Component.translatable("toast.don.journal_entry");
                Component subtitle;
                if (e.title() != null) {
                    subtitle = e.titleIsKey() ? Component.translatable(e.title()) : Component.literal(e.title());
                } else {
                    subtitle = Component.literal(prettyId(e.id()));
                }
                SystemToast.add(toastManager, new SystemToast.SystemToastId(5000L), title, subtitle);
            }
        });
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

    public List<JournalSyncPayload.Entry> getEntries() {
        return entries;
    }

    public List<Identifier> getDiscoveredOrdered() {
        List<Identifier> ids = new ArrayList<>();
        for (var e : entries) {
            try {
                ids.add(Identifier.parse(e.id()));
            } catch (Exception ignored) {}
        }
        return ids;
    }

    public boolean isDiscovered(Identifier id) {
        return entries.stream().anyMatch(e -> e.id().equals(id.toString()));
    }

    public List<String> getDiscoveredRaw() {
        return entries.stream().map(JournalSyncPayload.Entry::id).toList();
    }
}
