package net.multyfora.modjam.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.world.feature.FirstContactStructureFeature;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

public class FirstContactUtils {
    private static final Identifier ENTERED_TAG = Identifier.fromNamespaceAndPath("modjam", "entered_first_contact");

    public static void teleportToFirstContact(ServerPlayer player) {
        var server = ((ServerLevel) player.level()).getServer();
        var targetLevel = server.getLevel(ModDimensions.FIRST_CONTACT_LEVEL_KEY);
        if (targetLevel == null) return;

        FirstContactStructureFeature.placeAtSpawn(targetLevel);

        var targetPos = new BlockPos(0, 2, 0);

        teleportPlayer(player, targetLevel, targetPos);
        setRespawn(player, ModDimensions.FIRST_CONTACT_LEVEL_KEY, targetPos);
        player.getPersistentData().putBoolean(ENTERED_TAG.toString(), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        PacketDistributor.sendToPlayer(player, new FirstContactEnterPayload());
    }

    public static void leaveDimension(ServerPlayer player) {
        var overworld = ((ServerLevel) player.level()).getServer().overworld();
        var spawnPos = overworld.getRespawnData().pos();

        teleportPlayer(player, overworld, spawnPos);
        setRespawn(player, Level.OVERWORLD, spawnPos);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static boolean hasEnteredFirstContact(ServerPlayer player) {
        return player.getPersistentData().contains(ENTERED_TAG.toString());
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel targetLevel, BlockPos pos) {
        player.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            Set.of(), player.getYRot(), player.getXRot(), false);
    }

    private static void setRespawn(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        var respawnData = LevelData.RespawnData.of(dimension, pos, 0.0f, 0.0f);
        var config = new ServerPlayer.RespawnConfig(respawnData, false);
        player.setRespawnPosition(config, false);
    }
}
