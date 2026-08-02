package net.multyfora.modjam;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.multyfora.modjam.item.BrightestItem;
import net.multyfora.modjam.item.JournalItem;
import net.multyfora.modjam.world.entity.BrightestEntity;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.multyfora.modjam.network.DialogueCompletePayload;
import net.multyfora.modjam.network.FirstContactEnterPayload;
import net.multyfora.modjam.network.FirstContactLeavePayload;
import net.multyfora.modjam.network.FirstContactTogglePayload;
import net.multyfora.modjam.network.OpenBrightestMenuPayload;
import net.multyfora.modjam.world.dimension.FirstContactLeaveFlow;
import net.multyfora.modjam.world.dimension.FirstContactUtils;
import net.multyfora.modjam.world.dimension.ModDimensions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(modjam.MODID)
public class modjam {
    public static final String MODID = "modjam";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredItem<Item> BRIGHTEST = ITEMS.registerItem("brightest", BrightestItem::new);

    public static final DeferredItem<Item> JOURNAL_ITEM = ITEMS.registerItem("discovery_journal", JournalItem::new);

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
            }).build());

    public modjam(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModDimensions.BIOMES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

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

        registrar.playToServer(
            DialogueCompletePayload.TYPE,
            DialogueCompletePayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    FirstContactLeaveFlow.startLeaveSequence(player);
                }
            }
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

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