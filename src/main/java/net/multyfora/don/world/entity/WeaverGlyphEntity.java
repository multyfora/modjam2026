package net.multyfora.don.world.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.lightweaver.LightWeaverShapes;
import org.slf4j.Logger;

public class WeaverGlyphEntity extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<String> DATA_ENCHANT = SynchedEntityData.defineId(WeaverGlyphEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(WeaverGlyphEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<BlockPos> DATA_WALL_POS = SynchedEntityData.defineId(WeaverGlyphEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_RANDOM = SynchedEntityData.defineId(WeaverGlyphEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean randomPlaceholder = true;

    public WeaverGlyphEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public boolean isRandomPlaceholder() {
        return randomPlaceholder;
    }

    public void setRandomPlaceholder(boolean random) {
        this.randomPlaceholder = random;
        entityData.set(DATA_RANDOM, random);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ENCHANT, "");
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_WALL_POS, BlockPos.ZERO);
        builder.define(DATA_RANDOM, true);
    }

    public void setWallData(BlockPos wallPos, Direction facing) {
        entityData.set(DATA_WALL_POS, wallPos.immutable());
        entityData.set(DATA_FACING, facing);
        Vec3 center = Vec3.atCenterOf(wallPos).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(0.55));
        setPos(center.x, center.y, center.z);
    }

    public void setWallData(BlockPos wallPos, Direction facing, String enchantId) {
        setWallData(wallPos, facing);
        if (enchantId == null || enchantId.isEmpty()) {
            randomPlaceholder = true;
            entityData.set(DATA_RANDOM, true);
            entityData.set(DATA_ENCHANT, "");
        } else {
            randomPlaceholder = false;
            entityData.set(DATA_RANDOM, false);
            entityData.set(DATA_ENCHANT, enchantId);
        }
    }

    public String getEnchantId() {
        return entityData.get(DATA_ENCHANT);
    }

    public Direction getFacing() {
        return entityData.get(DATA_FACING);
    }

    public BlockPos getWallPos() {
        return entityData.get(DATA_WALL_POS);
    }

    public LightWeaverShapes.WeaverShape getShape() {
        String id = getEnchantId();
        if (id == null || id.isEmpty()) return null;
        for (var shape : LightWeaverShapes.SHAPES) {
            if (shape.id().equals(id) || shape.enchantment().identifier().toString().equals(id)) {
                return shape;
            }
        }
        if (id.equals(LightWeaverShapes.EMPTY_SHAPE.id()) || id.equals(LightWeaverShapes.EMPTY_SHAPE.enchantment().identifier().toString())) {
            return LightWeaverShapes.EMPTY_SHAPE;
        }
        return null;
    }

    private void tryGenerateRandom() {
        boolean isRandom = randomPlaceholder;
        try {
            isRandom = entityData.get(DATA_RANDOM);
        } catch (Exception ignored) {}
        if (!isRandom) return;
        if (!getEnchantId().isEmpty()) return;
        if (!(level() instanceof ServerLevel sl)) return;
        BlockPos wallPos = getWallPos();
        if (wallPos.equals(BlockPos.ZERO)) return;
        long seed = sl.getSeed();
        long posHash = wallPos.asLong();
        int facingHash = getFacing().get3DDataValue() * 0x9E3779B9;
        RandomSource random = RandomSource.create(seed ^ posHash ^ facingHash ^ 0x9E3779B97F4A7C15L);
        if (LightWeaverShapes.SHAPES.isEmpty()) return;
        var shape = LightWeaverShapes.SHAPES.get(random.nextInt(LightWeaverShapes.SHAPES.size()));
        entityData.set(DATA_ENCHANT, shape.enchantment().identifier().toString());
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        fixWallPosIfNeeded();
        tryGenerateRandom();
    }

    @Override
    public void tick() {
        super.tick();
        fixWallPosIfNeeded();
        tryGenerateRandom();
    }

    private void fixWallPosIfNeeded() {
        BlockPos wallPos = getWallPos();
        Direction facing = getFacing();
        if (wallPos.equals(BlockPos.ZERO) || facing == null || level() == null || level().isClientSide()) return;
        Vec3 faceVec = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        Vec3 expectedCenter = Vec3.atCenterOf(wallPos).add(faceVec.scale(0.55));
        if (expectedCenter.distanceTo(position()) > 0.6) {
            BlockPos expectedWall = BlockPos.containing(position().subtract(faceVec.scale(0.55)));
            BlockPos corrected = resolveWallAlongNormal(expectedWall, facing);
            entityData.set(DATA_WALL_POS, corrected);
            Vec3 newCenter = Vec3.atCenterOf(corrected).add(faceVec.scale(0.55));
            setPos(newCenter.x, newCenter.y, newCenter.z);
            try {
                randomPlaceholder = entityData.get(DATA_RANDOM);
            } catch (Exception ignored) {}
        }
    }

    private BlockPos resolveWallAlongNormal(BlockPos expectedWall, Direction facing) {
        if (!level().getBlockState(expectedWall).isAir()) return expectedWall;
        BlockPos towardEntity = expectedWall.relative(facing);
        if (!level().getBlockState(towardEntity).isAir()) return towardEntity;
        BlockPos awayFromEntity = expectedWall.relative(facing.getOpposite());
        if (!level().getBlockState(awayFromEntity).isAir()) return awayFromEntity;
        LOGGER.warn("WeaverGlyphEntity at {} expected a wall block at {} (facing {}) but found air on all three positions along the normal; keeping the derived position", position(), expectedWall, facing);
        return expectedWall;
    }

    @Override
    public float rotate(Rotation rotation) {
        Direction old = getFacing();
        Direction rotated = rotation.rotate(old);
        if (rotated != old) {
            entityData.set(DATA_FACING, rotated);
        }
        return super.rotate(rotation);
    }

    @Override
    public float mirror(Mirror mirror) {
        Direction old = getFacing();
        Direction mirrored = mirror.mirror(old);
        if (mirrored != old) {
            entityData.set(DATA_FACING, mirrored);
        }
        return super.mirror(mirror);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String enchant = input.getStringOr("enchant", "");
        int facingIdx = input.getIntOr("facing", Direction.NORTH.get3DDataValue());
        BlockPos wallPos = input.read("wallPos", BlockPos.CODEC).orElse(BlockPos.ZERO);
        Direction facing = Direction.from3DDataValue(facingIdx);
        if (facing.getAxis() == Direction.Axis.Y) facing = Direction.NORTH;
        boolean random = input.getBooleanOr("random", enchant == null || enchant.isEmpty());
        randomPlaceholder = random;
        entityData.set(DATA_ENCHANT, enchant == null ? "" : enchant);
        entityData.set(DATA_RANDOM, random);
        entityData.set(DATA_FACING, facing);
        entityData.set(DATA_WALL_POS, wallPos);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (randomPlaceholder || entityData.get(DATA_RANDOM)) {
            output.putString("enchant", "");
            output.putBoolean("random", true);
        } else {
            output.putString("enchant", getEnchantId());
            output.putBoolean("random", false);
        }
        output.putInt("facing", getFacing().get3DDataValue());
        output.store("wallPos", BlockPos.CODEC, getWallPos());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        Direction d = getFacing();
        double half = 0.35;
        double t = 0.03125;
        double hx = d.getAxis() == Direction.Axis.X ? t : half;
        double hy = half;
        double hz = d.getAxis() == Direction.Axis.Z ? t : half;
        return new AABB(pos.x - hx, pos.y - hy, pos.z - hz, pos.x + hx, pos.y + hy, pos.z + hz);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return InteractionResult.PASS;
    }
}