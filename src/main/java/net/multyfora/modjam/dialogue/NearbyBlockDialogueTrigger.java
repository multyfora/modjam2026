package net.multyfora.modjam.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record NearbyBlockDialogueTrigger(String block, int radius) implements DialogueTrigger {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("modjam", "nearby_block");

    public static final MapCodec<NearbyBlockDialogueTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("block").forGetter(NearbyBlockDialogueTrigger::block),
        Codec.INT.optionalFieldOf("radius", 8).forGetter(NearbyBlockDialogueTrigger::radius)
    ).apply(instance, NearbyBlockDialogueTrigger::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public boolean matches(ServerPlayer player) {
        Block target = resolve();
        if (target == null) return false;
        BlockPos center = player.blockPosition();
        int r = radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    BlockState state = player.level().getBlockState(center.offset(dx, dy, dz));
                    if (state.is(target)) return true;
                }
            }
        }
        return false;
    }

    private Block resolve() {
        try {
            return BuiltInRegistries.BLOCK.getValue(Identifier.parse(block));
        } catch (Exception e) {
            return null;
        }
    }
}