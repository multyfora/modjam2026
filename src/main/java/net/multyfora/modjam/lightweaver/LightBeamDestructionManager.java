package net.multyfora.modjam.lightweaver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

public final class LightBeamDestructionManager {
    private static final LightBeamDestructionManager INSTANCE = new LightBeamDestructionManager();
    private static final int BATCH_PER_TICK = 10;

    private final List<Carving> carvings = new ArrayList<>();

    private static final class Carving {
        final ServerLevel level;
        final List<BlockPos> blocks;
        final Entity breaker;
        int cursor;

        Carving(ServerLevel level, List<BlockPos> blocks, Entity breaker) {
            this.level = level;
            this.blocks = blocks;
            this.breaker = breaker;
        }
    }

    private LightBeamDestructionManager() {
    }

    public static LightBeamDestructionManager getInstance() {
        return INSTANCE;
    }

    public void queue(ServerLevel level, List<BlockPos> blocks, Entity breaker) {
        if (!blocks.isEmpty()) {
            carvings.add(new Carving(level, List.copyOf(blocks), breaker));
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (carvings.isEmpty()) {
            return;
        }
        carvings.removeIf(this::tickCarving);
    }

    private boolean tickCarving(Carving carving) {
        ServerLevel level = carving.level;
        int remaining = BATCH_PER_TICK;
        while (remaining-- > 0 && carving.cursor < carving.blocks.size()) {
            BlockPos pos = carving.blocks.get(carving.cursor++);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.liquid()) {
                continue;
            }
            level.destroyBlock(pos, false, carving.breaker, 0);
            level.levelEvent(null, 2001, pos, Block.getId(state));
        }
        return carving.cursor >= carving.blocks.size();
    }
}