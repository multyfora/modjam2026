package net.multyfora.don.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.multyfora.don.lightweaver.LightBeamDestructionManager;
import net.multyfora.don.don;
import net.multyfora.don.network.LightBeamPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = don.MODID)
public final class LightBeamHandler {
    public static final double BEAM_RANGE = 64.0;
    public static final double BEAM_HALF_WIDTH = 1.5;
    public static final float BEAM_DAMAGE = 200.0F;
    private static final double STEP = 0.5;
    private static final double START_OFFSET = 0.4;
    private static final int BLOCK_CARVE_RADIUS = 1;
    private static final int MAX_CARVE_BLOCKS = 1500;
    private static final int EFFECT_DELAY_TICKS = 12;

    private record PendingEffects(ServerLevel level, Player player, ItemStack bow, InteractionHand hand,
                                  List<BlockPos> blocks, Vec3 start, Vec3 look, float power, int fireTick) {
    }

    private static final List<PendingEffects> PENDING = new ArrayList<>();

    private LightBeamHandler() {
    }

    public static boolean tryFireBeam(ServerLevel level, LivingEntity shooter, ItemStack bow,
                                      InteractionHand hand, float power) {
        if (!(shooter instanceof Player player) || !hasLightBeamEnchantment(level, bow)) {
            return false;
        }
        fire(level, player, bow, hand, power);
        return true;
    }

    public static boolean hasLightBeamEnchantment(Level level, ItemStack stack) {
        Holder<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .get(don.LIGHT_BEAM_ENCHANTMENT).orElse(null);
        return enchantment != null && stack.getEnchantments().getLevel(enchantment) >= 1;
    }

    private static void fire(ServerLevel level, Player player, ItemStack bow, InteractionHand hand, float power) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(look.scale(START_OFFSET));
        double range = BEAM_RANGE * power;
        List<BlockPos> blocks = collectBlocks(level, start, look, range);

        PENDING.add(new PendingEffects(
            level, player, bow, hand, blocks, start, look, power,
            level.getServer().getTickCount() + EFFECT_DELAY_TICKS
        ));

        PacketDistributor.sendToPlayersInDimension(level, new LightBeamPayload(
            start.x, start.y, start.z, look.x, look.y, look.z, (float) range
        ));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        PENDING.removeIf(pending -> {
            if (pending.fireTick() > tick) {
                return false;
            }
            applyEffects(pending);
            return true;
        });
    }

    private static void applyEffects(PendingEffects pending) {
        ServerLevel level = pending.level();
        Player player = pending.player();
        Vec3 start = pending.start();
        Vec3 look = pending.look();
        double range = BEAM_RANGE * pending.power();
        float damage = BEAM_DAMAGE * pending.power();

        AABB path = new AABB(start, start.add(look.scale(range))).inflate(BEAM_HALF_WIDTH);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, path, e -> e != player && e.isAlive())) {
            entity.hurtServer(level, level.damageSources().playerAttack(player), damage);
        }

        if (!pending.blocks().isEmpty()) {
            LightBeamDestructionManager.getInstance().queue(level, pending.blocks(), player);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0f, 1.0f);

        for (int i = 1; i <= 8; i++) {
            Vec3 p = start.add(look.scale(range * i / 8.0));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.2, 0.2, 0.2, 0.02);
        }

        pending.bow().hurtAndBreak(pending.bow().getMaxDamage(), player, pending.hand().asEquipmentSlot());
    }

    private static List<BlockPos> collectBlocks(ServerLevel level, Vec3 start, Vec3 look, double range) {
        List<BlockPos> blocks = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Vec3 up = Math.abs(look.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side1 = look.cross(up).normalize();
        Vec3 side2 = look.cross(side1).normalize();
        Vec3 cursor = start;
        for (int guard = 0; guard < 512; guard++) {
            cursor = cursor.add(look.scale(STEP));
            if (cursor.distanceToSqr(start) > BEAM_RANGE * BEAM_RANGE) {
                break;
            }
            for (int di = -BLOCK_CARVE_RADIUS; di <= BLOCK_CARVE_RADIUS; di++) {
                for (int dj = -BLOCK_CARVE_RADIUS; dj <= BLOCK_CARVE_RADIUS; dj++) {
                    BlockPos pos = BlockPos.containing(cursor.add(side1.scale(di)).add(side2.scale(dj)));
                    if (!seen.add(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.liquid()) {
                        continue;
                    }
                    if (state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.BARRIER) {
                        if (di == 0 && dj == 0) {
                            return blocks;
                        }
                        continue;
                    }
                    blocks.add(pos);
                    if (blocks.size() >= MAX_CARVE_BLOCKS) {
                        return blocks;
                    }
                }
            }
        }
        return blocks;
    }
}