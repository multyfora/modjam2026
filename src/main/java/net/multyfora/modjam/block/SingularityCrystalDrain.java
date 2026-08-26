package net.multyfora.modjam.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
    private static final int SKY_EXPOSURE_LEVEL = 8;

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
                tryAbsorbCharge(level, pos);
            }
        }
    }

    private static void tryAbsorbCharge(ServerLevel level, BlockPos pos) {
        int range = LightEnergyManager.RANGE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    boolean took = false;
                    if (level.getBlockEntity(neighbor) instanceof AmethystCrystalBlockEntity crystal
                        && crystal.getCharges() > 0) {
                        took = crystal.useLight();
                    } else if (level.getBlockEntity(neighbor) instanceof SoulLightBlockEntity soul
                        && soul.getCharges() > 0) {
                        took = soul.useLight();
                    }
                    if (!took) continue;

                    modjam.LOGGER.info("[SCDrain] crystal {} absorbed a charge from {} and reverted", pos, neighbor);
                    level.setBlock(pos, amethystClusterState(), 3);
                    if (level.getBlockEntity(pos) instanceof AmethystCrystalBlockEntity reborn) {
                        reborn.setCharges(1);
                    }
                    return;
                }
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
                boolean exposed = isExposedToLight(level, pos);
                if (exposed) {
                    int accumulated = timers.getOrDefault(immutable, 0) + EXPOSURE_INTERVAL;
                    if (accumulated >= EXPOSURE_TIME) {
                        modjam.LOGGER.info("[SCDrain] reverting singularity crystal at {} after {} ticks of exposure", immutable, accumulated);
                        level.setBlock(immutable, amethystClusterState(), 3);
                        timers.remove(immutable);
                    } else {
                        timers.put(immutable, accumulated);
                        modjam.LOGGER.debug("[SCDrain] crystal {} exposed, {}/{} ticks", immutable, accumulated, EXPOSURE_TIME);
                    }
                } else {
                    if (timers.remove(immutable) != null) {
                        modjam.LOGGER.info("[SCDrain] crystal {} lost exposure, timer reset", immutable);
                    }
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
        if (level.isBrightOutside()
            && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY()) {
            return true;
        }
        return hasNearbyLightSource(level, pos);
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