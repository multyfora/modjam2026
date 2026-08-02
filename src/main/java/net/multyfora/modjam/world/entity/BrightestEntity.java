package net.multyfora.modjam.world.entity;

import net.minecraft.core.BlockPos;
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
import net.multyfora.modjam.network.OpenBrightestMenuPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class BrightestEntity extends Entity {

    public static final float MODEL_HEIGHT = 1.2f;
    public static final float FLOAT_HEIGHT = 0.6f;

    private static final int LIGHT_RADIUS = 2;
    private static final int LIGHT_REFRESH_INTERVAL = 20;
    private static final int MAX_LIGHT = 15;

    private final List<BlockPos> litPositions = new ArrayList<>();
    private BlockPos lastLightPos;
    private int lightTimer;

    public BrightestEntity(EntityType<? extends BrightestEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && --lightTimer <= 0) {
            lightTimer = LIGHT_REFRESH_INTERVAL;
            refreshLights();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearLights();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        float halfHeight = getDimensions(Pose.STANDING).height() / 2.0f;
        return super.makeBoundingBox(pos).move(0.0, FLOAT_HEIGHT + MODEL_HEIGHT / 2.0 - halfHeight, 0.0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenBrightestMenuPayload());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return true;
    }

    private void refreshLights() {
        BlockPos pos = blockPosition();
        if (pos.equals(lastLightPos) && !litPositions.isEmpty()) return;
        clearLights();
        lastLightPos = pos.immutable();
        int radius = LIGHT_RADIUS;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        BlockPos lightPos = pos.offset(x, y, z);
                        level().getAuxLightManager(lightPos).setLightAt(lightPos, MAX_LIGHT);
                        litPositions.add(lightPos);
                    }
                }
            }
        }
    }

    private void clearLights() {
        for (var pos : litPositions) {
            level().getAuxLightManager(pos).setLightAt(pos, 0);
        }
        litPositions.clear();
        lastLightPos = null;
    }
}
