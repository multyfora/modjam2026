package net.multyfora.don.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.multyfora.don.light.LightDrainField;
import net.multyfora.don.don;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@EventBusSubscriber(modid = don.MODID)
public final class CrystalTracker {
    private static final Object LOCK = new Object();
    private static final Map<ResourceKey<Level>, Set<net.minecraft.world.level.ChunkPos>> LOADED = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CRYSTALS = new HashMap<>();

    private CrystalTracker() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelChunk chunk = event.getChunk();
        Level level = chunk.getLevel();
        ResourceKey<Level> key = level.dimension();
        net.minecraft.world.level.ChunkPos chunkPos = chunk.getPos();

        synchronized (LOCK) {
            if (!LOADED.computeIfAbsent(key, k -> new HashSet<>()).add(chunkPos)) {
                return;
            }
            scanChunk(level, key, chunkPos);
            refreshField();
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        LevelChunk chunk = event.getChunk();
        Level level = chunk.getLevel();
        ResourceKey<Level> key = level.dimension();
        net.minecraft.world.level.ChunkPos chunkPos = chunk.getPos();

        synchronized (LOCK) {
            Set<net.minecraft.world.level.ChunkPos> loaded = LOADED.get(key);
            if (loaded != null) {
                loaded.remove(chunkPos);
            }
            Set<BlockPos> crystals = CRYSTALS.get(key);
            if (crystals != null) {
                int sx = net.minecraft.core.SectionPos.blockToSectionCoord(chunkPos.getMinBlockX());
                int sz = net.minecraft.core.SectionPos.blockToSectionCoord(chunkPos.getMinBlockZ());
                crystals.removeIf(pos -> pos.getX() >> 4 == sx && pos.getZ() >> 4 == sz);
            }
            refreshField();
        }
    }

    public static void notifyBlockChanged(Level level, BlockPos pos, BlockState previous, BlockState current) {
        if (!previous.is(don.SINGULARITY_CRYSTAL_BLOCK.get()) && !current.is(don.SINGULARITY_CRYSTAL_BLOCK.get())) {
            return;
        }

        ResourceKey<Level> key = level.dimension();
        boolean markSections = false;
        synchronized (LOCK) {
            Set<BlockPos> crystals = CRYSTALS.computeIfAbsent(key, k -> new HashSet<>());
            if (current.is(don.SINGULARITY_CRYSTAL_BLOCK.get())) {
                markSections = crystals.add(pos.immutable());
            } else {
                markSections = crystals.remove(pos);
            }
            refreshField();
        }

        if (markSections) {
            markSectionsDirty(level, pos);
        }
    }

    public static List<BlockPos> crystals(ResourceKey<Level> key) {
        synchronized (LOCK) {
            Set<BlockPos> crystals = CRYSTALS.get(key);
            return crystals == null || crystals.isEmpty() ? List.of() : new ArrayList<>(crystals);
        }
    }

    private static void scanChunk(Level level, ResourceKey<Level> key, net.minecraft.world.level.ChunkPos chunkPos) {
        Set<BlockPos> crystals = CRYSTALS.computeIfAbsent(key, k -> new HashSet<>());
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(baseX + x, y, baseZ + z);
                    if (level.getBlockState(pos).is(don.SINGULARITY_CRYSTAL_BLOCK.get())) {
                        crystals.add(pos.immutable());
                        markSectionsDirty(level, pos);
                    }
                }
            }
        }
    }

    private static void refreshField() {
        List<BlockPos> all = new ArrayList<>();
        for (Set<BlockPos> set : CRYSTALS.values()) {
            all.addAll(set);
        }
        LightDrainField.update(all);
    }

    private static void markSectionsDirty(Level level, BlockPos pos) {
        if (!level.isClientSide()) return;
        try {
            Class.forName("net.multyfora.don.client.CrystalTrackerClient").getMethod("markDirty", Level.class, BlockPos.class).invoke(null, level, pos);
        } catch (Exception ignored) {}
    }
}