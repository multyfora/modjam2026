package net.multyfora.don.dialogue;

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
import net.multyfora.don.don;
import net.multyfora.don.network.DialogueEventStartPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.resource.ListenerKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DialogueEventManager extends SimplePreparableReloadListener<Map<Identifier, DialogueEventDefinition>> {
    private static final String FOLDER = "dialogue_event";
    private static final String PERSISTENCE_KEY = "don_dialogue_events";
    private static final String TIMING_KEY = "don_dialogue_times";
    private static final int CHECK_INTERVAL = 4;

    public static final Identifier ENCHANTING_TABLE_USED = Identifier.fromNamespaceAndPath(don.MODID, "enchanting_table_used");
    public static final Identifier OVERWORLD_WELCOME = Identifier.fromNamespaceAndPath(don.MODID, "overworld_welcome");

    private static final DialogueEventManager INSTANCE = new DialogueEventManager();

    private volatile Map<Identifier, DialogueEventDefinition> definitions = Map.of();
    private long tickCounter;

    public static DialogueEventManager getInstance() {
        return INSTANCE;
    }

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addRetainedListener(
            ListenerKey.create(Identifier.fromNamespaceAndPath(don.MODID, "dialogue_events")),
            getInstance()
        );
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickServer(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.closeContainer();
            tryFire(player, ENCHANTING_TABLE_USED);
        }
    }

    public static boolean tryFire(ServerPlayer player, Identifier id) {
        DialogueEventDefinition definition = INSTANCE.definitions.get(id);
        if (definition == null) return false;
        if (definition.once() && isFired(player, id)) return false;
        fire(player, id, definition);
        return true;
    }

    public static boolean runEvent(ServerPlayer player, Identifier id) {
        DialogueEventDefinition definition = INSTANCE.definitions.get(id);
        if (definition == null) return false;
        fire(player, id, definition);
        return true;
    }

    public static Set<Identifier> registeredEvents() {
        return INSTANCE.definitions.keySet();
    }

    private void tickServer(MinecraftServer server) {
        if (definitions.isEmpty()) return;
        if (++tickCounter % CHECK_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(ServerPlayer player) {
        for (Map.Entry<Identifier, DialogueEventDefinition> entry : definitions.entrySet()) {
            DialogueEventDefinition definition = entry.getValue();
            if (definition.once() && isFired(player, entry.getKey())) continue;
            if (matchesAll(definition, player)) {
                fire(player, entry.getKey(), definition);
            }
        }
    }

    private static boolean matchesAll(DialogueEventDefinition definition, ServerPlayer player) {
        for (DialogueTrigger trigger : definition.triggers()) {
            if (!trigger.matches(player)) return false;
        }
        return true;
    }

    private static void fire(ServerPlayer player, Identifier id, DialogueEventDefinition definition) {
        if (definition.once()) {
            markFired(player, id);
        }
        recordFiredDayTime(player, id);
        PacketDistributor.sendToPlayer(player, new DialogueEventStartPayload(definition.lines()));
    }

    private static boolean isFired(ServerPlayer player, Identifier id) {
        for (Tag tag : firedList(player)) {
            if (tag instanceof StringTag string && string.value().equals(id.toString())) return true;
        }
        return false;
    }

    private static void markFired(ServerPlayer player, Identifier id) {
        ListTag list = firedList(player);
        if (isFired(player, id)) return;
        list.add(StringTag.valueOf(id.toString()));
    }

    public static void resetPlayerProgress(ServerPlayer player) {
        player.getPersistentData().remove(PERSISTENCE_KEY);
        player.getPersistentData().remove(TIMING_KEY);
    }

    public static long getFiredDayTime(ServerPlayer player, Identifier id) {
        return player.getPersistentData().getCompoundOrEmpty(TIMING_KEY)
            .getLong(id.toString()).orElse(-1L);
    }

    public static void recordFiredDayTime(ServerPlayer player, Identifier id) {
        CompoundTag times = player.getPersistentData().getCompoundOrEmpty(TIMING_KEY);
        times.putLong(id.toString(), player.level().getOverworldClockTime());
        player.getPersistentData().put(TIMING_KEY, times);
    }

    private static ListTag firedList(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(PERSISTENCE_KEY).orElse(null);
        if (list == null) {
            list = new ListTag();
            data.put(PERSISTENCE_KEY, list);
        }
        return list;
    }

    @Override
    protected Map<Identifier, DialogueEventDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        RegistryOps<JsonElement> ops = getRegistryLookup().createSerializationContext(JsonOps.INSTANCE);
        Map<Identifier, DialogueEventDefinition> map = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : resourceManager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath();
            Identifier id = Identifier.fromNamespaceAndPath(
                file.getNamespace(),
                path.substring(FOLDER.length() + 1, path.length() - ".json".length())
            );
            try (var reader = entry.getValue().openAsReader()) {
                var element = JsonParser.parseReader(reader);
                DialogueEventDefinition.CODEC.parse(ops, element)
                    .resultOrPartial(error -> don.LOGGER.error("Failed to load dialogue event {}: {}", file, error))
                    .ifPresent(definition -> map.put(id, definition));
            } catch (Exception e) {
                don.LOGGER.error("Failed to load dialogue event {}", file, e);
            }
        }
        return Map.copyOf(map);
    }

    @Override
    protected void apply(Map<Identifier, DialogueEventDefinition> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.definitions = map;
        don.LOGGER.info("Loaded {} don dialogue event(s)", map.size());
    }
}