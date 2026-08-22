package net.multyfora.modjam;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.multyfora.modjam.block.AmethystCrystalBlockEntity;
import net.multyfora.modjam.block.SingularityCrystalBlock;
import net.multyfora.modjam.block.SingularityCrystalDrain;
import net.multyfora.modjam.item.BrightestItem;
import net.multyfora.modjam.item.JournalItem;
import net.multyfora.modjam.item.LightWeaverItem;
import net.multyfora.modjam.light.LightEnergyManager;
import net.multyfora.modjam.lightweaver.CheatSheetUI;
import net.multyfora.modjam.lightweaver.LightBeamDestructionManager;
import net.multyfora.modjam.lightweaver.LightWeaverShapes;
import net.multyfora.modjam.lightweaver.WeaverPaper;
import net.multyfora.modjam.world.entity.BrightestEntity;
import net.multyfora.modjam.world.entity.LightWeaverEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.multyfora.modjam.dialogue.DialogueEventManager;
import net.multyfora.modjam.network.DialogueCompletePayload;
import net.multyfora.modjam.network.DialogueEventStartPayload;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.network.FirstContactLeavePayload;
import net.multyfora.modjam.network.FirstContactTogglePayload;
import net.multyfora.modjam.network.LightBeamPayload;
import net.multyfora.modjam.network.OpenBrightestMenuPayload;
import net.multyfora.modjam.network.OpenCheatSheetPayload;
import net.multyfora.modjam.network.SavePaperPatternPayload;
import net.multyfora.modjam.world.dimension.FirstContactLeaveFlow;
import net.multyfora.modjam.world.dimension.FirstContactUtils;
import net.multyfora.modjam.world.dimension.ModDimensions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Set;

@Mod(modjam.MODID)
public class modjam {
    public static final String MODID = "modjam";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(Registries.ENCHANTMENT, MODID);

    public static final DeferredHolder<Enchantment, Enchantment> LIGHT_BEAM_ENCHANTMENT = ENCHANTMENTS.register("light_beam",
        () -> Enchantment.enchantment(Enchantment.definition(
            HolderSet.direct(), 1, 1, new Enchantment.Cost(1, 0), new Enchantment.Cost(10, 0), 8,
            EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.OFFHAND
        )).build(Identifier.fromNamespaceAndPath(MODID, "light_beam")));

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredItem<Item> BRIGHTEST = ITEMS.registerItem("brightest", BrightestItem::new);

    public static final DeferredItem<Item> JOURNAL_ITEM = ITEMS.registerItem("discovery_journal", JournalItem::new);

