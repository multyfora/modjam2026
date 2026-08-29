package net.multyfora.don.client;

import net.minecraft.resources.Identifier;
import net.multyfora.don.network.JournalSyncPayload;

import java.util.ArrayList;
import java.util.List;

public final class ClientJournalState {
    private static final ClientJournalState INSTANCE = new ClientJournalState();
    private volatile List<JournalSyncPayload.Entry> entries = List.of();

    private ClientJournalState() {}

    public static ClientJournalState getInstance() {
        return INSTANCE;
    }

    public void handle(JournalSyncPayload payload) {
        this.entries = List.copyOf(payload.entries());
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
