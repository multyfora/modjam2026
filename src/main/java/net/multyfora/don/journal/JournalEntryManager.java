package net.multyfora.don.journal;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.multyfora.don.dialogue.DialogueTrigger;
import net.multyfora.don.don;
import net.multyfora.don.network.JournalSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.resource.ListenerKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JournalEntryManager extends SimplePreparableReloadListener<Map<Identifier, JournalEntryDefinition>> {
    private static final String FOLDER = "journal_entry";
    private static final String PERSISTENCE_KEY = "don_journal_entries";
    private static final String TIMING_KEY = "don_journal_times";
    private static final int CHECK_INTERVAL = 4;

    private static final JournalEntryManager INSTANCE = new JournalEntryManager();

    private volatile Map<Identifier, JournalEntryDefinition> definitions = Map.of();
    private long tickCounter;

    public static JournalEntryManager getInstance() {
        return INSTANCE;
    }

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addRetainedListener(
            ListenerKey.create(Identifier.fromNamespaceAndPath(don.MODID, "journal_entries")),
            getInstance()
        );
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickServer(event.getServer());
    }

    public static Set<Identifier> registeredEntries() {
        return INSTANCE.definitions.keySet();
    }

    public static JournalEntryDefinition getDefinition(Identifier id) {
        return INSTANCE.definitions.get(id);
    }

    public static Map<Identifier, JournalEntryDefinition> getDefinitions() {
        return INSTANCE.definitions;
    }

    public static boolean isDiscovered(ServerPlayer player, Identifier id) {
        for (Tag tag : discoveredList(player)) {
            if (tag instanceof StringTag string && string.value().equals(id.toString())) return true;
        }
        return false;
    }

    public static List<Identifier> getDiscoveredOrdered(ServerPlayer player) {
        List<Identifier> ids = new ArrayList<>();
        for (Tag tag : discoveredList(player)) {
            if (tag instanceof StringTag string) {
                try {
                    ids.add(Identifier.parse(string.value()));
                } catch (Exception ignored) {}
            }
        }
        CompoundTag times = player.getPersistentData().getCompoundOrEmpty(TIMING_KEY);
        ids.sort(Comparator.comparingLong(id -> times.getLong(id.toString()).orElse(Long.MAX_VALUE)));
        return ids;
    }

    public static long getDiscoveredTime(ServerPlayer player, Identifier id) {
        return player.getPersistentData().getCompoundOrEmpty(TIMING_KEY).getLong(id.toString()).orElse(-1L);
    }

    public static boolean tryDiscover(ServerPlayer player, Identifier id) {
        JournalEntryDefinition def = INSTANCE.definitions.get(id);
        if (def == null) return false;
        if (isDiscovered(player, id)) return false;
        discover(player, id);
        return true;
    }

    public static void syncToPlayer(ServerPlayer player) {
        List<JournalSyncPayload.Entry> entries = getDiscoveredOrdered(player).stream()
            .map(id -> {
                JournalEntryDefinition def = INSTANCE.definitions.get(id);
                if (def == null) return null;
                String image = def.image().map(Identifier::toString).orElse(null);
                String title = def.title().orElse(null);
                return new JournalSyncPayload.Entry(id.toString(), image, def.description(), def.descriptionIsKey(), title, def.titleIsKey());
            })
            .filter(e -> e != null)
            .toList();
        PacketDistributor.sendToPlayer(player, new JournalSyncPayload(entries));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    public static void resetPlayerProgress(ServerPlayer player) {
        player.getPersistentData().remove(PERSISTENCE_KEY);
        player.getPersistentData().remove(TIMING_KEY);
        syncToPlayer(player);
    }

    private void tickServer(MinecraftServer server) {
        if (definitions.isEmpty()) return;
        if (++tickCounter % CHECK_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(ServerPlayer player) {
        boolean anyNew = false;
        for (Map.Entry<Identifier, JournalEntryDefinition> entry : definitions.entrySet()) {
            if (isDiscovered(player, entry.getKey())) continue;
            if (matchesAll(entry.getValue(), player)) {
                discover(player, entry.getKey());
                anyNew = true;
            }
        }
        if (anyNew) {
            syncToPlayer(player);
        }
    }

    private static boolean matchesAll(JournalEntryDefinition def, ServerPlayer player) {
        for (DialogueTrigger trigger : def.triggers()) {
            if (trigger.matches(player)) return true;
        }
        return false;
    }

    private static void discover(ServerPlayer player, Identifier id) {
        ListTag list = discoveredList(player);
        list.add(StringTag.valueOf(id.toString()));
        CompoundTag times = player.getPersistentData().getCompoundOrEmpty(TIMING_KEY);
        times.putLong(id.toString(), player.level().getOverworldClockTime());
        player.getPersistentData().put(TIMING_KEY, times);
    }

    private static ListTag discoveredList(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(PERSISTENCE_KEY).orElse(null);
        if (list == null) {
            list = new ListTag();
            data.put(PERSISTENCE_KEY, list);
        }
        return list;
    }

    @Override
    protected Map<Identifier, JournalEntryDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        RegistryOps<JsonElement> ops = getRegistryLookup().createSerializationContext(JsonOps.INSTANCE);
        Map<Identifier, JournalEntryDefinition> map = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : resourceManager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath();
            Identifier id = Identifier.fromNamespaceAndPath(
                file.getNamespace(),
                path.substring(FOLDER.length() + 1, path.length() - ".json".length())
            );
            try (var reader = entry.getValue().openAsReader()) {
                var element = JsonParser.parseReader(reader);
                JournalEntryDefinition.CODEC.parse(ops, element)
                    .resultOrPartial(error -> don.LOGGER.error("Failed to load journal entry {}: {}", file, error))
                    .ifPresent(def -> map.put(id, def));
            } catch (Exception e) {
                don.LOGGER.error("Failed to load journal entry {}", file, e);
            }
        }
        return Map.copyOf(map);
    }

    @Override
    protected void apply(Map<Identifier, JournalEntryDefinition> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.definitions = map;
        don.LOGGER.info("Loaded {} journal entry(ies)", map.size());
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                syncToPlayer(player);
            }
        }
    }
}
