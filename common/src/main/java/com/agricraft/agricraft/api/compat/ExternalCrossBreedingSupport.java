package com.agricraft.agricraft.api.compat;

import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.genetic.AgriCrossBreedEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import java.util.Optional;

/** Optional native crops participate only in an existing empty cross-crop tick. */
public final class ExternalCrossBreedingSupport {
    public interface Provider {
        Optional<Boolean> breed(AgriCrossBreedEngine engine, AgriCrop target, RandomSource random);
        boolean supportsSticks(LevelReader level, BlockPos pos);
        boolean supportsSpecies(ResourceLocation species);
    }
    private static Provider provider;
    private ExternalCrossBreedingSupport() {}
    public static void register(Provider value) { provider = value; }
    public static Optional<Boolean> breed(AgriCrossBreedEngine engine, AgriCrop target, RandomSource random) {
        return provider == null ? Optional.empty() : provider.breed(engine, target, random);
    }
    public static boolean supportsSticks(LevelReader level, BlockPos pos) {
        return provider != null && provider.supportsSticks(level, pos);
    }
    public static boolean supportsSpecies(ResourceLocation species) {
        return provider != null && provider.supportsSpecies(species);
    }
}
