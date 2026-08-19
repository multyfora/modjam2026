package net.multyfora.modjam.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.multyfora.modjam.item.LightBeamHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Redirect(
        method = "releaseUsing",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;shoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/world/entity/LivingEntity;)V")
    )
    private void modjam$redirectBeamShot(BowItem instance, ServerLevel level, LivingEntity shooter,
                                         InteractionHand hand, ItemStack weapon, List<ItemStack> projectiles,
                                         float velocity, float inaccuracy, boolean isCrit, LivingEntity target) {
        if (!LightBeamHandler.tryFireBeam(level, shooter, weapon, hand, velocity / 3.0f)) {
            ((ProjectileWeaponItemAccessor) instance).modjam$callShoot(
                level, shooter, hand, weapon, projectiles, velocity, inaccuracy, isCrit, target);
        }
    }
}