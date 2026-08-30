package net.multyfora.don;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.effect.MobEffect;
import net.multyfora.don.effect.LightDrainEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.multyfora.don.block.AmethystCrystalBlockEntity;
import net.multyfora.don.block.CrackedQuartzBlock;
import net.multyfora.don.block.LightConduitBlock;
import net.multyfora.don.block.LightConduitBlockEntity;
import net.multyfora.don.block.MysticBrazierBlock;
import net.multyfora.don.block.MysticBrazierBlockEntity;
import net.multyfora.don.block.PortableStarBlock;
import net.multyfora.don.block.PortableStarBlockEntity;
import net.multyfora.don.block.SingularityCrystalBlock;
import net.multyfora.don.block.SingularityCrystalDrain;
import net.multyfora.don.block.SoulLightBlockEntity;
import net.multyfora.don.item.BrightestItem;
import net.multyfora.don.item.JournalItem;
import net.multyfora.don.item.LightBeamHandler;
import net.multyfora.don.item.LightWeaverItem;
import net.multyfora.don.item.MysticalMonocle;
import net.multyfora.don.item.SealedSingularityItem;
import net.multyfora.don.item.WallWritingItem;
import net.multyfora.don.world.entity.WallWritingEntity;
import net.multyfora.don.light.LightEnergyManager;
import net.multyfora.don.lightweaver.CheatSheetUI;
import net.multyfora.don.lightweaver.LightBeamDestructionManager;
import net.multyfora.don.lightweaver.LightWeaverShapes;
import net.multyfora.don.lightweaver.WeaverPaper;
import net.multyfora.don.world.entity.BrightestEntity;
import net.multyfora.don.world.entity.LightWeaverEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.multyfora.don.dialogue.DialogueEventManager;
import net.multyfora.don.cutscene.CutsceneManager;
import net.multyfora.don.journal.JournalEntryManager;
import net.multyfora.don.network.AcceptDealPayload;
import net.multyfora.don.network.CutsceneCompletePayload;
import net.multyfora.don.network.DialogueCompletePayload;
import net.multyfora.don.network.DialogueEventStartPayload;
import net.multyfora.don.network.FirstContactEnterPayload;
import net.multyfora.don.network.FirstContactLeavePayload;
import net.multyfora.don.network.FirstContactTogglePayload;
import net.multyfora.don.network.JournalOpenPayload;
import net.multyfora.don.network.JournalSyncPayload;
import net.multyfora.don.network.LightBeamPayload;
import net.multyfora.don.network.OpenBrightestMenuPayload;
import net.multyfora.don.network.RefuseDealPayload;
import net.multyfora.don.network.SavePaperPatternPayload;
import net.multyfora.don.network.SetStarMysticalPayload;
import net.multyfora.don.network.StartCutscenePayload;
import net.multyfora.don.network.WallWritingReadPayload;
import net.multyfora.don.world.dimension.FirstContactLeaveFlow;
import net.multyfora.don.world.dimension.FirstContactUtils;
import net.multyfora.don.world.dimension.ModDimensions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Set;

@Mod(don.MODID)
public class don {
    public static final String MODID = "don";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static volatile java.nio.file.Path pendingWorldDeletion = null;
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    public static final DeferredHolder<MobEffect, LightDrainEffect> LIGHT_DRAIN_EFFECT = MOB_EFFECTS.register("light_drain",
        LightDrainEffect::new);

