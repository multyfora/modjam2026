package net.multyfora.don.light;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.don.block.AmethystCrystalBlockEntity;
import net.multyfora.don.block.SoulLightBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class LightEnergyManager {
    public static final int RANGE = 2;

    private record RegisteredSource(LightSource source, Predicate<BlockState> active) {}

    private static final Map<Block, RegisteredSource> SOURCES = new HashMap<>();

    private LightEnergyManager() {
    }

    public static void registerSource(Block block, double intensity, double mysticalComponent) {
        registerSource(block, intensity, mysticalComponent, state -> true);
    }

    public static void registerSource(Block block, double intensity, double mysticalComponent,
                                      Predicate<BlockState> active) {
        SOURCES.put(block, new RegisteredSource(new LightSource(intensity, mysticalComponent), active));
    }

    public static LightSource getSource(Block block) {
        RegisteredSource registered = SOURCES.get(block);
        return registered == null ? null : registered.source();
    }

    public static boolean isSource(Block block) {
        return SOURCES.containsKey(block);
    }

    public static boolean isActiveSource(BlockState state) {
        RegisteredSource registered = SOURCES.get(state.getBlock());
        if (registered != null && registered.active().test(state)) return true;
        return false;
    }

    public static boolean isActiveSource(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isActiveSource(state)) return true;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ForwardingLightSource fwd) {
            return fwd.forwardedEnergy().isPresent();
        }
        return false;
    }

    public static LightEnergy compute(Level level, BlockPos pos) {
        double[] intensity = {0};
        double[] componentSum = {0};
        int[] componentCount = {0};
        Set<BlockPos> seenSources = new HashSet<>();
        Set<BlockPos> visitedConduits = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(neighbor);
                    if (be instanceof ForwardingLightSource fwd && fwd.forwardedEnergy().isPresent()) {
                        if (visitedConduits.add(neighbor)) queue.add(neighbor);
                        continue;
                    }
                    collectRealSource(level, neighbor, seenSources, intensity, componentSum, componentCount);
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos conduitPos = queue.poll();
            for (int dx = -RANGE; dx <= RANGE; dx++) {
                for (int dy = -RANGE; dy <= RANGE; dy++) {
                    for (int dz = -RANGE; dz <= RANGE; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos sourcePos = conduitPos.offset(dx, dy, dz);
                        if (seenSources.contains(sourcePos)) continue;
                        BlockEntity be = level.getBlockEntity(sourcePos);
                        if (be instanceof ForwardingLightSource) continue;
                        collectRealSource(level, sourcePos, seenSources, intensity, componentSum, componentCount);
                    }
                }
            }
            for (int dx = -RANGE; dx <= RANGE; dx++) {
                for (int dy = -RANGE; dy <= RANGE; dy++) {
                    for (int dz = -RANGE; dz <= RANGE; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = conduitPos.offset(dx, dy, dz);
                        if (visitedConduits.contains(next)) continue;
                        BlockEntity nbe = level.getBlockEntity(next);
                        if (nbe instanceof ForwardingLightSource nFwd && nFwd.forwardedEnergy().isPresent()) {
                            visitedConduits.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }

        return new LightEnergy(intensity[0], componentCount[0] == 0 ? 0 : componentSum[0] / componentCount[0]);
    }

    private static void collectRealSource(Level level, BlockPos pos, Set<BlockPos> seen, double[] intensity, double[] sum, int[] count) {
        if (seen.contains(pos)) return;
        BlockState state = level.getBlockState(pos);
        RegisteredSource registered = SOURCES.get(state.getBlock());
        if (registered == null || !registered.active().test(state)) return;
        BlockEntity be = level.getBlockEntity(pos);
        LightSource source = registered.source();
        if (be instanceof TunableLightSource tunable) {
            source = new LightSource(source.intensity(), tunable.tunedMystical());
        }
        seen.add(pos);
        intensity[0] += source.intensity();
        sum[0] += source.mysticalComponent();
        count[0]++;
    }

    public static void drainAll(Level level, BlockPos pos) {
        drainCrystalsAround(level, pos);
    }

    /**
     * Drains one charge from every charged crystal around the given position.
     * A crystal that runs out of charges collapses into a singularity crystal
     * and stops emitting light.
     */
    public static void drainCrystalsAround(Level level, BlockPos pos) {
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(neighbor);
                    if (!isSource(state.getBlock())) continue;
                    switch (level.getBlockEntity(neighbor)) {
                        case AmethystCrystalBlockEntity crystal -> crystal.useLight();
                        case SoulLightBlockEntity soul -> soul.useLight();
                        default -> { }
                    }
                }
            }
        }
    }
}
