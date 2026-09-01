package net.multyfora.don.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.multyfora.don.don;

public class LabMusicManager {
    private static final LabMusicManager INSTANCE = new LabMusicManager();
    private static final Music LAB_MUSIC = new Music(don.FIRST_CONTACT_MUSIC, 0, 0, true);
    private static final TagKey<Structure> LAB_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(don.MODID, "lab"));
    private boolean wasInLab;

    public static LabMusicManager getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            stopIfNeeded(mc);
            return;
        }
        boolean inLab = isInLab();
        MusicManager musicManager = mc.getMusicManager();
        if (inLab) {
            if (!musicManager.isPlayingMusic(LAB_MUSIC)) {
                musicManager.startPlaying(LAB_MUSIC);
            }
        } else {
            stopIfNeeded(mc);
        }
        wasInLab = inLab;
    }

    public void handlePayload(boolean play) {
        Minecraft mc = Minecraft.getInstance();
        MusicManager musicManager = mc.getMusicManager();
        if (play) {
            if (!musicManager.isPlayingMusic(LAB_MUSIC)) {
                musicManager.startPlaying(LAB_MUSIC);
            }
            wasInLab = true;
        } else {
            stopIfNeeded(mc);
        }
    }

    private boolean isInLab() {
        if (BetrayedClientState.isBetrayed()) return false;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        try {
            var server = mc.getSingleplayerServer();
            if (server != null) {
                var overworld = server.overworld();
                if (overworld != null) {
                    if (overworld.getServer().getPlayerList().getPlayer(mc.player.getUUID()) != null
                        && don.hasBetrayed(overworld.getServer().getPlayerList().getPlayer(mc.player.getUUID()))) return false;
                    StructureStart start = overworld.structureManager().getStructureWithPieceAt(mc.player.blockPosition(), LAB_TAG);
                    return start != StructureStart.INVALID_START;
                }
            }
        } catch (Exception e) {
        }
        return wasInLab;
    }

    private void stopIfNeeded(Minecraft mc) {
        if (wasInLab) {
            mc.getMusicManager().stopPlaying();
            wasInLab = false;
        }
    }
}
