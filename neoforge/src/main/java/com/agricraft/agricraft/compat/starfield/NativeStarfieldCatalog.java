package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.agriculture.StardewCropData;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.crop.WildSeedCropBlock;
import com.stardew.craft.block.terrain.TerrainSoils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Guarded native bridge; growth and harvest calculations remain owned by Starfield. */
public final class NativeStarfieldCatalog implements StarfieldCompat.Bridge {
    @Override public void consumed(java.util.UUID plantingId) { NativeStarfieldLifecycle.consumed(plantingId); }
    @Override public Optional<?> production(Object machine, ItemStack input, Optional<?> plan) {
        return NativeStarfieldProduction.prepare(machine, input, plan);
    }
    @Override public ItemStack automation(ServerLevel level, BlockPos pos, java.util.function.Consumer<ItemStack> output,
            java.util.function.Function<java.util.function.Consumer<ItemStack>, ItemStack> original) {
        return NativeStarfieldAutomation.collect(level, pos, output, original);
    }
    @Override public void giantBegin() { NativeStarfieldGiants.begin(); }
    @Override public void giantEnd() { NativeStarfieldGiants.end(); }
    @Override public List<?> giantPlan(Object context, List<?> crops, List<?> plan) { return NativeStarfieldGiants.plan(context, crops, plan); }
    @Override public void giantCommit(ServerLevel level) { NativeStarfieldGiants.commit(level); }
    @Override public void transformed(ServerLevel level, BlockPos root, Runnable original) { NativeStarfieldSpecials.transformed(level, root, original); }
    @Override public void forageRemoving(BlockState state, Level level, BlockPos pos, BlockState next) { NativeStarfieldSpecials.removing(state, level, pos, next); }
    @Override public boolean soilAdvance(ServerLevel level, BlockPos root, boolean infertile, java.util.function.BooleanSupplier original) { return NativeStarfieldResilience.soil(level, root, infertile, original); }
    @Override public boolean seasonPaused(ServerLevel level, BlockPos root, BlockState state) { return NativeStarfieldResilience.paused(level, root, state); }
    @Override public java.util.Collection<BlockPos> giantSeasonEligible(ServerLevel level, java.util.Collection<BlockPos> roots, int day, boolean offline) { return NativeStarfieldResilience.giantEligible(level, roots, day, offline); }
    @Override public Optional<net.minecraft.nbt.CompoundTag> inspection(ServerLevel level, BlockPos pos) { return NativeStarfieldInspection.read(level, pos); }
    @Override public boolean mayUseTools(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        return com.stardew.craft.event.FarmAreaProtectionEvents.canModifyDecorationAt(player, player.serverLevel(), pos);
    }
    @Override public void harvest(ServerLevel level, BlockPos pos, net.minecraft.world.entity.player.Player player, Runnable original) {
        NativeStarfieldHarvest.harvest(level, pos, player, original);
    }
    @Override public net.minecraft.world.InteractionResult useSeed(net.minecraft.world.item.context.UseOnContext context,
            java.util.function.Supplier<net.minecraft.world.InteractionResult> original) { return NativeStarfieldLifecycle.useSeed(context, original); }
    @Override public Optional<net.minecraft.world.InteractionResult> trowel(net.minecraft.world.item.context.UseOnContext context) { return NativeStarfieldTransfer.use(context); }
    @Override public Optional<net.minecraft.world.InteractionResult> sticks(net.minecraft.world.item.context.UseOnContext context) { return NativeStarfieldSticks.use(context); }
    @Override public Optional<StarfieldPlantData.Entry> planting(ServerLevel level, BlockPos pos) { return NativeStarfieldLifecycle.ensure(level, pos); }
    @Override public Optional<ItemStack> seedFromPlant(ServerLevel level, BlockPos pos) { return NativeStarfieldLifecycle.seed(level, pos); }
    @Override public boolean setSticks(ServerLevel level, BlockPos pos, String variant) { return NativeStarfieldLifecycle.sticks(level, pos, variant); }
    @Override public void scan(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) { NativeStarfieldLifecycle.scan(level, chunk); }
    @Override public void placed(BlockState state, Level level, BlockPos pos, BlockState old) { NativeStarfieldLifecycle.placed(state, level, pos, old); }
    @Override public void removing(BlockState state, Level level, BlockPos pos, BlockState next) { NativeStarfieldLifecycle.removing(state, level, pos, next); }
    @Override public void restored(ServerLevel level, BlockPos pos, Object growth) { NativeStarfieldLifecycle.restored(level, pos, growth); }
    @Override public void endTick() { NativeStarfieldLifecycle.endTick(); NativeStarfieldGiants.reset(); }
    @Override public void reset() { NativeStarfieldLifecycle.reset(); NativeStarfieldGiants.reset(); }
    private static final Set<String> SEASONS = Set.of("spring", "summer", "fall", "winter");
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of(), List.of(), List.of());
    private final Set<String> providerFailures = ConcurrentHashMap.newKeySet();

    public NativeStarfieldCatalog() {}

    private record Snapshot(Map<ResourceLocation, StarfieldCompat.Species> byId,
                            Map<ResourceLocation, StarfieldCompat.Species> bySeed,
                            List<StarfieldCompat.Species> ordered, List<String> diagnostics) {}

    @Override
    public void refresh(ServerLevel level) {
        NativeStarfieldLifecycle.invalidateScans();
        var byId = new HashMap<ResourceLocation, StarfieldCompat.Species>();
        var bySeed = new HashMap<ResourceLocation, StarfieldCompat.Species>();
        var problems = new ArrayList<String>();
        var items = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR)
                .map(ItemStack::new).toList();
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("stardewcraft") || !(block instanceof StardewCropBlock crop)) continue;
            try {
                var seedItems = items.stream().filter(crop::isSeedItem).toList();
                if (seedItems.size() != 1) throw new IllegalArgumentException("Expected exactly one native seed, found " + seedItems.size());
                var seed = BuiltInRegistries.ITEM.getKey(seedItems.getFirst().getItem());
                var iconStack = crop.getCloneItemStack(level, BlockPos.ZERO, crop.defaultBlockState());
                if (iconStack.isEmpty()) throw new IllegalArgumentException("Native crop icon is empty");
                var icon = BuiltInRegistries.ITEM.getKey(iconStack.getItem());
                var phases = Arrays.stream(crop.getPhaseDaysForDisplay()).boxed().toList();
                if (phases.isEmpty() || phases.stream().anyMatch(day -> day < 0))
                    throw new IllegalArgumentException("Invalid native display phases " + phases);
                var metadata = metadata(StardewAgricultureDataApi.crop(crop.defaultBlockState()));
                boolean forage = crop instanceof WildSeedCropBlock;
                var species = new StarfieldCompat.Species(id, seed, forage ? Optional.empty() : Optional.of(icon),
                        icon, phases, crop.canRegrowForDisplay() ? crop.getRegrowDaysForDisplay() : -1,
                        crop.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF),
                        crop.requiresScytheHarvest(), forage, metadata);
                if (bySeed.containsKey(seed)) throw new IllegalArgumentException("Native seed is ambiguous: " + seed);
                byId.put(id, species);
                bySeed.put(seed, species);
            } catch (RuntimeException exception) {
                problems.add(id + ": " + exception.getMessage());
            }
        }
        var ordered = byId.values().stream().sorted(Comparator.comparing(species -> species.id().toString())).toList();
        // Readers observe one complete generation. Invalid species disappear from lookup,
        // but no world state is deleted or silently converted to another species.
        snapshot = new Snapshot(Map.copyOf(byId), Map.copyOf(bySeed), ordered, List.copyOf(problems));
        providerFailures.clear();
        for (var problem : problems) AgriCraft.LOGGER.warn("Starfield crop excluded from compatibility catalog: {}", problem);
        AgriCraft.LOGGER.info("Starfield compatibility catalog: {} native crops, {} invalid entries", ordered.size(), problems.size());
    }

    private static Optional<StarfieldCompat.Metadata> metadata(StardewCropData data) {
        if (data == null) return Optional.empty();
        if (data.seasons().stream().anyMatch(season -> !SEASONS.contains(season)))
            throw new IllegalArgumentException("Unknown season in metadata: " + data.seasons());
        if (data.phaseDays().stream().anyMatch(day -> day < 0) || data.regrowDays() < -1 || data.farmingExperience() < 0)
            throw new IllegalArgumentException("Invalid phase/regrowth/experience metadata");
        requireItem(data.seed(), "metadata seed");
        requireItem(data.produce(), "metadata produce");
        if (!Set.of(ResourceLocation.parse("stardewcraft:grab"), ResourceLocation.parse("stardewcraft:scythe"))
                .contains(data.harvestMethod()))
            throw new IllegalArgumentException("Unknown harvest method: " + data.harvestMethod());
        return Optional.of(new StarfieldCompat.Metadata(data.seasons(), data.phaseDays(), data.regrowDays(),
                data.farmingExperience(), data.harvestMethod(), data.produce(), data.seed()));
    }

    private static void requireItem(ResourceLocation id, String role) {
        if (id == null || BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR).isEmpty())
            throw new IllegalArgumentException("Unknown " + role + ": " + id);
    }

    @Override public List<StarfieldCompat.Species> species() { return snapshot.ordered(); }
    @Override public Optional<StarfieldCompat.Species> species(ResourceLocation id) { return Optional.ofNullable(snapshot.byId().get(id)); }
    @Override public Optional<StarfieldCompat.Species> seed(ItemStack stack) {
        return Optional.ofNullable(snapshot.bySeed().get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    @Override
    public Optional<StarfieldCompat.Plant> inspect(Level level, BlockPos position) {
        if (level instanceof ServerLevel server) {
            var forage = NativeStarfieldSpecials.forageEntry(server, position);
            if (forage.isPresent()) return species(forage.get().type()).map(species -> new StarfieldCompat.Plant(
                    species, position, StarfieldCompat.Part.ROOT, StardewCropBlock.MAX_AGE, true, Optional.empty()));
        }
        var state = StardewCropRuntime.inspect(level, position);
        if (state == null) return Optional.empty();
        var species = snapshot.byId().get(state.typeId());
        if (species == null) return Optional.empty();
        try {
            var data = metadata(StardewAgricultureDataApi.crop(level, state.root(), level.getBlockState(state.root())));
            return Optional.of(new StarfieldCompat.Plant(species, state.root(),
                    StarfieldCompat.Part.valueOf(state.part().name()), state.visualStage(), state.mature(), data));
        } catch (RuntimeException exception) {
            var message = species.id() + ": " + exception.getMessage();
            if (providerFailures.add(message)) AgriCraft.LOGGER.warn("Starfield crop metadata rejected at {}: {}", state.root(), message);
            return Optional.empty();
        }
    }

    @Override public Optional<StarfieldCompat.Soil> soil(BlockState state) {
        return TerrainSoils.cropSupport(state) ? Optional.of(new StarfieldCompat.Soil(TerrainSoils.sandy(state),
                TerrainSoils.infertile(state), TerrainSoils.blocksSprinklers(state))) : Optional.empty();
    }

    @Override public List<String> diagnostics() {
        var problems = new ArrayList<>(snapshot.diagnostics());
        problems.addAll(providerFailures.stream().sorted().toList());
        return List.copyOf(problems);
    }
}
