package net.multyfora.don.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public final class CrystalTrackerClient {
    private CrystalTrackerClient() {}

    public static void markDirty(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel)) return;
        Minecraft mc = Minecraft.getInstance();
        LevelExtractor extractor = mc.levelExtractor;
        if (extractor == null) return;
        extractor.setSectionDirtyWithNeighbors(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()),
            SectionPos.blockToSectionCoord(pos.getZ())
        );
    }
}
