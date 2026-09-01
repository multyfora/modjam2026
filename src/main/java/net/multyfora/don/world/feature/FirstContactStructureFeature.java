package net.multyfora.don.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.don;

public class FirstContactStructureFeature {
    private static final Identifier GRAVEYARD_ID =
        Identifier.fromNamespaceAndPath(don.MODID, "light_graveyard");
    private static final int EDGE_PADDING = 4;
    private static final int HALF_FLOOR_X = 103 / 2 + EDGE_PADDING;
    private static final int HALF_FLOOR_Z = 208 / 2 + EDGE_PADDING;

    public static void placeAtSpawn(ServerLevel level, BlockPos center) {
        if (isAlreadyPlaced(level, center)) {
            fillLightAroundBrightest(level, new Vec3(0.5, 8.0, 19.5));
            return;
        }

        int ox = center.getX();
        int oz = center.getZ();

        buildSafetyFloor(level, ox, oz);
        placeGraveyardTemplate(level, ox, oz);
        fillLightAroundBrightest(level, new Vec3(0.5, 8.0, 19.5));
        relightArea(level, ox, oz);
    }

    public static void fillLightAroundBrightest(ServerLevel level, Vec3 pos) {
        var lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        BlockPos center = BlockPos.containing(pos);
        int radius = 5;
        int rsq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rsq) continue;
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlock(p, lightState, 3);
                    }
                }
            }
        }
    }

    public static void relightArea(ServerLevel level, int ox, int oz) {
        var lightEngine = level.getLightEngine();
        int minChunkX = (ox - HALF_FLOOR_X) >> 4;
        int maxChunkX = (ox + HALF_FLOOR_X) >> 4;
        int minChunkZ = (oz - HALF_FLOOR_Z) >> 4;
        int maxChunkZ = (oz + HALF_FLOOR_Z) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                lightEngine.propagateLightSources(new ChunkPos(chunkX, chunkZ));
            }
        }
    }

    private static boolean isAlreadyPlaced(ServerLevel level, BlockPos origin) {
        for (int dy = -1; dy <= 1; dy++) {
            if (!level.getBlockState(new BlockPos(origin.getX(), dy, origin.getZ())).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static void buildSafetyFloor(ServerLevel level, int ox, int oz) {
        int halfX = HALF_FLOOR_X;
        int halfZ = HALF_FLOOR_Z;
        for (int x = -halfX; x <= halfX; x++) {
            for (int z = -halfZ; z <= halfZ; z++) {
                level.setBlock(new BlockPos(ox + x, -1, oz + z), Blocks.CONCRETE.black().defaultBlockState(), 3);
            }
        }
    }

    private static void placeGraveyardTemplate(ServerLevel level, int ox, int oz) {
        try {
            var manager = level.getStructureManager();
            var template = manager.get(GRAVEYARD_ID).orElse(null);
            if (template == null) {
                don.LOGGER.warn("Could not load structure {}", GRAVEYARD_ID);
                return;
            }
            var size = template.getSize();
            var offset = new BlockPos(ox - size.getX() / 2, 0, oz - size.getZ() / 2);
            var settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE);
            template.placeInWorld(level, offset, offset, settings, level.getRandom(), 3);
            replaceWallsWithUrns(level, offset, size);
        } catch (Exception e) {
            don.LOGGER.warn("Could not place structure {}", GRAVEYARD_ID, e);
        }
    }

    private static void replaceWallsWithUrns(ServerLevel level, BlockPos origin, Vec3i size) {
        var urnState = don.LIGHT_URN_BLOCK.get().defaultBlockState();
        int replaced = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.BLACKSTONE_WALL)) {
                        level.setBlock(pos, urnState, 3);
                        replaced++;
                    }
                }
            }
        }
        don.LOGGER.info("Replaced {} blackstone wall(s) with light urns", replaced);
    }
}