    public static final DeferredHolder<EntityType<?>, EntityType<LightWeaverEntity>> LIGHT_WEAVER_ENTITY =
        ENTITY_TYPES.register("light_weaver", () -> EntityType.Builder.of(LightWeaverEntity::new, MobCategory.MISC)
            .sized(0.625f, 0.625f)
            .setUpdateInterval(2)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MODID, "light_weaver"))));
    public static final DeferredItem<Item> LIGHT_WEAVER_ITEM = ITEMS.registerItem("light_weaver", LightWeaverItem::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmethystCrystalBlockEntity>> AMETHYST_CRYSTAL_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            "amethyst_crystal",
            () -> new BlockEntityType<>(AmethystCrystalBlockEntity::new, Set.of(Blocks.AMETHYST_CLUSTER))
        );

    public static final DeferredBlock<SingularityCrystalBlock> SINGULARITY_CRYSTAL_BLOCK = BLOCKS.registerBlock(
        "singularity_crystal",
        SingularityCrystalBlock::new,
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.5f).sound(SoundType.AMETHYST).noOcclusion()
    );
    public static final DeferredItem<BlockItem> SINGULARITY_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("singularity_crystal", SINGULARITY_CRYSTAL_BLOCK);

    public static final Identifier CHEAT_SHEET_ID = Identifier.fromNamespaceAndPath(MODID, "cheat_sheet");

    public static final DeferredHolder<SoundEvent, SoundEvent> FIRST_CONTACT_MUSIC = SOUND_EVENTS.register(
        "first_contact_music",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "first_contact_music"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<BrightestEntity>> BRIGHTEST_ENTITY =
        ENTITY_TYPES.register("brightest", () -> EntityType.Builder.of(BrightestEntity::new, MobCategory.MISC)
            .sized(0.6f, 0.8f)
            .setUpdateInterval(20)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MODID, "brightest"))));

    public static final DeferredBlock<Block> EXAMPLE_BLENDER_BLOCK = BLOCKS.registerSimpleBlock("example_blender_block", p -> p.mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLENDER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_blender_block", EXAMPLE_BLENDER_BLOCK);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.modjam"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                output.accept(EXAMPLE_BLENDER_BLOCK_ITEM.get());
                output.accept(BRIGHTEST.get());
                output.accept(JOURNAL_ITEM.get());
                output.accept(LIGHT_WEAVER_ITEM.get());
                output.accept(SINGULARITY_CRYSTAL_BLOCK_ITEM.get());
            }).build());

    public modjam(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        modEventBus.addListener(this::onRegisterAttributes);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENCHANTMENTS.register(modEventBus);
        ModDimensions.BIOMES.register(modEventBus);

        PlayerUIMenuType.register(CHEAT_SHEET_ID, player -> p -> CheatSheetUI.create(p));

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(DialogueEventManager.getInstance());
        NeoForge.EVENT_BUS.addListener(DialogueEventManager::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(SingularityCrystalDrain.getInstance());
        NeoForge.EVENT_BUS.register(LightBeamDestructionManager.getInstance());

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID);

        registrar.playToClient(
            FirstContactLeavePayload.TYPE,
            FirstContactLeavePayload.STREAM_CODEC
        );

        registrar.playToClient(
            FirstContactEnterPayload.TYPE,
            FirstContactEnterPayload.STREAM_CODEC
        );

        registrar.playToClient(
            OpenBrightestMenuPayload.TYPE,
            OpenBrightestMenuPayload.STREAM_CODEC
        );

        registrar.playToClient(
            DialogueEventStartPayload.TYPE,
            DialogueEventStartPayload.STREAM_CODEC
        );

        registrar.playToClient(
            LightBeamPayload.TYPE,
            LightBeamPayload.STREAM_CODEC
        );

        registrar.playToServer(
            DialogueCompletePayload.TYPE,
            DialogueCompletePayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    FirstContactLeaveFlow.startLeaveSequence(player);
                }
            }
        );

        registrar.playToServer(
            SavePaperPatternPayload.TYPE,
            SavePaperPatternPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    InteractionHand hand = payload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    ItemStack stack = player.getItemInHand(hand);
                    if (WeaverPaper.isPaper(stack) && LightWeaverShapes.isValidPacked(payload.packed())) {
                        WeaverPaper.writePattern(stack, payload.packed());
                    }
                }
            }
        );

        registrar.playToServer(
            OpenCheatSheetPayload.TYPE,
            OpenCheatSheetPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    PlayerUIMenuType.openUI(player, CHEAT_SHEET_ID);
                }
            }
        );
    }

    private void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put(LIGHT_WEAVER_ENTITY.get(), LightWeaverEntity.createAttributes().build());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        LightEnergyManager.registerSource(Blocks.AMETHYST_CLUSTER, 1.0, 3.5);

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
            event.accept(EXAMPLE_BLENDER_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!FirstContactUtils.hasEnteredFirstContact(player)) {
                FirstContactUtils.teleportToFirstContact(player);
            } else if (player.level().dimension() == ModDimensions.FIRST_CONTACT_LEVEL_KEY) {
                FirstContactUtils.ensureBrightest((ServerLevel) player.level(), player);
            }
        }
    }
}