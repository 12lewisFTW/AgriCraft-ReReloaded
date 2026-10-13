package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.api.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.agricraft.agricraft.common.item.TrowelItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Safe entry point: this class has no references to optional Starfield types. */
public final class StarfieldCompat {
    private static final Set<String> VERIFIED_VERSIONS = Set.of("0.6.3fix1");
    private static volatile Bridge bridge;
    private static volatile Status status = Status.NOT_STARTED;
    private static final Map<GlobalPos, StarfieldPlantData.Entry> inactiveRemovals = new HashMap<>();
    private static final Map<GlobalPos, StarfieldPlantData.Entry> brokenSticks = new HashMap<>();

    private StarfieldCompat() {}

    public enum Status { NOT_STARTED, ABSENT, DISABLED, INCOMPATIBLE, FAILED, READY }
    public enum Part { ROOT, UPPER, EXTENSION }

    /** Descriptive metadata, not an executable phase schedule. Native code owns crop behavior. */
    public record Metadata(List<String> seasons, List<Integer> phaseDays, int regrowDays,
                           int farmingExperience, ResourceLocation harvestMethod,
                           ResourceLocation produce, ResourceLocation seed) {
        public Metadata { seasons = List.copyOf(seasons); phaseDays = List.copyOf(phaseDays); }
    }

    public record Species(ResourceLocation id, ResourceLocation seed, Optional<ResourceLocation> produce,
                          ResourceLocation icon, List<Integer> nativePhaseDays, int nativeRegrowDays,
                          boolean twoParts, boolean requiresScythe, boolean seasonalForage,
                          Optional<Metadata> metadata) {
        public Species { nativePhaseDays = List.copyOf(nativePhaseDays); }
    }

    /** A visual stage is intentionally separate from elapsed days and native phase persistence. */
    public record Plant(Species species, BlockPos root, Part part, int visualStage, boolean mature,
                        Optional<Metadata> metadata) {
        public Plant { root = root.immutable(); }
    }

    public record Soil(boolean sandy, boolean infertile, boolean blocksSprinklers) {}

    // Mod presence/version is immutable for this process. The lazy holder avoids
    // consulting ModList in hot paths and waits until normal mod construction.
    private static final class Dependency {
        private static final String VERSION = ModList.get().getMods().stream()
                .filter(mod -> mod.getModId().equals(CompatConfig.STARFIELD_MOD_ID))
                .map(mod -> mod.getVersion().toString()).findFirst().orElse("");
        private static final boolean AVAILABLE = isVerifiedVersion(VERSION);
    }

    public static boolean isAvailable() { return Dependency.AVAILABLE; }

