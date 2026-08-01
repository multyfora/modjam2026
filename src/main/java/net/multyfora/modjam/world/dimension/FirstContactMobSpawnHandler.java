package net.multyfora.modjam.world.dimension;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.multyfora.modjam.modjam;

@EventBusSubscriber(modid = modjam.MODID)
public class FirstContactMobSpawnHandler {

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (isFirstContact(event.getLevel().getLevel())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (isFirstContact(event.getLevel().getLevel())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static boolean isFirstContact(Level level) {
        return level.dimension().equals(ModDimensions.FIRST_CONTACT_LEVEL_KEY);
    }
}
