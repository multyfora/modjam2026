package net.multyfora.don.light;

import net.minecraft.core.BlockPos;

import java.util.List;

public final class LightDrainField {
    public static final int RADIUS = 8;

    private static volatile List<BlockPos> centers = List.of();

    private LightDrainField() {
    }

    public static void update(List<BlockPos> newCenters) {
        centers = List.copyOf(newCenters);
    }

    public static boolean isDrained(BlockPos pos) {
        List<BlockPos> snapshot = centers;
        if (snapshot.isEmpty()) return false;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (BlockPos center : snapshot) {
            int dx = x - center.getX();
            int dy = y - center.getY();
            int dz = z - center.getZ();
            if (dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS) {
                return true;
            }
        }
        return false;
    }
}