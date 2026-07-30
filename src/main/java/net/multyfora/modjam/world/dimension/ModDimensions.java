package net.multyfora.modjam.world.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.multyfora.modjam.modjam;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class ModDimensions {
    public static final ResourceKey<DimensionType> FIRST_CONTACT_TYPE_KEY = ResourceKey.create(
        Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact"));
    public static final ResourceKey<LevelStem> FIRST_CONTACT_STEM_KEY = ResourceKey.create(
        Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact"));
    public static final ResourceKey<Level> FIRST_CONTACT_LEVEL_KEY = ResourceKey.create(
        Registries.DIMENSION, Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact"));
    public static final ResourceKey<Biome> FIRST_CONTACT_BIOME_KEY = ResourceKey.create(
        Registries.BIOME, Identifier.fromNamespaceAndPath(modjam.MODID, "first_contact"));

    public static final DeferredRegister<Biome> BIOMES =
        DeferredRegister.create(Registries.BIOME, modjam.MODID);

    public static final DeferredHolder<Biome, Biome> FIRST_CONTACT_BIOME =
        BIOMES.register("first_contact", ModDimensions::createBiome);

    private static Biome createBiome() {
        var effects = new BiomeSpecialEffects(0x000000, Optional.empty(), Optional.empty(), Optional.empty(),
            BiomeSpecialEffects.GrassColorModifier.NONE);
        return new Biome.BiomeBuilder()
            .hasPrecipitation(false)
            .temperature(0.5f)
            .downfall(0.0f)
            .specialEffects(effects)
            .mobSpawnSettings(new MobSpawnSettings.Builder().build())
            .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
            .build();
    }
}
