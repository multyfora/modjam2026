package net.multyfora.modjam.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.multyfora.modjam.modjam;

public class FirstContactStructureFeature {
    private static final int HALF = 10;

    public static void placeAtSpawn(ServerLevel level) {
        BlockPos origin = findOrigin(level);
        if (isAlreadyPlaced(level, origin)) return;

        int ox = origin.getX();
        int oz = origin.getZ();
        int oy = 0;

        buildFloor(level, ox, oz, oy);
        buildInterior(level, ox, oz, oy);
        buildWalls(level, ox, oz, oy);
        tryPlaceDesertPyramidTemplate(level, ox, oz, oy);
        buildPyramidTiers(level, ox, oz, oy);
        buildTreasureRoom(level, ox, oz, oy, level.getRandom());
    }

    private static BlockPos findOrigin(ServerLevel level) {
        try {
            return level.getRespawnData().pos();
        } catch (Exception e) {
            return BlockPos.ZERO;
        }
    }

    private static boolean isAlreadyPlaced(ServerLevel level, BlockPos origin) {
        return level.getBlockState(origin.above()).is(Blocks.SANDSTONE);
    }

    private static void tryPlaceDesertPyramidTemplate(ServerLevel level, int ox, int oz, int oy) {
        try {
            var manager = level.getStructureManager();
            var id = Identifier.withDefaultNamespace("desert_pyramid/desert_pyramid");
            var opt = manager.get(id);
            if (opt.isPresent()) {
                var template = opt.get();
                var size = template.getSize();
                var offset = new BlockPos(ox - size.getX() / 2, oy, oz - size.getZ() / 2);
                var settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE);
                template.placeInWorld(level, offset, offset, settings, level.getRandom(), 3);
            }
        } catch (Exception e) {
            modjam.LOGGER.warn("Could not load desert pyramid template, building simplified version", e);
        }
    }

    private static void buildFloor(ServerLevel level, int ox, int oz, int oy) {
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                var state = (Math.abs(x) == HALF || Math.abs(z) == HALF)
                    ? Blocks.SANDSTONE.defaultBlockState()
                    : (((x * 31 + z * 7) % 3 != 0)
                        ? Blocks.SANDSTONE.defaultBlockState()
                        : Blocks.CUT_SANDSTONE.defaultBlockState());
                level.setBlock(new BlockPos(ox + x, oy, oz + z), state, 3);
            }
        }
    }

    private static void buildWalls(ServerLevel level, int ox, int oz, int oy) {
        for (int y = 1; y <= 3; y++) {
            for (int x = -HALF; x <= HALF; x++) {
                for (int z = -HALF; z <= HALF; z++) {
                    boolean onEdge = Math.abs(x) == HALF || Math.abs(z) == HALF;
                    boolean isEntrance = z == HALF && Math.abs(x) <= 1;
                    if (onEdge && !isEntrance) {
                        level.setBlock(new BlockPos(ox + x, oy + y, oz + z), Blocks.SANDSTONE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void buildInterior(ServerLevel level, int ox, int oz, int oy) {
        for (int y = 0; y <= 3; y++) {
            for (int x = -HALF + 1; x <= HALF - 1; x++) {
                for (int z = -HALF + 1; z <= HALF - 1; z++) {
                    boolean isEntrance = z == HALF - 1 && Math.abs(x) <= 1;
                    if (!isEntrance) {
                        level.setBlock(new BlockPos(ox + x, oy + y, oz + z),
                            y == 0 ? Blocks.SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void buildPyramidTiers(ServerLevel level, int ox, int oz, int oy) {
        for (int tier = 0; tier < 4; tier++) {
            int tierStart = 4 + tier * 2;
            int size = 21 - 2 - tier * 2;
            int half = size / 2;
            for (int dy = 0; dy < 2; dy++) {
                int y = tierStart + dy;
                for (int x = -half; x <= half; x++) {
                    for (int z = -half; z <= half; z++) {
                        boolean onEdge = Math.abs(x) == half || Math.abs(z) == half;
                        level.setBlock(new BlockPos(ox + x, oy + y, oz + z),
                            onEdge ? Blocks.SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void buildTreasureRoom(ServerLevel level, int ox, int oz, int oy, net.minecraft.util.RandomSource random) {
        int pitHalf = 1;
        int roomHalf = 3;

        for (int y = -1; y >= -5; y--) {
            for (int x = -pitHalf; x <= pitHalf; x++) {
                for (int z = -pitHalf; z <= pitHalf; z++) {
                    boolean onEdge = Math.abs(x) == pitHalf || Math.abs(z) == pitHalf;
                    level.setBlock(new BlockPos(ox + x, oy + y, oz + z),
                        onEdge ? Blocks.SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        int treasureY = -5;
        for (int x = -roomHalf; x <= roomHalf; x++) {
            for (int z = -roomHalf; z <= roomHalf; z++) {
                boolean onEdge = Math.abs(x) == roomHalf || Math.abs(z) == roomHalf;
                if (onEdge) {
                    level.setBlock(new BlockPos(ox + x, oy + treasureY, oz + z), Blocks.SANDSTONE.defaultBlockState(), 3);
                }
            }
        }

        int floorY = treasureY + 1;
        for (int x = -roomHalf + 1; x <= roomHalf - 1; x++) {
            for (int z = -roomHalf + 1; z <= roomHalf - 1; z++) {
                level.setBlock(new BlockPos(ox + x, oy + floorY, oz + z), Blocks.CHISELED_SANDSTONE.defaultBlockState(), 3);
            }
        }

        var chestPositions = new BlockPos[]{
            new BlockPos(ox - 2, oy + floorY + 1, oz - 2),
            new BlockPos(ox + 2, oy + floorY + 1, oz - 2),
            new BlockPos(ox - 2, oy + floorY + 1, oz + 2),
            new BlockPos(ox + 2, oy + floorY + 1, oz + 2),
        };
        for (var chestPos : chestPositions) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        }

        level.setBlock(new BlockPos(ox, oy + floorY + 1, oz), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
    }
}
