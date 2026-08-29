package net.multyfora.modjam.client;

import net.multyfora.modjam.block.PortableStarBlockEntity;
import net.multyfora.modjam.lightweaver.WeaverPaper;
import net.multyfora.modjam.client.cutscene.CutsceneClientController;
import net.multyfora.modjam.modjam;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = modjam.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.CLIENT) return;
        if (WeaverPaper.isPaper(event.getItemStack())) {
            PaperPatternGui.open(event.getHand().ordinal(), event.getItemStack());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.CLIENT || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getEntity().isShiftKeyDown()) return;
        var pos = event.getPos();
        if (event.getLevel().getBlockState(pos).is(modjam.PORTABLE_STAR_BLOCK.get())) {
            if (event.getLevel().getBlockEntity(pos) instanceof PortableStarBlockEntity star) {
                PortableStarGui.open(pos, star.getMystical());
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ItemActivationManager.getInstance().tick();
        BrightestVisitationManager.getInstance().tick();
        DialogueSystem.getInstance().tick();
        DialogueEventClientHandler.getInstance().tick();
        FirstContactTransitionState.getInstance().tick();
        BrightestInteractionManager.getInstance().tick();
        FirstContactMusicManager.getInstance().tick();
        SingularityDarknessManager.getInstance().tick();
        CutsceneClientController.getInstance().tick();
        MonocleHud.getInstance().tick();
        FirstContactShader.setIntensity(FirstContactTransitionState.getInstance().getIntensity());
    }
}
