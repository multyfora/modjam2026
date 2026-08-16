package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.modjam.light.LightEnergyManager;
import net.multyfora.modjam.modjam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

public class SingularityCrystalDrain {
    private static final SingularityCrystalDrain INSTANCE = new SingularityCrystalDrain();
    private static final int DRAIN_INTERVAL = 160;
    private static final int EXPOSURE_INTERVAL = 20;
    private static final int EXPOSURE_TIME = 6000;
    private static final int LIGHT_RADIUS = 7;
    private static final int SKY_OCCLUDER = 15;

    private final java.util.Map<net.minecraft.resources.ResourceKey<Level>, java.util.Map<BlockPos, Integer>> exposure = new java.util.HashMap<>();

    public static SingularityCrystalDrain getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();

        if (tick % EXPOSURE_INTERVAL == 0) {
            exposurePass(server);
        }

        if (tick % DRAIN_INTERVAL != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            List<BlockPos> crystals = CrystalTracker.crystals(level.dimension());
            for (BlockPos pos : crystals) {
                LightEnergyManager.drainCrystalsAround(level, pos);
            }
        }
    }

    private void exposurePass(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.resources.ResourceKey<Level> key = level.dimension();
            List<BlockPos> crystals = CrystalTracker.crystals(key);
            java.util.Map<BlockPos, Integer> timers = exposure.computeIfAbsent(key, k -> new java.util.HashMap<>());

            for (BlockPos pos : crystals) {
                if (!level.getBlockState(pos).is(modjam.SINGULARITY_CRYSTAL_BLOCK.get())) continue;
                BlockPos immutable = pos.immutable();
                if (isExposedToLight(level, pos)) {
                    int accumulated = timers.getOrDefault(immutable, 0) + EXPOSURE_INTERVAL;
                    if (accumulated >= EXPOSURE_TIME) {
                        level.setBlock(immutable, amethystClusterState(), 3);
                        timers.remove(immutable);
                    } else {
                        timers.put(immutable, accumulated);
                    }
                } else {
                    timers.remove(immutable);
                }
            }

            timers.keySet().retainAll(new java.util.HashSet<>(crystals));
        }
    }

    private static BlockState amethystClusterState() {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, Direction.UP);
    }

    private static boolean isExposedToLight(ServerLevel level, BlockPos pos) {
        if (level.dimensionType().hasSkyLight() && isSunUp(level) && seesOpenSky(level, pos)) {
            return true;
        }
        return hasNearbyLightSource(level, pos);
    }

    private static boolean isSunUp(Level level) {
        return level.getOverworldClockTime() % 24000L < 12000L;
    }

    private static boolean seesOpenSky(ServerLevel level, BlockPos pos) {
        for (int y = pos.getY() + 1; y < level.getMaxY(); y++) {
            BlockPos above = pos.atY(y);
            if (level.getBlockState(above).getLightDampening() >= SKY_OCCLUDER) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasNearbyLightSource(ServerLevel level, BlockPos pos) {
        for (int dx = -LIGHT_RADIUS; dx <= LIGHT_RADIUS; dx++) {
            for (int dy = -LIGHT_RADIUS; dy <= LIGHT_RADIUS; dy++) {
                for (int dz = -LIGHT_RADIUS; dz <= LIGHT_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    if (!level.isLoaded(neighbor)) continue;
                    int emission = level.getBlockState(neighbor).getLightEmission();
                    if (emission <= 0) continue;
                    int distance = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                    if (emission - distance >= 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}