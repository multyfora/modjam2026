package net.multyfora.don.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ProjectileWeaponItem.class)
public interface ProjectileWeaponItemAccessor {
    @Invoker("shoot")
    void don$callShoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                          List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
                          LivingEntity target);
}