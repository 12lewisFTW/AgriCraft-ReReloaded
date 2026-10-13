package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.compat.ExternalSeedSupport;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.stardew.craft.block.crop.StardewCropBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.Optional;

/** Guarded registration; remote clients resolve real seed items from native block accessors. */
public final class NativeStarfieldSeeds implements ExternalSeedSupport.Provider {
    @Override public boolean mayUseAnalyzer(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        return !(player instanceof net.minecraft.server.level.ServerPlayer server) || !StarfieldCompat.isActive()
                || StarfieldCompat.mayUseTools(server, pos);
    }
    private Optional<ResourceLocation> type(ItemStack stack, Level level) {
        if (!CompatConfig.enableStarfield || stack.isEmpty() || level == null) return Optional.empty();
        if (!level.isClientSide) return StarfieldCompat.isActive() ? StarfieldCompat.seed(stack).map(StarfieldCompat.Species::id) : Optional.empty();
        return BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof StardewCropBlock crop && crop.isSeedItem(stack))
                .map(BuiltInRegistries.BLOCK::getKey).filter(id -> id.getNamespace().equals("stardewcraft")).findFirst();
    }
    @Override public boolean accepts(ItemStack stack, Level level) { return genome(stack, level).isPresent(); }
    @Override public Optional<AgriGenome> genome(ItemStack stack, Level level) {
        try { return type(stack, level).map(id -> AgriGenome.fromNBT(StarfieldGenomes.readSeed(stack, id))); }
        catch (RuntimeException invalid) { return Optional.empty(); }
    }
    @Override public ItemStack prepare(ItemStack stack, Level level) {
        try {
            var id = type(stack, level).orElseThrow();
            var result = stack.copy(); StarfieldGenomes.writeSeed(result, id, StarfieldGenomes.readSeed(stack, id)); return result;
        } catch (RuntimeException invalid) { return ItemStack.EMPTY; }
    }
}
