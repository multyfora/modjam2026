package net.multyfora.modjam.world.entity;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.stateless.StatelessGeoEntity;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.slot.ItemResourceHandlerSlot;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.light.LightEnergy;
import net.multyfora.modjam.light.LightEnergyManager;
import net.multyfora.modjam.lightweaver.LightWeaverMenuType;
import net.multyfora.modjam.lightweaver.LightWeaverShapes;
import net.multyfora.modjam.lightweaver.LightWeaverShapes.WeaverShape;
import net.multyfora.modjam.network.LightWeaverInfusePayload;
import net.multyfora.modjam.network.OpenCheatSheetPayload;
import net.multyfora.modjam.modjam;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class LightWeaverEntity extends Entity implements StatelessGeoEntity {
    private static final int OUTER_GOLD = 0xFF6B4A20;
    private static final int GOLD_BORDER = 0xFFD4A840;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int INNER_STONE = 0xFF3A2410;
    private static final int DIALOG_BG = 0xE2170803;
    private static final int DIAMOND = 0xFFFFD700;
    private static final int CELL_EMPTY = 0xFF2A1A0E;
    private static final int CELL_FILLED = 0xFFD4A840;
    private static final int PROGRESS_TRACK = 0xFF241408;
    private static final int PROGRESS_FILL = 0xFF7FE7FF;

    private static final int FADE_TICKS = 8;
    private static final int TICKS_PER_TIER = 20;

    private static final int BUTTON_BG = 0xCC3A2410;
    private static final int BUTTON_HOVER = 0xCC8B6914;
    private static final int BUTTON_PRESSED = 0xCC5C3A00;
    private static final int BUTTON_TEXT = 0xFFFFF3D6;

    private static final double LIGHT_MIN = 3.4;
    private static final double LIGHT_MAX = 3.6;

    private static final EntityDataAccessor<String> DATA_STATUS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_PROCESSING = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PROGRESS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_PROGRESS = SynchedEntityData.defineId(LightWeaverEntity.class, EntityDataSerializers.INT);

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(1);

    private Holder<Enchantment> pendingEnchantment;
    private int pendingLevel;
    private String pendingShapeName;

    private final AnimatableInstanceCache animatableCache = new InstancedAnimatableInstanceCache(this);

    public LightWeaverEntity(EntityType<? extends LightWeaverEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATUS, "");
        builder.define(DATA_PROCESSING, false);
        builder.define(DATA_PROGRESS, 0);
        builder.define(DATA_MAX_PROGRESS, 1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("armor", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> {
            if (!stack.isEmpty()) {
                try (Transaction transaction = Transaction.openRoot()) {
                    itemHandler.insert(0, ItemResource.of(stack), stack.getCount(), transaction);
                    transaction.commit();
                }
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ItemResource resource = itemHandler.getResource(0);
        if (!resource.isEmpty()) {
            output.store("armor", ItemStack.OPTIONAL_CODEC, resource.toStack(itemHandler.getAmountAsInt(0)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (isProcessing() && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.END_ROD,
                        position().x + (random.nextDouble() - 0.5) * 0.6,
                        position().y + 1.05 + random.nextDouble() * 0.3,
                        position().z + (random.nextDouble() - 0.5) * 0.6,
                        0.0, 0.02, 0.0);
            }
            return;
        }

        if (!isProcessing()) return;

        setProgress(getProgress() + 1);

        if (level() instanceof ServerLevel serverLevel && getProgress() % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    position().x, position().y + 1.05, position().z,
                    6, 0.3, 0.25, 0.3, 0.03);
        }

        if (getProgress() >= getMaxProgress()) {
            finishInfuse();
        }
    }

    public ModularUI createUI(Player player) {
        boolean[] cells = new boolean[LightWeaverShapes.GRID_SIZE * LightWeaverShapes.GRID_SIZE];
        int[] heldButton = {-1};

        Label hintLabel = new Label();
        hintLabel.setText("Draw a shape");
        hintLabel.textStyle(s -> s.textColor(0xFF8A7A60).adaptiveWidth(true));

        UIElement grid = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(1).paddingAll(6))
                .style(s -> s.background(SDFRectTexture.of(INNER_STONE).setRadius(6f).setBorderColor(OUTER_GOLD)));
        for (int row = 0; row < LightWeaverShapes.GRID_SIZE; row++) {
            UIElement rowElement = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(1));
            for (int col = 0; col < LightWeaverShapes.GRID_SIZE; col++) {
                final int r = row;
                final int c = col;
                UIElement cell = new UIElement()
                        .layout(l -> l.width(24).height(24))
                        .style(s -> s.background(cellTexture(cells, r, c)));
                cell.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                    if (heldButton[0] >= 0) {
                        setCell(cell, cells, r, c, heldButton[0] == 0);
                        updateHint(hintLabel, cells);
                    }
                });
                cell.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    heldButton[0] = e.button;
                    setCell(cell, cells, r, c, e.button == 0);
                    updateHint(hintLabel, cells);
                    e.stopPropagation();
                });
                rowElement.addChild(cell);
            }
            grid.addChild(rowElement);
        }

        UIElement gridColumn = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(6).alignItems(AlignItems.CENTER));
        gridColumn.addChild(grid);
        gridColumn.addChild(hintLabel);

        UIElement rightColumn = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(6).width(130).alignItems(AlignItems.CENTER));

        rightColumn.addChild(new Label().setText("Armor").textStyle(s -> s.textColor(0xFFB0A088).adaptiveWidth(true)));
        rightColumn.addChild(new ItemSlot().bind(
                new ItemResourceHandlerSlot(itemHandler, 0).setCanPlace(LightWeaverEntity::canHoldEnchantments))
                .layout(l -> l.width(24).height(24)));

        Button infuseButton = brightButton("Infuse", e -> sendToServer(
                new LightWeaverInfusePayload(getId(), LightWeaverShapes.pack(cells))));
        Button clearButton = brightButton("Clear", e -> {
            java.util.Arrays.fill(cells, false);
            refreshGrid(grid, cells);
            updateHint(hintLabel, cells);
        });
        Button cheatButton = brightButton("Cheat Sheet", e -> sendToServer(new OpenCheatSheetPayload()));

        rightColumn.addChild(infuseButton);
        rightColumn.addChild(clearButton);
        rightColumn.addChild(cheatButton);

        Label statusLabel = new Label();
        statusLabel.bindDataSource(SupplierDataSource.of(() -> Component.literal(getStatus())));
        statusLabel.layout(l -> l.width(130));
        statusLabel.textStyle(s -> s.textColor(GOLD_BORDER).textWrap(TextWrap.WRAP)
                .adaptiveHeight(true).textAlignHorizontal(Horizontal.CENTER));
        rightColumn.addChild(statusLabel);

        Label lightLabel = new Label();
        lightLabel.textStyle(s -> s.textColor(0xFF8A7A60).adaptiveWidth(true));
        rightColumn.addChild(lightLabel);

        UIElement progressFill = new UIElement()
                .layout(l -> l.heightPercent(100).widthPercent(0))
                .style(s -> s.background(SDFRectTexture.of(PROGRESS_FILL).setRadius(3f)));
        UIElement progressTrack = new UIElement()
                .layout(l -> l.widthPercent(100).height(8))
                .style(s -> s.background(SDFRectTexture.of(PROGRESS_TRACK).setRadius(3f).setBorderColor(DARK_GOLD)))
                .addChild(progressFill);
        progressTrack.addEventListener(UIEvents.TICK, e -> {
            progressFill.layout(l -> l.widthPercent(getProgressFraction() * 100f));
            infuseButton.setText(isProcessing() ? "Infusing..." : "Infuse");
        });
        rightColumn.addChild(progressTrack);

        UIElement header = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(6).alignItems(AlignItems.CENTER));
        header.addChild(new Label().setText("Light Weaver").textStyle(s -> s.textColor(GOLD_BORDER).adaptiveWidth(true)));

        UIElement mainRow = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(12).alignItems(AlignItems.START));
        mainRow.addChild(gridColumn);
        mainRow.addChild(rightColumn);

        UIElement panel = new UIElement()
                .layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(10).paddingAll(14).alignItems(AlignItems.CENTER))
                .style(s -> s.background(SDFRectTexture.of(DIALOG_BG).setRadius(10f).setBorderColor(0x55D4A840)))
                .addChild(header)
                .addChild(ornament())
                .addChild(mainRow)
                .addChild(new InventorySlots());

        UIElement inner = new UIElement()
                .layout(l -> l.widthPercent(100).paddingAll(2))
                .style(s -> s.background(SDFRectTexture.of(INNER_STONE).setRadius(12f).setBorderColor(DARK_GOLD)))
                .addChild(panel);

        UIElement halo = new UIElement()
                .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(-16).top(-16).right(-16).bottom(-16))
                .style(s -> s.background(SDFRectTexture.of(0x38FFD700).setRadius(30f)));

        UIElement dialogBox = new UIElement()
                .layout(l -> l.widthPercent(70).flexDirection(FlexDirection.COLUMN))
                .addChildren(halo, inner);

        UIElement root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN)
                        .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .addChild(dialogBox);

        int[] fadeTicks = {0};
        float[] time = {0f};
        root.addEventListener(UIEvents.MOUSE_UP, e -> heldButton[0] = -1);
        root.addEventListener(UIEvents.TICK, e -> {
            time[0] += 0.04f;
            if (fadeTicks[0] < FADE_TICKS) fadeTicks[0]++;
            float t = Mth.clamp((float) fadeTicks[0] / FADE_TICKS, 0f, 1f);
            float fade = 1f - (1f - t) * (1f - t) * (1f - t);
            dialogBox.style(s -> s.opacity(fade));
            float pulse = isProcessing() ? 0.85f : 0.55f + 0.3f * (float) Math.sin(time[0] * 1.2f);
            halo.style(s -> s.opacity(fade * pulse));

            LightEnergy energy = LightEnergyManager.compute(level(), blockPosition());
            lightLabel.setText(energy.isPresent()
                    ? "Aura: " + formatLight(energy.mysticalComponent()) + " \u00B7 Intensity: " + formatLight(energy.intensity())
                    : "Aura: \u2014 \u00B7 Intensity: 0");
        });

        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), player);
    }


    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player instanceof ServerPlayer serverPlayer) {
            LightWeaverMenuType.openUI(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return true;
    }


    public void tryInfuse(ServerPlayer player, String packed) {
        if (isProcessing()) return;
        boolean[] cells = LightWeaverShapes.unpack(packed);

        if (LightWeaverShapes.isEmpty(cells)) {
            setStatus("Draw a shape to infuse");
            playFail();
            return;
        }

        WeaverShape shape = LightWeaverShapes.match(cells);
        if (shape == null) {
            setStatus("The light does not recognize this shape");
            playFail();
            return;
        }

        LightEnergy energy = LightEnergyManager.compute(level(), blockPosition());
        if (!energy.isPresent()) {
            setStatus("No light sources nearby");
            playFail();
            return;
        }
        if (energy.mysticalComponent() < LIGHT_MIN || energy.mysticalComponent() > LIGHT_MAX) {
            setStatus("The light's essence (" + formatLight(energy.mysticalComponent())
                    + ") is beyond this machine's range (" + formatLight(LIGHT_MIN) + "-" + formatLight(LIGHT_MAX) + ")");
            playFail();
            return;
        }

        ItemResource armorResource = itemHandler.getResource(0);
        if (armorResource.isEmpty()) {
            setStatus("Place armor to infuse");
            playFail();
            return;
        }

        ItemStack armorStack = armorResource.toStack(itemHandler.getAmountAsInt(0));
        if (!canHoldEnchantments(armorStack)) {
            setStatus("This item cannot hold the light");
            playFail();
            return;
        }

        Holder<Enchantment> enchantment = resolveEnchantment(level(), shape.enchantment());
        if (enchantment == null || !enchantment.value().isSupportedItem(armorStack)) {
            setStatus("The light cannot bind " + shape.displayName() + " here");
            playFail();
            return;
        }

        int currentLevel = armorStack.getEnchantments().getLevel(enchantment);
        if (currentLevel >= 1) {
            setStatus("This item already holds " + shape.displayName());
            playFail();
            return;
        }

        int maxLevel = enchantment.value().getMaxLevel();
        int appliedLevel = Mth.clamp((int) Math.round(energy.intensity()), 1, maxLevel);
        int xpCost = shape.tier() * appliedLevel;

        if (!player.getAbilities().instabuild) {
            if (player.experienceLevel < xpCost) {
                setStatus("Need " + xpCost + " experience levels");
                playFail();
                return;
            }
            player.giveExperienceLevels(-xpCost);
        }

        pendingEnchantment = enchantment;
        pendingLevel = appliedLevel;
        pendingShapeName = shape.displayName();
        setProgress(0);
        setMaxProgress(Math.max(1, shape.tier() * TICKS_PER_TIER));
        setProcessing(true);
        setStatus("Infusing " + shape.displayName() + "...");
        playSound(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.4f);
    }

    private void finishInfuse() {
        setProcessing(false);

        if (pendingEnchantment == null) {
            setStatus("Something disrupted the infusion");
            pendingEnchantment = null;
            return;
        }

        ItemResource armorResource = itemHandler.getResource(0);
        if (armorResource.isEmpty()) {
            setStatus("The item vanished mid-infusion!");
            pendingEnchantment = null;
            return;
        }

        ItemStack stack = armorResource.toStack(itemHandler.getAmountAsInt(0));
        ItemStack enchanted = stack.copy();
        enchanted.enchant(pendingEnchantment, pendingLevel);

        try (Transaction transaction = Transaction.openRoot()) {
            itemHandler.extract(0, armorResource, itemHandler.getAmountAsInt(0), transaction);
            itemHandler.insert(0, ItemResource.of(enchanted), 1, transaction);
            transaction.commit();
        }

        LightEnergyManager.drainAll(level(), blockPosition());

        playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.1f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    position().x, position().y + 1.2, position().z,
                    24, 0.4, 0.4, 0.4, 0.06);
        }

        setStatus("Infused " + pendingShapeName + " " + toRoman(pendingLevel));
        pendingEnchantment = null;
    }


    public String getStatus() {
        return entityData.get(DATA_STATUS);
    }

    private void setStatus(String status) {
        entityData.set(DATA_STATUS, status);
    }

    public boolean isProcessing() {
        return entityData.get(DATA_PROCESSING);
    }

    private void setProcessing(boolean processing) {
        entityData.set(DATA_PROCESSING, processing);
    }

    public float getProgressFraction() {
        return getMaxProgress() <= 0 ? 0f : Mth.clamp((float) getProgress() / getMaxProgress(), 0f, 1f);
    }

    private int getProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    private void setProgress(int progress) {
        entityData.set(DATA_PROGRESS, progress);
    }

    private int getMaxProgress() {
        return entityData.get(DATA_MAX_PROGRESS);
    }

    private void setMaxProgress(int maxProgress) {
        entityData.set(DATA_MAX_PROGRESS, maxProgress);
    }


    private void playFail() {
        level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    private static void sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(payload.toVanillaServerbound());
        }
    }

    private static boolean canHoldEnchantments(ItemStack stack) {
        return EnchantmentHelper.canStoreEnchantments(stack);
    }

    @Nullable
    private static Holder<Enchantment> resolveEnchantment(Level level, ResourceKey<Enchantment> key) {
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return registry.get(key).orElse(null);
    }

    private static String toRoman(int level) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= numerals.length ? numerals[level - 1] : String.valueOf(level);
    }

    private static String formatLight(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static SDFRectTexture cellTexture(boolean[] cells, int row, int col) {
        return SDFRectTexture.of(cells[row * LightWeaverShapes.GRID_SIZE + col] ? CELL_FILLED : CELL_EMPTY)
                .setRadius(2f)
                .setBorderColor(DARK_GOLD);
    }

    private static void setCell(UIElement cell, boolean[] cells, int row, int col, boolean filled) {
        cells[row * LightWeaverShapes.GRID_SIZE + col] = filled;
        cell.style(s -> s.background(cellTexture(cells, row, col)));
    }

    private static void updateHint(Label hintLabel, boolean[] cells) {
        if (LightWeaverShapes.isEmpty(cells)) {
            hintLabel.setText("Draw a shape");
            return;
        }
        WeaverShape match = LightWeaverShapes.match(cells);
        hintLabel.setText(match != null ? "Recognized: " + match.displayName() : "Unrecognized shape");
    }

    private static void refreshGrid(UIElement grid, boolean[] cells) {
        int index = 0;
        for (UIElement row : grid.getChildren()) {
            for (UIElement cell : row.getChildren()) {
                final int i = index;
                cell.style(s -> s.background(cellTexture(cells, i / LightWeaverShapes.GRID_SIZE, i % LightWeaverShapes.GRID_SIZE)));
                index++;
            }
        }
    }

    private static Button brightButton(String text, UIEventListener onClick) {
        var btn = new Button().setText(text).setOnClick(onClick);
        btn.layout(l -> l.widthPercent(100).height(22));
        btn.textStyle(ts -> ts.textColor(BUTTON_TEXT).textShadow(true).fontSize(11));
        btn.style(s -> s.background(SDFRectTexture.of(BUTTON_BG).setRadius(6f).setBorderColor(DARK_GOLD)));
        btn.buttonStyle(s -> s
                .baseTexture(SDFRectTexture.of(BUTTON_BG).setRadius(6f).setBorderColor(DARK_GOLD))
                .hoverTexture(SDFRectTexture.of(BUTTON_HOVER).setRadius(6f).setBorderColor(DIAMOND))
                .pressedTexture(SDFRectTexture.of(BUTTON_PRESSED).setRadius(6f).setBorderColor(GOLD_BORDER)));
        return btn;
    }

    private static UIElement ornament() {
        var wrap = new UIElement()
                .layout(l -> l.widthPercent(100).height(8)
                        .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));

        var lineLeft = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        var diamond = new UIElement()
                .layout(l -> l.width(5).height(5))
                .style(s -> s.background(SDFRectTexture.of(DIAMOND).setRadius(1f)));
        diamond.transform(t -> t.rotation(45f));

        var lineRight = new UIElement()
                .layout(l -> l.flex(1).height(1))
                .style(s -> s.background(SDFRectTexture.of(DARK_GOLD).setRadius(0.5f)));

        wrap.addChildren(lineLeft, diamond, lineRight);
        return wrap;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>("controller", state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}
