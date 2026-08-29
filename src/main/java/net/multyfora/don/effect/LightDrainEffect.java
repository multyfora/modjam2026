package net.multyfora.don.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class LightDrainEffect extends MobEffect {

    public static final int DAMAGE_INTERVAL_TICKS = 60;

    public LightDrainEffect() {
        super(MobEffectCategory.HARMFUL, 0x0A0014);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        mob.hurtServer(level, level.damageSources().magic(), 1.0f);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % DAMAGE_INTERVAL_TICKS == 0;
    }
}
