package net.multyfora.don.cutscene;

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
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.don;
import net.multyfora.don.dialogue.DialogueEventManager;
import net.multyfora.don.network.StartCutscenePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.resource.ListenerKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CutsceneManager extends SimplePreparableReloadListener<Map<Identifier, CutsceneDefinition>> {
    private static final String FOLDER = "cutscene";
    private static final String PERSISTENCE_KEY = "don_cutscenes";
    private static final int CHECK_INTERVAL = 4;

    public static final Identifier ACCEPTED_DEAL = Identifier.fromNamespaceAndPath(don.MODID, "accepted_deal");

    private static final CutsceneManager INSTANCE = new CutsceneManager();

    private volatile Map<Identifier, CutsceneDefinition> definitions = Map.of();
    private long tickCounter;

    public static CutsceneManager getInstance() {
        return INSTANCE;
    }

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addRetainedListener(
            ListenerKey.create(Identifier.fromNamespaceAndPath(don.MODID, "cutscenes")),
            getInstance()
        );
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickServer(event.getServer());
    }

    public static boolean tryFire(ServerPlayer player, Identifier id) {
        CutsceneDefinition definition = INSTANCE.definitions.get(id);
        if (definition == null) return false;
        if (definition.once() && isFired(player, id)) return false;
        fire(player, id, definition);
        return true;
    }

    public static boolean runEvent(ServerPlayer player, Identifier id) {
        CutsceneDefinition definition = INSTANCE.definitions.get(id);
        if (definition == null) return false;
        fire(player, id, definition);
        return true;
    }

    public static Set<Identifier> registeredCutscenes() {
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
        for (Map.Entry<Identifier, CutsceneDefinition> entry : definitions.entrySet()) {
            CutsceneDefinition definition = entry.getValue();
            if (definition.once() && isFired(player, entry.getKey())) continue;
            boolean matches = true;
            for (var trigger : definition.triggers()) {
                if (!trigger.matches(player)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                fire(player, entry.getKey(), definition);
            }
        }
    }

    private static void fire(ServerPlayer player, Identifier id, CutsceneDefinition definition) {
        if (definition.once()) {
            markFired(player, id);
        }
        DialogueEventManager.recordFiredDayTime(player, id);
        var frames = definition.keyframes().stream()
            .map(CutsceneManager::toFrame)
            .toList();
        PacketDistributor.sendToPlayer(player,
            new StartCutscenePayload(id.toString(), definition.durationTicks(), frames, definition.lines(),
                definition.lineSynced()));
    }

    private static StartCutscenePayload.Frame toFrame(CutsceneKeyframe keyframe) {
        Vec3 pos = keyframe.pos();
        Vec3 lookAt = keyframe.lookAt().orElse(Vec3.ZERO);
        Vec3 rot = keyframe.rot().orElse(Vec3.ZERO);
        return new StartCutscenePayload.Frame(
            keyframe.time(),
            pos.x, pos.y, pos.z,
            keyframe.lookAt().isPresent(), lookAt.x, lookAt.y, lookAt.z,
            keyframe.rot().isPresent(), (float) rot.x, (float) rot.y
        );
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
    protected Map<Identifier, CutsceneDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        RegistryOps<JsonElement> ops = getRegistryLookup().createSerializationContext(JsonOps.INSTANCE);
        Map<Identifier, CutsceneDefinition> map = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : resourceManager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath();
            Identifier id = Identifier.fromNamespaceAndPath(
                file.getNamespace(),
                path.substring(FOLDER.length() + 1, path.length() - ".json".length())
            );
            try (var reader = entry.getValue().openAsReader()) {
                var element = JsonParser.parseReader(reader);
                CutsceneDefinition.CODEC.parse(ops, element)
                    .resultOrPartial(error -> don.LOGGER.error("Failed to load cutscene {}: {}", file, error))
                    .ifPresent(definition -> map.put(id, definition));
            } catch (Exception e) {
                don.LOGGER.error("Failed to load cutscene {}", file, e);
            }
        }
        return Map.copyOf(map);
    }

    @Override
    protected void apply(Map<Identifier, CutsceneDefinition> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.definitions = map;
        don.LOGGER.info("Loaded {} don cutscene(s)", map.size());
    }
}