    public static final ResourceKey<Enchantment> LIGHT_BEAM_ENCHANTMENT = ResourceKey.create(
        Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MODID, "light_beam"));
    public static final ResourceKey<Enchantment> GLOWMARK_ENCHANTMENT = ResourceKey.create(
        Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MODID, "glowmark"));
    public static final ResourceKey<Enchantment> LIGHT_DRAIN_ENCHANTMENT = ResourceKey.create(
        Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MODID, "light_drain"));

    public static final DeferredItem<Item> BRIGHTEST = ITEMS.registerItem("brightest", BrightestItem::new);

    public static final DeferredItem<Item> SEALED_SINGULARITY = ITEMS.registerItem("sealed_singularity",
        properties -> new SealedSingularityItem(properties.stacksTo(1)));

    public static final DeferredItem<Item> JOURNAL_ITEM = ITEMS.registerItem("discovery_journal", JournalItem::new);

    public static final DeferredItem<Item> MYSTICAL_MONOCLE = ITEMS.registerItem("mystical_monocle",
        properties -> new MysticalMonocle(properties
            .stacksTo(1)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                .setAsset(ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "mystical_monocle")))
                .build())));

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
            () -> new BlockEntityType<>(AmethystCrystalBlockEntity::new, Set.of(Blocks.AMETHYST_CLUSTER, Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD))
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulLightBlockEntity>> SOUL_LIGHT_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            "soul_light",
            () -> new BlockEntityType<>(SoulLightBlockEntity::new,
                Set.of(Blocks.SOUL_LANTERN, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.SOUL_CAMPFIRE, Blocks.LANTERN, Blocks.TORCH, Blocks.WALL_TORCH, Blocks.CAMPFIRE))
        );

    public static final DeferredBlock<SingularityCrystalBlock> SINGULARITY_CRYSTAL_BLOCK = BLOCKS.registerBlock(
        "singularity_crystal",
        SingularityCrystalBlock::new,
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.5f).sound(SoundType.AMETHYST).noOcclusion().requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> SINGULARITY_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("singularity_crystal", SINGULARITY_CRYSTAL_BLOCK);

    public static final DeferredBlock<Block> LIGHT_URN_BLOCK = BLOCKS.registerBlock(
        "light_urn",
        Block::new,
        p -> p.strength(1.5f).sound(SoundType.STONE).noOcclusion().lightLevel(state -> 1).requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> LIGHT_URN_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("light_urn", LIGHT_URN_BLOCK);

    public static final DeferredBlock<MysticBrazierBlock> MYSTIC_BRAZIER_BLOCK = BLOCKS.registerBlock(
        "mystic_brazier",
        MysticBrazierBlock::new,
        p -> p.mapColor(MapColor.COLOR_ORANGE).strength(2.0f).sound(SoundType.STONE).noOcclusion()
            .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false).isRedstoneConductor((state, level, pos) -> false)
            .lightLevel(state -> state.getValue(MysticBrazierBlock.LIT) ? 14 : 0)
    );
    public static final DeferredItem<BlockItem> MYSTIC_BRAZIER_ITEM = ITEMS.registerSimpleBlockItem("mystic_brazier", MYSTIC_BRAZIER_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MysticBrazierBlockEntity>> MYSTIC_BRAZIER_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            "mystic_brazier",
            () -> new BlockEntityType<>(MysticBrazierBlockEntity::new, Set.of(MYSTIC_BRAZIER_BLOCK.get()))
        );

    public static final DeferredBlock<PortableStarBlock> PORTABLE_STAR_BLOCK = BLOCKS.registerBlock(
        "portable_star",
        PortableStarBlock::new,
        p -> p.mapColor(MapColor.COLOR_YELLOW).strength(3.0f).sound(SoundType.AMETHYST).noOcclusion().requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> PORTABLE_STAR_ITEM = ITEMS.registerSimpleBlockItem("portable_star", PORTABLE_STAR_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortableStarBlockEntity>> PORTABLE_STAR_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            "portable_star",
            () -> new BlockEntityType<>(PortableStarBlockEntity::new, Set.of(PORTABLE_STAR_BLOCK.get()))
        );

    public static final DeferredBlock<LightConduitBlock> LIGHT_CONDUIT_BLOCK = BLOCKS.registerBlock(
        "light_conduit",
        LightConduitBlock::new,
        p -> p.mapColor(MapColor.COLOR_YELLOW).strength(2.0f).sound(SoundType.AMETHYST).noOcclusion()
            .lightLevel(state -> state.getValue(LightConduitBlock.POWERED) ? 10 : 0)
    );
    public static final DeferredItem<BlockItem> LIGHT_CONDUIT_ITEM = ITEMS.registerSimpleBlockItem("light_conduit", LIGHT_CONDUIT_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightConduitBlockEntity>> LIGHT_CONDUIT_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            "light_conduit",
            () -> new BlockEntityType<>(LightConduitBlockEntity::new, Set.of(LIGHT_CONDUIT_BLOCK.get()))
        );

    public static final DeferredBlock<CrackedQuartzBlock> CRACKED_QUARTZ_BLOCK = BLOCKS.registerBlock(
        "cracked_quartz",
        CrackedQuartzBlock::new,
        p -> p.strength(1.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> CRACKED_QUARTZ_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cracked_quartz", CRACKED_QUARTZ_BLOCK);

    public static final DeferredBlock<Block> MOSSY_QUARTZ_BLOCK = BLOCKS.registerSimpleBlock(
        "mossy_quartz",
        p -> p.mapColor(MapColor.COLOR_CYAN).strength(1.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> MOSSY_QUARTZ_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mossy_quartz", MOSSY_QUARTZ_BLOCK);

    public static final DeferredBlock<StairBlock> MOSSY_QUARTZ_STAIRS = BLOCKS.registerBlock(
        "mossy_quartz_stairs",
        p -> new StairBlock(MOSSY_QUARTZ_BLOCK.get().defaultBlockState(),
            p.mapColor(MapColor.COLOR_CYAN).strength(1.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    );
    public static final DeferredItem<BlockItem> MOSSY_QUARTZ_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("mossy_quartz_stairs", MOSSY_QUARTZ_STAIRS);

    public static final DeferredBlock<SlabBlock> MOSSY_QUARTZ_SLAB = BLOCKS.registerBlock(
        "mossy_quartz_slab",
        SlabBlock::new,
        p -> p.mapColor(MapColor.COLOR_CYAN).strength(1.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> MOSSY_QUARTZ_SLAB_ITEM = ITEMS.registerSimpleBlockItem("mossy_quartz_slab", MOSSY_QUARTZ_SLAB);

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

    public static final DeferredHolder<EntityType<?>, EntityType<WallWritingEntity>> WALL_WRITING_ENTITY =
        ENTITY_TYPES.register("wall_writing", () -> EntityType.Builder.of(WallWritingEntity::new, MobCategory.MISC)
            .sized(0.7f, 0.7f)
            .setUpdateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MODID, "wall_writing"))));

    public static final DeferredItem<Item> WALL_WRITING_ITEM = ITEMS.registerItem("wall_writing",
        properties -> new WallWritingItem(properties.stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.don"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> JOURNAL_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(BRIGHTEST.get());
                output.accept(SEALED_SINGULARITY.get());
                output.accept(SEALED_SINGULARITY.get());
                output.accept(JOURNAL_ITEM.get());
                output.accept(MYSTICAL_MONOCLE.get());
                output.accept(LIGHT_WEAVER_ITEM.get());
                output.accept(SINGULARITY_CRYSTAL_BLOCK_ITEM.get());
                output.accept(LIGHT_URN_BLOCK_ITEM.get());
                output.accept(CRACKED_QUARTZ_BLOCK_ITEM.get());
                output.accept(MOSSY_QUARTZ_BLOCK_ITEM.get());
                output.accept(MOSSY_QUARTZ_STAIRS_ITEM.get());
                output.accept(MOSSY_QUARTZ_SLAB_ITEM.get());
                output.accept(MYSTIC_BRAZIER_ITEM.get());
                output.accept(PORTABLE_STAR_ITEM.get());
                output.accept(LIGHT_CONDUIT_ITEM.get());
            }).build());

    public don(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        modEventBus.addListener(this::onRegisterAttributes);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        ModDimensions.BIOMES.register(modEventBus);

        PlayerUIMenuType.register(CHEAT_SHEET_ID, player -> p -> CheatSheetUI.create(p));

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(DialogueEventManager.getInstance());
        NeoForge.EVENT_BUS.addListener(DialogueEventManager::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(CutsceneManager.getInstance());
        NeoForge.EVENT_BUS.addListener(CutsceneManager::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(JournalEntryManager.getInstance());
        NeoForge.EVENT_BUS.addListener(JournalEntryManager::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(SingularityCrystalDrain.getInstance());
        NeoForge.EVENT_BUS.register(LightBeamDestructionManager.getInstance());


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

        registrar.playToClient(
            StartCutscenePayload.TYPE,
            StartCutscenePayload.STREAM_CODEC
        );

        registrar.playToClient(
            JournalSyncPayload.TYPE,
            JournalSyncPayload.STREAM_CODEC
        );

        registrar.playToClient(
            WallWritingReadPayload.TYPE,
            WallWritingReadPayload.STREAM_CODEC
        );

        registrar.playToServer(
            JournalOpenPayload.TYPE,
            JournalOpenPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    JournalEntryManager.syncToPlayer(player);
                }
            }
        );

        registrar.playToServer(
            CutsceneCompletePayload.TYPE,
            CutsceneCompletePayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player
                    && payload.id().equals(CutsceneManager.ACCEPTED_DEAL.toString())) {
                    FirstContactLeaveFlow.startLeaveSequence(player);
                }
            }
        );

        registrar.playToServer(
            AcceptDealPayload.TYPE,
            AcceptDealPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    player.getPersistentData().putBoolean(FirstContactUtils.ACCEPTED_DEAL_TAG.toString(), true);
                    if (!CutsceneManager.runEvent(player, CutsceneManager.ACCEPTED_DEAL)) {
                        FirstContactLeaveFlow.startLeaveSequence(player);
                    }
                }
            }
        );

        registrar.playToServer(
            RefuseDealPayload.TYPE,
            RefuseDealPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
                    var server = serverLevel.getServer();
                    player.connection.disconnect(net.minecraft.network.chat.Component.literal("you were deemed unworthy to save Nayir"));
                    if (server.isDedicatedServer()) return;

                    var worldPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .toAbsolutePath().normalize();
                    pendingWorldDeletion = worldPath;

                    server.execute(() -> {
                        for (var lvl : server.getAllLevels()) lvl.noSave = true;
                        server.halt(false);
                    });
                }
            }
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
            SetStarMysticalPayload.TYPE,
            SetStarMysticalPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level) {
                    BlockPos pos = new BlockPos(payload.x(), payload.y(), payload.z());
                    if (pos.distToCenterSqr(player.position()) > 36.0) return;
                    if (level.getBlockEntity(pos) instanceof PortableStarBlockEntity star) {
                        star.setMystical(payload.mystical());
                    }
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
        LightEnergyManager.registerSource(Blocks.BEACON, 100.0, 0.5);
        LightEnergyManager.registerSource(Blocks.SOUL_LANTERN, 1.0, 5.0);
        LightEnergyManager.registerSource(Blocks.SOUL_TORCH, 1.0, 5.0);
        LightEnergyManager.registerSource(Blocks.SOUL_CAMPFIRE, 1.0, 5.0);
        LightEnergyManager.registerSource(MYSTIC_BRAZIER_BLOCK.get(), 0.5, 2.0,
            state -> state.getValue(MysticBrazierBlock.LIT));
        LightEnergyManager.registerSource(PORTABLE_STAR_BLOCK.get(), 1000.0, 0.0);
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
            JournalEntryManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        var path = pendingWorldDeletion;
        if (path == null) return;
        pendingWorldDeletion = null;
        deleteWorldFolder(path);
    }

    private static void deleteWorldFolder(java.nio.file.Path root) {
        if (tryDeleteTree(root, 5)) {
            LOGGER.warn("World folder deleted after refusing Brightest's deal: {}", root);
            return;
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
            ProcessBuilder pb = os.contains("win")
                ? new ProcessBuilder("cmd", "/c", "ping 127.0.0.1 -n 4 > nul & rmdir /s /q \"" + root + "\"")
                : new ProcessBuilder("sh", "-c", "sleep 3; rm -rf '" + root + "'");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
            LOGGER.warn("World folder not fully gone yet, handed off to fallback process: {}", root);
        } catch (Exception e) {
            LOGGER.error("Fallback world-delete process failed to start for {}", root, e);
        }
    }

    private static boolean tryDeleteTree(java.nio.file.Path root, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (!java.nio.file.Files.exists(root)) return true;
            try (var walk = java.nio.file.Files.walk(root)) {
                walk.sorted(java.util.Comparator.comparingInt((java.nio.file.Path p) -> p.getNameCount()).reversed())
                    .forEach(p -> {
                        try {
                            p.toFile().setWritable(true);
                            java.nio.file.Files.deleteIfExists(p);
                        } catch (Exception ignored) {}
                    });
            } catch (Exception ignored) {}
            if (!java.nio.file.Files.exists(root)) return true;
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
        return !java.nio.file.Files.exists(root);
    }
}