package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.compat.ExternalCrossBreedingSupport;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizer;
import com.agricraft.agricraft.api.genetic.AgriCrossBreedEngine;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.api.requirement.AgriGrowthResponse;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.SimpleFluidloggedBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.terrain.TerrainSoils;
import com.stardew.craft.core.FarmAreaResolver;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Guarded native placement; the active AgriCraft engine owns all genetic rolls. */
public final class NativeStarfieldBreeding implements ExternalCrossBreedingSupport.Provider {
    // A breeding descriptor only: no registered virtual plant, products or growth executor.
    private static final AgriPlant DESCRIPTOR = new AgriPlant.Builder().stages(2, 4).harvest(0)
            .chances(0, 0, 0.1).build();

    @Override public boolean supportsSpecies(ResourceLocation species) {
        return StarfieldCompat.isActive() && StarfieldCompat.species(species).isPresent();
    }

    @Override public boolean supportsSticks(LevelReader world, BlockPos pos) {
        return StarfieldCompat.isActive() && world instanceof ServerLevel level
                && level.dimension().location().equals(ResourceLocation.parse("stardewcraft:stardew_valley"))
                && level.hasChunkAt(pos.below()) && TerrainSoils.cropSupport(level.getBlockState(pos.below()));
    }

    @Override public Optional<Boolean> breed(AgriCrossBreedEngine engine, AgriCrop crop, RandomSource random) {
        if (!StarfieldCompat.isActive() || !(crop instanceof CropBlockEntity target)
                || !(crop.getLevel() instanceof ServerLevel level) || target.isRemoved()
                || level.getBlockEntity(crop.getBlockPos()) != target || !crop.isCrossCropSticks()
                || crop.hasPlant() || crop.hasWeeds()) return Optional.empty();
        var parents = new ArrayList<AgriCrop>();
        var roots = new HashSet<BlockPos>();
        boolean nativePresent = false;
        for (var direction : Direction.Plane.HORIZONTAL) {
            var pos = crop.getBlockPos().relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            var ordinary = AgriApi.getCrop(level, pos);
            if (ordinary.isPresent()) { parents.add(ordinary.get()); continue; }
            var parentState = level.getBlockState(pos);
            var forage = NativeStarfieldSpecials.forageEntry(level, pos);
            StardewCropBlock block;
            if (forage.isPresent()) block = (StardewCropBlock) BuiltInRegistries.BLOCK.get(forage.get().type());
            else if (parentState.getBlock() instanceof StardewCropBlock nativeBlock
                    && StarfieldCompat.species(BuiltInRegistries.BLOCK.getKey(nativeBlock)).isPresent()) block = nativeBlock;
            else continue;
            nativePresent = true;
            // Horizontal neighbours at the root's height, never an upper carrier as a second parent.
            if (!NativeStarfieldLifecycle.rootAt(level, pos).equals(pos) || !roots.add(pos)
                    || !Objects.equals(FarmAreaResolver.getOwnerAt(pos), FarmAreaResolver.getOwnerAt(crop.getBlockPos()))) continue;
            var entry = StarfieldCompat.planting(level, pos);
            if (entry.isEmpty()) continue;
            parents.add(new View(level, pos, AgriGenome.fromNBT(entry.get().genome()), forage.isPresent() || mature(level, pos, parentState),
                    block.canPlantAt(level, pos), !entry.get().sticks().isEmpty()));
        }
        if (!nativePresent) return Optional.empty();
        try {
            var selected = engine.selectParents(parents.stream(), random);
            if (selected.isEmpty()) return Optional.of(false);
            if (selected.stream().limit(2).noneMatch(View.class::isInstance))
                return Optional.of(engine.breedSelected(crop, selected, random));
            var staging = new View(level, crop.getBlockPos(), null, false, false, true);
            if (!engine.breedSelected(staging, selected, random) || staging.genome == null) return Optional.of(false);
            var childId = ResourceLocation.tryParse(staging.genome.getSpeciesGene().getDominant().trait());
            if (childId == null || !allowedSpecies(selected, childId)) return Optional.of(false);
            return Optional.of(place(target, staging.genome, childId));
        } catch (RuntimeException failure) {
            AgriCraft.LOGGER.warn("Starfield crossbreeding rejected at {}: {}", crop.getBlockPos(), failure.toString());
            return Optional.of(false);
        }
    }

