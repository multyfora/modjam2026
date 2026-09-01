package net.multyfora.don.lightweaver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.multyfora.don.block.MysticBrazierBlock;
import net.multyfora.don.world.entity.LightWeaverEntity;
import net.multyfora.don.don;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LightWeaverSpawner {
    private static final Identifier TEMPLATE_ID = Identifier.fromNamespaceAndPath(don.MODID, "hollow_cathedral");
    private static final long SPAWN_COOLDOWN_MS = 1000L;
    private static final int MAX_BRAZIERS = 8;
    private static final long[] TARGET_COMBINATION = {1, 0, 1, 1, 0, 0, 1, 1};

    private static final Set<Long> spawnedOrigins = new HashSet<>();
    private static long lastSpawnMs = 0;
    private static boolean startupScanDone = false;

    private LightWeaverSpawner() {}

    public static void onBrazierLitChanged(Level level, BlockPos changedPos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = System.currentTimeMillis();
        if (now - lastSpawnMs < SPAWN_COOLDOWN_MS) return;

        StructureTemplate template = loadTemplate(serverLevel);
        if (template == null) return;

        List<BlockPos> relBraziers = getRelativeBraziers(template);
        if (relBraziers.isEmpty()) return;

        List<BlockPos> worldBraziers = scanBraziers(serverLevel, changedPos);
        if (worldBraziers.isEmpty()) return;

        for (BlockPos worldPos : worldBraziers) {
            for (BlockPos rel : relBraziers) {
                for (Rotation rotation : Rotation.values()) {
                    StructurePlaceSettings settings = new StructurePlaceSettings()
                            .setRotation(rotation);
                    BlockPos transformedRel = StructureTemplate.calculateRelativePosition(settings, rel);
                    long originX = worldPos.getX() - transformedRel.getX();
                    long originY = worldPos.getY() - transformedRel.getY();
                    long originZ = worldPos.getZ() - transformedRel.getZ();
                    long originHash = BlockPos.asLong((int) originX, (int) originY, (int) originZ);
                    if (spawnedOrigins.contains(originHash)) continue;

                    List<BlockPos> matched = matchBraziers(serverLevel, originX, originY, originZ, relBraziers, settings);
                    if (matched == null) continue;

                    matched.sort(Comparator.<BlockPos>comparingInt(Vec3i::getX)
                            .thenComparingInt(Vec3i::getZ)
                            .thenComparingInt(Vec3i::getY));
                    if (matched.size() > MAX_BRAZIERS) {
                        matched = matched.subList(0, MAX_BRAZIERS);
                    }

                    long[] current = new long[matched.size()];
                    for (int i = 0; i < matched.size(); i++) {
                        current[i] = serverLevel.getBlockState(matched.get(i))
                                .getValue(MysticBrazierBlock.LIT) ? 1 : 0;
                    }
                    if (!matches(current)) continue;

                    if (hasLightWeaverNear(serverLevel, matched)) continue;

                    spawnLightWeaver(serverLevel, matched);
                    spawnedOrigins.add(originHash);
                    lastSpawnMs = now;
                    return;
                }
            }
        }
    }

    public static void onServerTick(ServerLevel level) {
        if (!startupScanDone) {
            startupScanDone = true;
            scanExistingLightWeavers(level);
        }
    }

    private static void scanExistingLightWeavers(ServerLevel level) {
        StructureTemplate template = loadTemplate(level);
        if (template == null) return;
        List<BlockPos> relBraziers = getRelativeBraziers(template);
        if (relBraziers.isEmpty()) return;

        var weavers = level.getEntities(EntityTypeTest.forClass(LightWeaverEntity.class), e -> true);
        for (LightWeaverEntity weaver : weavers) {
            BlockPos weaverPos = weaver.blockPosition();
            List<BlockPos> worldBraziers = scanBraziers(level, weaverPos);
            if (worldBraziers.isEmpty()) continue;

            for (BlockPos worldPos : worldBraziers) {
                for (BlockPos rel : relBraziers) {
                    for (Rotation rotation : Rotation.values()) {
                        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
                        BlockPos transformedRel = StructureTemplate.calculateRelativePosition(settings, rel);
                        long originX = worldPos.getX() - transformedRel.getX();
                        long originY = worldPos.getY() - transformedRel.getY();
                        long originZ = worldPos.getZ() - transformedRel.getZ();
                        long originHash = BlockPos.asLong((int) originX, (int) originY, (int) originZ);
                        if (spawnedOrigins.contains(originHash)) continue;
                        List<BlockPos> matched = matchBraziers(level, originX, originY, originZ, relBraziers, settings);
                        if (matched == null) continue;
                        double distSq = weaver.distanceToSqr(
                                matched.get(0).getX() + 0.5,
                                matched.get(0).getY() + 0.5,
                                matched.get(0).getZ() + 0.5);
                        if (distSq <= 1024.0) {
                            spawnedOrigins.add(originHash);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static final int SCAN_RADIUS = 32;

    private static List<BlockPos> scanBraziers(ServerLevel level, BlockPos center) {
        List<BlockPos> worldBraziers = new ArrayList<>();
        int minChunkX = (center.getX() - SCAN_RADIUS) >> 4;
        int maxChunkX = (center.getX() + SCAN_RADIUS) >> 4;
        int minChunkZ = (center.getZ() - SCAN_RADIUS) >> 4;
        int maxChunkZ = (center.getZ() + SCAN_RADIUS) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                int baseX = cx << 4;
                int baseZ = cz << 4;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int wx = baseX + x;
                        int wz = baseZ + z;
                        if (Math.abs(wx - center.getX()) > SCAN_RADIUS) continue;
                        if (Math.abs(wz - center.getZ()) > SCAN_RADIUS) continue;
                        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                            BlockPos bp = new BlockPos(wx, y, wz);
                            if (level.getBlockState(bp).is(don.MYSTIC_BRAZIER_BLOCK.get())) {
                                worldBraziers.add(bp);
                            }
                        }
                    }
                }
            }
        }
        return worldBraziers;
    }

    private static StructureTemplate loadTemplate(ServerLevel level) {
        StructureTemplateManager manager = level.getStructureManager();
        return manager.get(TEMPLATE_ID).orElse(null);
    }

    private static List<BlockPos> getRelativeBraziers(StructureTemplate template) {
        List<StructureTemplate.StructureBlockInfo> infos = template.filterBlocks(
                BlockPos.ZERO, new StructurePlaceSettings(), don.MYSTIC_BRAZIER_BLOCK.get());
        List<BlockPos> result = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : infos) {
            result.add(info.pos());
        }
        return result;
    }

    private static List<BlockPos> matchBraziers(ServerLevel level,
            long originX, long originY, long originZ,
            List<BlockPos> relBraziers, StructurePlaceSettings settings) {
        List<BlockPos> matched = new ArrayList<>();
        for (BlockPos rel : relBraziers) {
            BlockPos transformed = StructureTemplate.calculateRelativePosition(settings, rel);
            int px = (int) (originX + transformed.getX());
            int py = (int) (originY + transformed.getY());
            int pz = (int) (originZ + transformed.getZ());
            BlockPos wp = new BlockPos(px, py, pz);
            if (!level.getBlockState(wp).is(don.MYSTIC_BRAZIER_BLOCK.get())) {
                return null;
            }
            matched.add(wp);
        }
        return matched;
    }

    private static boolean matches(long[] current) {
        if (current.length != TARGET_COMBINATION.length) return false;
        for (int i = 0; i < TARGET_COMBINATION.length; i++) {
            if (current[i] != TARGET_COMBINATION[i]) return false;
        }
        return true;
    }

    private static boolean hasLightWeaverNear(ServerLevel level, List<BlockPos> braziers) {
        for (BlockPos pos : braziers) {
            var found = level.getEntities(
                    EntityTypeTest.forClass(LightWeaverEntity.class),
                    e -> e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 1024.0);
            if (!found.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void spawnLightWeaver(ServerLevel level, List<BlockPos> matched) {
        List<net.minecraft.server.level.ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        net.minecraft.server.level.ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        double cx = 0, cy = 0, cz = 0;
        for (BlockPos p : matched) {
            cx += p.getX(); cy += p.getY(); cz += p.getZ();
        }
        cx /= matched.size(); cy /= matched.size(); cz /= matched.size();

        for (net.minecraft.server.level.ServerPlayer p : players) {
            double d = p.distanceToSqr(cx, cy, cz);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = p;
            }
        }
        if (nearest == null) return;

        LightWeaverEntity weaver = don.LIGHT_WEAVER_ENTITY.get().create(level,
                net.minecraft.world.entity.EntitySpawnReason.NATURAL);
        if (weaver == null) return;

        double ox = (level.getRandom().nextDouble() - 0.5) * 4.0;
        double oz = (level.getRandom().nextDouble() - 0.5) * 4.0;
        double spawnX = nearest.getX() + ox;
        double spawnY = nearest.getY();
        double spawnZ = nearest.getZ() + oz;

        weaver.setPos(spawnX, spawnY, spawnZ);
        weaver.setYRot(level.getRandom().nextFloat() * 360.0f);

        weaver.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));

        level.addFreshEntity(weaver);

        level.playSound(null, spawnX, spawnY, spawnZ,
                SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 1.0f, 1.0f);
    }
}
