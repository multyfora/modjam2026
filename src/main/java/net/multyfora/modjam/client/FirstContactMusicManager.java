package net.multyfora.modjam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.multyfora.modjam.modjam;
import net.multyfora.modjam.world.dimension.ModDimensions;

public class FirstContactMusicManager {
    private static final FirstContactMusicManager INSTANCE = new FirstContactMusicManager();

    private static final Music FIRST_CONTACT_MUSIC = new Music(modjam.FIRST_CONTACT_MUSIC, 0, 0, true);

    private boolean wasInFirstContact;

    public static FirstContactMusicManager getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            stopIfNeeded(mc);
            return;
        }

        boolean inFirstContact = mc.player.level().dimension() == ModDimensions.FIRST_CONTACT_LEVEL_KEY;
        MusicManager musicManager = mc.getMusicManager();
        if (inFirstContact) {
            if (!musicManager.isPlayingMusic(FIRST_CONTACT_MUSIC)) {
                musicManager.startPlaying(FIRST_CONTACT_MUSIC);
            }
        } else {
            stopIfNeeded(mc);
        }
        wasInFirstContact = inFirstContact;
    }

    private void stopIfNeeded(Minecraft mc) {
        if (wasInFirstContact) {
            mc.getMusicManager().stopPlaying();
            wasInFirstContact = false;
        }
    }
}