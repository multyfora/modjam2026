package net.multyfora.don.client;

import net.multyfora.don.network.DialogueEventStartPayload;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class DialogueEventClientHandler {
    private static final DialogueEventClientHandler INSTANCE = new DialogueEventClientHandler();

    private final Deque<List<String>> pending = new ArrayDeque<>();

    public static DialogueEventClientHandler getInstance() {
        return INSTANCE;
    }

    public void handle(DialogueEventStartPayload payload) {
        List<String> lines = List.copyOf(payload.lines());
        if (DialogueSystem.getInstance().isActive()) {
            pending.addLast(lines);
        } else {
            start(lines);
        }
    }

    public void tick() {
        if (!pending.isEmpty() && !DialogueSystem.getInstance().isActive()) {
            start(pending.removeFirst());
        }
    }

    public void clear() {
        pending.clear();
        BrightestVisitationManager.getInstance().end();
    }

    private void start(List<String> lines) {
        BrightestVisitationManager.getInstance().start(lines.size());
        DialogueSystem.getInstance().playMarkup(
            lines,
            () -> BrightestVisitationManager.getInstance().onLineChange(),
            () -> BrightestVisitationManager.getInstance().end()
        );
    }
}