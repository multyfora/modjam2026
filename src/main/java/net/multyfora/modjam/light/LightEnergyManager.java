package net.multyfora.modjam.light;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.modjam.block.AmethystCrystalBlockEntity;
import net.multyfora.modjam.block.SoulLightBlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class LightEnergyManager {
    public static final int RANGE = 2;

    private static final Map<Block, LightSource> SOURCES = new HashMap<>();

    private LightEnergyManager() {
    }

    public static void registerSource(Block block, double intensity, double mysticalComponent) {
        SOURCES.put(block, new LightSource(intensity, mysticalComponent));
    }

    public static LightSource getSource(Block block) {
        return SOURCES.get(block);
    }

    public static boolean isSource(Block block) {
        return SOURCES.containsKey(block);
    }

    public static LightEnergy compute(Level level, BlockPos pos) {
        double intensity = 0;
        double componentSum = 0;
        int componentCount = 0;

        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    LightSource source = SOURCES.get(level.getBlockState(pos.offset(dx, dy, dz)).getBlock());
                    if (source != null) {
                        intensity += source.intensity();
                        componentSum += source.mysticalComponent();
                        componentCount++;
                    }
                }
            }
        }

        return new LightEnergy(intensity, componentCount == 0 ? 0 : componentSum / componentCount);
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
