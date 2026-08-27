package net.multyfora.modjam.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.network.WallWritingReadPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class WallWritingEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_PLAIN = SynchedEntityData.defineId(WallWritingEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(WallWritingEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<BlockPos> DATA_WALL_POS = SynchedEntityData.defineId(WallWritingEntity.class, EntityDataSerializers.BLOCK_POS);

    public WallWritingEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PLAIN, "");
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_WALL_POS, BlockPos.ZERO);
    }

    public void setWallData(BlockPos wallPos, Direction facing, String plain) {
        entityData.set(DATA_WALL_POS, wallPos.immutable());
        entityData.set(DATA_FACING, facing);
        entityData.set(DATA_PLAIN, plain == null ? "" : plain);
        Vec3 center = Vec3.atCenterOf(wallPos).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(0.55));
        setPos(center.x, center.y, center.z);
    }

    public String getPlain() {
        return entityData.get(DATA_PLAIN);
    }

    public Direction getFacing() {
        return entityData.get(DATA_FACING);
    }

    public BlockPos getWallPos() {
        return entityData.get(DATA_WALL_POS);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String plain = input.getStringOr("plain", "");
        int facingIdx = input.getIntOr("facing", Direction.NORTH.get3DDataValue());
        BlockPos wallPos = input.read("wallPos", BlockPos.CODEC).orElse(BlockPos.ZERO);
        Direction facing = Direction.from3DDataValue(facingIdx);
        if (facing.getAxis() == Direction.Axis.Y) facing = Direction.NORTH;
        entityData.set(DATA_PLAIN, plain);
        entityData.set(DATA_FACING, facing);
        entityData.set(DATA_WALL_POS, wallPos);
        Vec3 center = Vec3.atCenterOf(wallPos).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(0.55));
        setPos(center.x, center.y, center.z);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("plain", getPlain());
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
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new WallWritingReadPayload(getPlain()));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
