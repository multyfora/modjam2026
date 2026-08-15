package net.multyfora.modjam.dialogue;

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
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.network.DialogueEventStartPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.resource.ListenerKey;

import java.util.HashMap;
import java.util.Map;

public class DialogueEventManager extends SimplePreparableReloadListener<Map<Identifier, DialogueEventDefinition>> {
    private static final String FOLDER = "dialogue_event";
    private static final String PERSISTENCE_KEY = "modjam_dialogue_events";
    private static final int CHECK_INTERVAL = 4;

    private static final DialogueEventManager INSTANCE = new DialogueEventManager();

    private volatile Map<Identifier, DialogueEventDefinition> definitions = Map.of();
    private long tickCounter;

    public static DialogueEventManager getInstance() {
        return INSTANCE;
    }

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addRetainedListener(
            ListenerKey.create(Identifier.fromNamespaceAndPath(modjam.MODID, "dialogue_events")),
            getInstance()
        );
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickServer(event.getServer());
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
                    .resultOrPartial(error -> modjam.LOGGER.error("Failed to load dialogue event {}: {}", file, error))
                    .ifPresent(definition -> map.put(id, definition));
            } catch (Exception e) {
                modjam.LOGGER.error("Failed to load dialogue event {}", file, e);
            }
        }
        return Map.copyOf(map);
    }

    @Override
    protected void apply(Map<Identifier, DialogueEventDefinition> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.definitions = map;
        modjam.LOGGER.info("Loaded {} modjam dialogue event(s)", map.size());
    }
}