    public static void register(IEventBus modBus) {
        // Keep channel declarations stable for AgriCraft's network protocol. They
        // do not send packets or schedule gameplay work when the dependency is absent.
        modBus.addListener(StarfieldPlantSync::register);
        if (!isAvailable()) {
            status = Dependency.VERSION.isEmpty() ? Status.ABSENT : Status.INCOMPATIBLE;
            if (status == Status.INCOMPATIBLE) AgriCraft.LOGGER.warn(
                    "Starfield compatibility is inactive: unverified version {} (verified: {}). Native crops are unchanged.",
                    Dependency.VERSION, VERIFIED_VERSIONS);
            return;
        }
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(StarfieldCompat::installSeeds));
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onAboutToStart);
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onStarted);
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onStopped);
        NeoForge.EVENT_BUS.addListener(StarfieldPlantSync::sent);
        NeoForge.EVENT_BUS.addListener(StarfieldPlantSync::unwatch);
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onEndTick);
        NeoForge.EVENT_BUS.addListener(StarfieldCompat::onLogout);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, StarfieldCompat::onRightClick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, StarfieldCompat::onBreak);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, StarfieldCompat::onDrops);
    }

    private static void installSeeds() {
        if (!CompatConfig.enableStarfield) return;
        var mod = ModList.get().getModContainerById(CompatConfig.STARFIELD_MOD_ID);
        if (mod.isEmpty() || !isVerifiedVersion(mod.get().getModInfo().getVersion().toString())) return;
        try {
            com.agricraft.agricraft.api.compat.ExternalSeedSupport.register((com.agricraft.agricraft.api.compat.ExternalSeedSupport.Provider)
                    Class.forName("com.agricraft.agricraft.compat.starfield.NativeStarfieldSeeds").getConstructor().newInstance());
            com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.register((com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Provider)
                    Class.forName("com.agricraft.agricraft.compat.starfield.NativeStarfieldIrrigation").getConstructor().newInstance());
            com.agricraft.agricraft.api.compat.ExternalCrossBreedingSupport.register((com.agricraft.agricraft.api.compat.ExternalCrossBreedingSupport.Provider)
                    Class.forName("com.agricraft.agricraft.compat.starfield.NativeStarfieldBreeding").getConstructor().newInstance());
            com.agricraft.agricraft.api.compat.ExternalHarvestSupport.register((com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Provider)
                    Class.forName("com.agricraft.agricraft.compat.starfield.NativeStarfieldAutomation").getConstructor().newInstance());
        } catch (ReflectiveOperationException | LinkageError failure) {
            AgriCraft.LOGGER.error("Cannot register optional Starfield seed/irrigation/breeding/harvest support", failure);
        }
    }

    private static void onAboutToStart(ServerAboutToStartEvent event) {
        StarfieldPlantSync.reset();
        inactiveRemovals.clear();
        brokenSticks.clear();
        bridge = null;
        if (!CompatConfig.enableStarfield) { status = Status.DISABLED; return; }
        var mod = ModList.get().getModContainerById(CompatConfig.STARFIELD_MOD_ID);
        if (mod.isEmpty()) { status = Status.ABSENT; return; }
        String version = mod.get().getModInfo().getVersion().toString();
        if (!isVerifiedVersion(version)) {
            status = Status.INCOMPATIBLE;
            AgriCraft.LOGGER.warn("Starfield compatibility is inactive: unverified version {} (verified: {}). Native crops are unchanged.",
                    version, VERIFIED_VERSIONS);
            return;
        }
        try {
            // The optional implementation is linked only after presence, config and version checks.
            bridge = (Bridge) Class.forName("com.agricraft.agricraft.compat.starfield.NativeStarfieldCatalog")
                    .getConstructor().newInstance();
            status = Status.NOT_STARTED;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            status = Status.FAILED;
            AgriCraft.LOGGER.error("Cannot load verified Starfield compatibility; native crops are unchanged", exception);
        }
    }

    public static boolean isVerifiedVersion(String version) { return VERIFIED_VERSIONS.contains(version); }

    private static void onStarted(ServerStartedEvent event) { refresh(event.getServer().overworld()); }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) refresh(event.getPlayerList().getServer().overworld());
    }

    private static void onStopped(ServerStoppedEvent event) {
        if (bridge != null) bridge.reset();
        StarfieldPlantSync.reset();
        inactiveRemovals.clear();
        brokenSticks.clear();
        bridge = null;
        status = Status.NOT_STARTED;
        com.agricraft.agricraft.api.AgriApi.getMutationHandler().invalidateComplexities();
    }

    /** Refresh only descriptive data; no block, genome, scheduler or SavedData mutation. */
    public static void refresh(ServerLevel level) {
        var active = bridge;
        if (active == null || !CompatConfig.enableStarfield) return;
        try {
            active.refresh(level);
            status = Status.READY;
            com.agricraft.agricraft.api.AgriApi.getMutationHandler().invalidateComplexities();
        } catch (LinkageError | RuntimeException exception) {
            bridge = null;
            status = Status.FAILED;
            AgriCraft.LOGGER.error("Starfield catalog refresh failed; compatibility is inactive and native crops are unchanged", exception);
            com.agricraft.agricraft.api.AgriApi.getMutationHandler().invalidateComplexities();
        }
    }

    public static Status status() { return CompatConfig.enableStarfield ? status : Status.DISABLED; }

    public static boolean soilAdvance(ServerLevel level, BlockPos root, boolean infertile, java.util.function.BooleanSupplier original) {
        return isActive() ? active().soilAdvance(level, root, infertile, original) : original.getAsBoolean();
    }

    public static boolean seasonPaused(ServerLevel level, BlockPos root, BlockState state) {
        return isActive() && active().seasonPaused(level, root, state);
    }

    public static java.util.Collection<BlockPos> giantSeasonEligible(ServerLevel level, java.util.Collection<BlockPos> roots, int day, boolean offline) {
        return isActive() ? active().giantSeasonEligible(level, roots, day, offline) : roots;
    }

    public static void giantBegin() { if (isActive()) active().giantBegin(); }
    public static Optional<?> production(Object machine, ItemStack input, Optional<?> plan) {
        return isActive() ? active().production(machine, input, plan) : plan;
    }
    public static ItemStack automation(ServerLevel level, BlockPos pos, java.util.function.Consumer<ItemStack> output,
            java.util.function.Function<java.util.function.Consumer<ItemStack>, ItemStack> original) {
        return isActive() ? active().automation(level, pos, output, original) : original.apply(output);
    }
    public static void giantEnd() { if (isActive()) active().giantEnd(); }
    public static List<?> giantPlan(Object context, List<?> crops, List<?> plan) {
        return isActive() ? active().giantPlan(context, crops, plan) : plan;
    }
    public static void giantCommit(ServerLevel level) { if (isActive()) active().giantCommit(level); }

    private static Bridge active() { return CompatConfig.enableStarfield ? bridge : null; }

    public static boolean isActive() { return active() != null && status == Status.READY; }

    public static boolean mayUseTools(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        return isActive() && player.mayBuild() && player.serverLevel().mayInteract(player, pos) && active().mayUseTools(player, pos);
    }
    public static Optional<net.minecraft.nbt.CompoundTag> inspection(ServerLevel level, BlockPos pos) {
        return isActive() && level.hasChunkAt(pos) ? active().inspection(level, pos) : Optional.empty();
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk)
            level.getServer().execute(() -> {
                if (level.hasChunk(chunk.getPos().x, chunk.getPos().z)) scan(level, chunk);
            });
    }
    private static void onEndTick(ServerTickEvent.Post event) {
        // An already loaded bridge also clears transient caches while configuration is off.
        // Startup in disabled/absent modes still never constructs it.
        if (bridge != null) bridge.endTick();
        brokenSticks.clear();
        inactiveRemovals.clear();
    }
    private static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!isActive() || !(event.getLevel() instanceof ServerLevel level)) return;
        var root = event.getPos(); var state = event.getState();
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) root = root.below();
        if (!level.hasChunkAt(root)) return;
        var data = StarfieldPlantData.existing(level);
        var key = GlobalPos.of(level.dimension(), root);
        if (data != null) data.get(key).filter(entry -> !entry.sticks().isEmpty()).ifPresent(entry -> {
            if (event.getPlayer().isCreative()) consumed(entry.plantingId());
            else brokenSticks.put(key, entry);
        });
    }
    private static void onDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (!isActive() || !(event.getBreaker() instanceof net.minecraft.world.entity.player.Player player) || player.isCreative()) return;
        var root = event.getPos(); var state = event.getState();
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) root = root.below();
        var entry = brokenSticks.remove(GlobalPos.of(event.getLevel().dimension(), root));
        if (entry == null) return;
        consumed(entry.plantingId());
        if (!event.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) return;
        var material = com.agricraft.agricraft.common.block.CropStickVariant.toItem(com.agricraft.agricraft.common.block.CropStickVariant.valueOf(entry.sticks().toUpperCase(java.util.Locale.ROOT)));
        var drop = new net.minecraft.world.entity.item.ItemEntity(event.getLevel(), root.getX()+.5, root.getY()+.5, root.getZ()+.5, material, 0,.2,0);
        drop.setDefaultPickUpDelay(); event.getDrops().add(drop);
    }
    static void consumed(java.util.UUID plantingId) { if (isActive()) active().consumed(plantingId); }
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) StarfieldPlantSync.logout(player);
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        var context = new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec());
        var result = StarfieldTools.use(context);
        result.ifPresent(value -> { event.setCanceled(true); event.setCancellationResult(value); });
    }

    public static InteractionResult useSeed(UseOnContext context, Supplier<InteractionResult> original) {
        return isActive() ? active().useSeed(context, original) : original.get();
    }
    public static void harvest(ServerLevel level, BlockPos pos, net.minecraft.world.entity.player.Player player, Runnable original) {
        if (isActive()) active().harvest(level, pos, player, original);
        else original.run();
    }
    public static boolean rejectHarvestFallback(ItemStack stack) {
        if (!CompatConfig.enableStarfield) return false;
        var item = stack.getItem();
        return item instanceof com.agricraft.agricraft.common.item.ClipperItem
                || item instanceof com.agricraft.agricraft.common.item.RakeItem
                || item instanceof com.agricraft.agricraft.common.item.MagnifyingGlassItem
                || item instanceof TrowelItem
                || item instanceof com.agricraft.agricraft.common.item.CropSticksItem
                || item instanceof com.agricraft.agricraft.common.item.SeedBagItem;
    }
    public static Optional<InteractionResult> trowel(UseOnContext context) {
        if (isActive()) return active().trowel(context);
        return context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .contains(StarfieldGenomes.TRANSFER_KEY) ? Optional.of(InteractionResult.FAIL) : Optional.empty();
    }
    public static Optional<InteractionResult> sticks(UseOnContext context) {
        if (!CompatConfig.enableStarfield) return Optional.empty();
        if (context.getLevel().isClientSide()) return StarfieldTools.clientSticks(context);
        return isActive() ? active().sticks(context) : Optional.empty();
    }
    public static Optional<StarfieldPlantData.Entry> planting(ServerLevel level, BlockPos position) {
        return isActive() ? active().planting(level, position) : Optional.empty();
    }
    public static Optional<ItemStack> seedFromPlant(ServerLevel level, BlockPos position) {
        return isActive() ? active().seedFromPlant(level, position) : Optional.empty();
    }
    public static boolean setSticks(ServerLevel level, BlockPos position, String variant) {
        return isActive() && active().setSticks(level, position, variant);
    }
    public static void scan(ServerLevel level, LevelChunk chunk) { if (isActive()) active().scan(level, chunk); }
    public static void placed(BlockState state, Level level, BlockPos pos, BlockState old) {
        if (isActive()) active().placed(state, level, pos, old);
    }
    public static void removing(BlockState state, Level level, BlockPos pos, BlockState next) {
        if (isActive()) { active().removing(state, level, pos, next); return; }
        // Dormant identity cleanup uses only Minecraft/AgriCraft types. It does not link the
        // optional implementation while disabled, or modify native blocks/scheduler state.
        if (level instanceof ServerLevel server && !state.is(next.getBlock())
                && (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)) {
            var data = StarfieldPlantData.existing(server);
            if (data != null) {
                data.get(GlobalPos.of(level.dimension(), pos)).ifPresent(entry -> inactiveRemovals.put(
                        GlobalPos.of(level.dimension(), pos.immutable()), entry));
                data.remove(server, pos);
            }
        }
    }
    public static void restored(ServerLevel level, BlockPos pos, Object growth) {
        if (isActive()) { active().restored(level, pos, growth); return; }
        var entry = inactiveRemovals.remove(GlobalPos.of(level.dimension(), pos));
        if (entry != null && BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(entry.form())) {
            var data = StarfieldPlantData.existing(level);
            if (data != null && data.writable(GlobalPos.of(level.dimension(), pos))) data.put(level, pos, entry);
        }
    }

    public static void transformed(ServerLevel level, BlockPos root, Runnable original) {
        if (isActive()) { active().transformed(level, root, original); return; }
        // Keep dormant wild-seed identity when the native scheduler changes its form.
        // This path only preserves our metadata; native selection and growth run unchanged.
        var data = StarfieldPlantData.existing(level);
        var key = GlobalPos.of(level.dimension(), root);
        var previous = data == null ? Optional.<StarfieldPlantData.Entry>empty() : data.get(key);
        original.run();
        if (previous.isEmpty() || !data.writable(key)) return;
        var old = previous.get();
        var form = BuiltInRegistries.BLOCK.getKey(level.getBlockState(root).getBlock());
        var encoded = old.encode();
        encoded.putString("form", form.toString());
        try {
            var next = StarfieldPlantData.Entry.decode(encoded);
            if (!next.form().equals(next.type())) data.put(level, root, next);
        } catch (IllegalArgumentException ignored) {
            // A different native replacement is a removal, not a compatible forage form.
        }
    }
    public static void forageRemoving(BlockState state, Level level, BlockPos pos, BlockState next) {
        if (isActive()) active().forageRemoving(state, level, pos, next); else removing(state, level, pos, next);
    }

    public static List<Species> species() {
        var active = active();
        return active == null ? List.of() : active.species();
    }

    public static Optional<Species> species(ResourceLocation id) {
        var active = active();
        return active == null ? Optional.empty() : active.species(id);
    }

    public static Optional<Species> seed(ItemStack stack) {
        var active = active();
        return active == null || stack.isEmpty() ? Optional.empty() : active.seed(stack);
    }

    public static Optional<Plant> inspect(Level level, BlockPos position) {
        var active = active();
        return active == null ? Optional.empty() : active.inspect(level, position);
    }

    public static Optional<Soil> soil(BlockState state) {
        var active = active();
        return active == null ? Optional.empty() : active.soil(state);
    }

    public static List<String> diagnostics() {
        var active = active();
        return active == null ? List.of() : active.diagnostics();
    }

    interface Bridge {
        void consumed(java.util.UUID plantingId);
        Optional<?> production(Object machine, ItemStack input, Optional<?> plan);
        ItemStack automation(ServerLevel level, BlockPos pos, java.util.function.Consumer<ItemStack> output,
                java.util.function.Function<java.util.function.Consumer<ItemStack>, ItemStack> original);
        void giantBegin();
        void giantEnd();
        List<?> giantPlan(Object context, List<?> crops, List<?> plan);
        void giantCommit(ServerLevel level);
        void transformed(ServerLevel level, BlockPos root, Runnable original);
        void forageRemoving(BlockState state, Level level, BlockPos pos, BlockState next);
        boolean soilAdvance(ServerLevel level, BlockPos root, boolean infertile, java.util.function.BooleanSupplier original);
        boolean seasonPaused(ServerLevel level, BlockPos root, BlockState state);
        java.util.Collection<BlockPos> giantSeasonEligible(ServerLevel level, java.util.Collection<BlockPos> roots, int day, boolean offline);
        Optional<net.minecraft.nbt.CompoundTag> inspection(ServerLevel level, BlockPos pos);
        boolean mayUseTools(net.minecraft.server.level.ServerPlayer player, BlockPos pos);
        void harvest(ServerLevel level, BlockPos pos, net.minecraft.world.entity.player.Player player, Runnable original);
        InteractionResult useSeed(UseOnContext context, Supplier<InteractionResult> original);
        Optional<InteractionResult> trowel(UseOnContext context);
        Optional<InteractionResult> sticks(UseOnContext context);
        Optional<StarfieldPlantData.Entry> planting(ServerLevel level, BlockPos position);
        Optional<ItemStack> seedFromPlant(ServerLevel level, BlockPos position);
        boolean setSticks(ServerLevel level, BlockPos position, String variant);
        void scan(ServerLevel level, LevelChunk chunk);
        void placed(BlockState state, Level level, BlockPos pos, BlockState old);
        void removing(BlockState state, Level level, BlockPos pos, BlockState next);
        void restored(ServerLevel level, BlockPos pos, Object growth);
        void endTick();
        void reset();
        void refresh(ServerLevel level);
        List<Species> species();
        Optional<Species> species(ResourceLocation id);
        Optional<Species> seed(ItemStack stack);
        Optional<Plant> inspect(Level level, BlockPos position);
        Optional<Soil> soil(BlockState state);
        List<String> diagnostics();
    }
}