    private static boolean mature(ServerLevel level, BlockPos root, BlockState state) {
        // The pinned native isMature/readiness methods repair legacy phases in-place.
        // Selection needs only their readiness predicate, without performing that repair.
        var growth = CropGrowthManager.get(level).getState(level, root);
        return state.getValue(StardewCropBlock.AGE) >= StardewCropBlock.MAX_AGE
                && (growth == null || !growth.regrowing || growth.dayInPhase <= 0);
    }

    private static boolean allowedSpecies(List<AgriCrop> parents, ResourceLocation child) {
        String first = parents.getFirst().getPlantId();
        if (parents.size() == 1) return child.toString().equals(first);
        String second = parents.get(1).getPlantId();
        if (first.equals(second) && child.toString().equals(first)) return true;
        // Different phenotypes (or a new child phenotype) require an actual mutation recipe.
        return AgriApi.getMutationsFromParents(first, second).anyMatch(recipe -> recipe.child().equals(child));
    }

    private static boolean place(CropBlockEntity target, AgriGenome genome, ResourceLocation type) {
        var level = (ServerLevel) target.getLevel(); var root = target.getBlockPos();
        var species = StarfieldCompat.species(type);
        if (species.isEmpty() || !level.dimension().location().equals(ResourceLocation.parse("stardewcraft:stardew_valley"))
                || !StarfieldPlantData.get(level).writable(GlobalPos.of(level.dimension(), root))) return false;
        var old = level.getBlockState(root);
        var state = BuiltInRegistries.BLOCK.get(type).defaultBlockState();
        if (!(state.getBlock() instanceof StardewCropBlock block) || !block.canPlantAt(level, root)
                || !NativeStarfieldSpecials.plantingAllowed(level, root, state)
                || !state.canSurvive(level, root) || !level.hasChunkAt(root.above())
                || level.isOutsideBuildHeight(root) || species.get().twoParts() && level.isOutsideBuildHeight(root.above())
                || !target.isCrossCropSticks() || target.hasPlant() || target.hasWeeds()
                || old.getValue(SimpleFluidloggedBlock.WATERLOGGED) || old.getValue(SimpleFluidloggedBlock.LAVALOGGED)
                || species.get().twoParts() && !level.getBlockState(root.above()).isAir()) return false;
        var tag = new CompoundTag(); genome.writeToNBT(tag); StarfieldGenomes.validate(tag, type);
        var seed = new ItemStack(BuiltInRegistries.ITEM.get(species.get().seed()));
        StarfieldGenomes.writeSeed(seed, type, tag);
        var savedTarget = target.saveWithFullMetadata(level.registryAccess());
        var oldUpper = level.getBlockState(root.above());
        var snapshots = new ArrayList<BlockSnapshot>();
        snapshots.add(BlockSnapshot.create(level.dimension(), level, root, Block.UPDATE_ALL));
        if (species.get().twoParts()) snapshots.add(BlockSnapshot.create(level.dimension(), level, root.above(), Block.UPDATE_ALL));
        boolean committed = false;
        try {
            // This is the final conversion from empty sticks to the native child, never a
            // temporary substitution of an existing native plant or its daily executor.
            if (!level.setBlock(root, state, Block.UPDATE_ALL)) return false;
            if (species.get().twoParts() && level.getBlockState(root.above()).isAir())
                level.setBlock(root.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
            if (!level.getBlockState(root).is(block) || !state.canSurvive(level, root)
                    || species.get().twoParts() && (!level.getBlockState(root.above()).is(block)
                    || level.getBlockState(root.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.UPPER)
                    || CropGrowthManager.get(level).getState(level, root) == null) return false;
            boolean canceled = snapshots.size() == 1 ? EventHooks.onBlockPlace(null, snapshots.getFirst(), Direction.UP)
                    : EventHooks.onMultiBlockPlace(null, snapshots, Direction.UP);
            if (canceled || !level.getBlockState(root).is(block)) return false;
            var entry = NativeStarfieldLifecycle.ensure(level, root).orElseThrow();
            StarfieldPlantData.get(level).put(level, root, new StarfieldPlantData.Entry(entry.plantingId(), type, tag,
                    (CompoundTag) seed.save(level.registryAccess()), old.getValue(CropBlock.STICK_VARIANT).getSerializedName()));
            committed = true;
            return true;
        } finally {
            if (!committed) {
                level.setBlock(root, old, Block.UPDATE_ALL);
                if (species.get().twoParts()) level.setBlock(root.above(), oldUpper, Block.UPDATE_ALL);
                var restored = level.getBlockEntity(root);
                if (restored instanceof CropBlockEntity) restored.loadWithComponents(savedTarget, level.registryAccess());
            }
        }
    }

    /** Confined breeding view. It cannot harvest, advance or mutate a parent in the world. */
    private static final class View implements AgriCrop {
        private final ServerLevel level;
        private final BlockPos pos;
        private AgriGenome genome;
        private final boolean mature, fertile, sticks;
        View(ServerLevel level, BlockPos pos, AgriGenome genome, boolean mature, boolean fertile, boolean sticks) {
            this.level = level; this.pos = pos; this.genome = genome;
            this.mature = mature; this.fertile = fertile; this.sticks = sticks;
        }
        @Override public boolean hasPlant() { return genome != null; }
        @Override public boolean hasWeeds() { return false; }
        @Override public boolean hasCropSticks() { return sticks; }
        @Override public boolean isCrossCropSticks() { return false; }
        @Override public String getPlantId() { return genome == null ? "" : genome.getSpeciesGene().getDominant().trait(); }
        @Override public AgriPlant getPlant() { return DESCRIPTOR; }
        @Override public AgriGenome getGenome() { return genome; }
        @Override public AgriGrowthStage getGrowthStage() { return new AgriGrowthStage(mature ? 1 : 0, 2); }
        @Override public boolean canBeHarvested() { return hasPlant() && mature; }
        @Override public AgriGrowthResponse getFertilityResponse() { return fertile ? AgriGrowthResponse.FERTILE : AgriGrowthResponse.INFERTILE; }
        @Override public Optional<AgriSoil> getSoil() { return Optional.empty(); }
        @Override public BlockPos getBlockPos() { return pos; }
        @Override public BlockState getBlockState() { return level.getBlockState(pos); }
        @Override public ServerLevel getLevel() { return level; }
        @Override public String getWeedId() { return ""; }
        @Override public AgriWeed getWeed() { return null; }
        @Override public AgriGrowthStage getWeedGrowthStage() { return new AgriGrowthStage(0, 2); }
        @Override public boolean acceptsFertilizer(AgriFertilizer fertilizer) { return false; }
        @Override public boolean harvest(Consumer<ItemStack> consumer, LivingEntity entity) { return false; }
        @Override public void getHarvestProducts(Consumer<ItemStack> consumer) {}
        @Override public void getClippingProducts(Consumer<ItemStack> consumer, ItemStack clipper) {}
        @Override public void plantGenome(AgriGenome value, LivingEntity entity) {
            if (genome != null) throw new UnsupportedOperationException("Read-only parent");
            genome = value;
        }
        @Override public boolean removeGenome() { return false; }
        @Override public void setGrowthStage(AgriGrowthStage stage) { throw new UnsupportedOperationException("Native growth"); }
        @Override public void setWeedGrowthStage(AgriGrowthStage stage) { throw new UnsupportedOperationException("Native growth"); }
        @Override public void setWeed(String id, AgriWeed weed) { throw new UnsupportedOperationException("Native parent"); }
        @Override public void removeWeeds() {}
        @Override public void applyGrowthTick() { throw new UnsupportedOperationException("Native growth"); }
    }
}
