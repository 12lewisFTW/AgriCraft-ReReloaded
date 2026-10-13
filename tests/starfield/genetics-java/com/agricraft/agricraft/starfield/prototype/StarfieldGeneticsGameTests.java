package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.registry.ModItems;
import com.agricraft.agricraft.compat.starfield.*;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.item.quality.QualityHelper;
import com.agricraft.agricraft.api.config.StatsConfig;
import com.agricraft.agricraft.api.config.CompatConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.time.StardewTimeManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldGeneticsGameTests {
    private static final ResourceLocation POTATO = ResourceLocation.parse("stardewcraft:potato_crop");

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeSeedRoundTripPreservesBothAllelesAndComponents(GameTestHelper helper) {
        var level = valley(helper);
        var root = root(level, 0);
        prepare(level, root);
        int season = StardewTimeManager.get().getCurrentSeason();
        StardewTimeManager.get().setCurrentSeason(0);
        try {
            var seed = seed(POTATO);
            seed.setCount(3);
            var foreign = new CompoundTag(); foreign.putInt("quality", 3); foreign.putString("other:owner", "untouched");
            seed.set(DataComponents.CUSTOM_DATA, CustomData.of(foreign));
            seed.set(DataComponents.CUSTOM_NAME, Component.literal("Native seed"));
            QualityHelper.setQuality(seed, QualityHelper.IRIDIUM);
            var genome = varied(POTATO);
            StarfieldGenomes.writeSeed(seed, POTATO, genome);
            var before = seed.copyWithCount(1);
            var result = use(level, root.below(), seed);
            helper.assertTrue(result.consumesAction(), "Native seed use failed: " + result + " soil=" + level.getBlockState(root.below()));
            var planted = StarfieldCompat.planting(level, root).orElseThrow();
            helper.assertTrue(planted.genome().equals(genome), "Seed use did not carry both alleles to the native plant");
            var back = StarfieldCompat.seedFromPlant(level, root).orElseThrow();
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, back), "Reverse seed lost native/foreign components");
            helper.assertTrue(QualityHelper.getQuality(back) == QualityHelper.IRIDIUM, "Reverse seed lost native quality");
            helper.assertTrue(seed.getCount() == 2 && StarfieldGenomes.readSeed(seed, POTATO).equals(genome), "Native seed consumption changed");
            helper.assertTrue(level.getBlockState(root).is(ModBlocks.POTATO_CROP.get()) && level.getBlockEntity(root) == null,
                    "Planting substituted the native block or added a block entity");
            helper.succeed();
        } finally { StardewTimeManager.get().setCurrentSeason(season); cleanup(level, root); }
    }

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void baselinesAreDeterministicAndBadSeedsFailBeforeMutation(GameTestHelper helper) {
        for (var species : StarfieldCompat.species()) {
            var seed = seed(species.id());
            var first = StarfieldGenomes.readSeed(seed, species.id());
            helper.assertTrue(first.equals(StarfieldGenomes.readSeed(seed, species.id())), "Baseline rolled again");
            var genome = AgriGenome.fromNBT(first);
            helper.assertTrue(genome.getStatGenes().size() == 6, "Expected six existing traits");
            for (var stat : AgriStatRegistry.getInstance()) helper.assertTrue(genome.getStatGene(stat).getTrait() == stat.getMin(), "Wrong baseline: " + stat.getId());
            helper.assertTrue(!seed.has(DataComponents.CUSTOM_DATA), "Reading a seed mutated its components");
        }
        int min = StatsConfig.growthMin, max = StatsConfig.growthMax;
        try {
            for (var bounds : List.of(new int[]{4, 8}, new int[]{7, 7}, new int[]{7, 6})) {
                StatsConfig.growthMin = bounds[0]; StatsConfig.growthMax = bounds[1];
                var genes = StarfieldGenomes.baseline(POTATO).getCompound("genes");
                var pair = genes.getCompound(AgriStatRegistry.getInstance().growthStat().getId());
                helper.assertTrue(pair.getInt("dom") == bounds[0] && pair.getInt("rec") == bounds[0], "Configured/frozen baseline changed");
            }
        } finally { StatsConfig.growthMin = min; StatsConfig.growthMax = max; }
        var level = valley(helper); var root = root(level, 16); prepare(level, root);
        try {
            var seed = seed(POTATO); var tag = new CompoundTag(); var envelope = new CompoundTag();
            envelope.putInt("schema", 99); tag.put(StarfieldGenomes.SEED_KEY, envelope);
            seed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); var before = seed.copy();
            helper.assertTrue(use(level, root.below(), seed) == InteractionResult.FAIL,
                    "Unknown seed schema was planted");
            helper.assertTrue(level.getBlockState(root).isAir() && ItemStack.isSameItemSameComponents(seed, before)
                    && seed.getCount() == before.getCount(), "Bad seed rejection changed world/item");
            helper.succeed();
        } finally { cleanup(level, root); }
    }

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void lifecycleDistinguishesReplantAndPreservesNativeRollback(GameTestHelper helper) {
        var level = valley(helper); var root = root(level, 32); prepare(level, root);
        try {
            level.setBlock(root, ModBlocks.CORN_CROP.get().defaultBlockState(), 3);
            var first = StarfieldCompat.planting(level, root).orElseThrow();
            helper.assertTrue(StarfieldCompat.planting(level, root.above()).orElseThrow().plantingId().equals(first.plantingId()), "Upper got a different UUID");
            var manager = CropGrowthManager.get(level); var growth = manager.getState(level, root);
            growth.phase = 2; growth.dayInPhase = 1; growth.lastDailyDay = 7;
            var before = level.getBlockState(root);
            helper.assertTrue(StarfieldCompat.setSticks(level, root.above(), "iron") && StarfieldCompat.setSticks(level, root, ""), "Representation metadata rejected");
            helper.assertTrue(level.getBlockState(root) == before && manager.getState(level, root) == growth
                    && StarfieldCompat.planting(level, root).orElseThrow().plantingId().equals(first.plantingId()), "Sticks reset identity/native progress");
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(), root)).isEmpty(), "Removal left a stale entry");
            level.setBlock(root, before, 3);
            var second = StarfieldCompat.planting(level, root).orElseThrow();
            helper.assertTrue(!second.plantingId().equals(first.plantingId()), "Same-species replant inherited old UUID");
            manager.restoreGrowthState(level, root, growth);
            helper.assertTrue(StarfieldCompat.planting(level, root).orElseThrow().plantingId().equals(first.plantingId()), "Native rejected-plan rollback lost original UUID");
            CompatConfig.enableStarfield = false;
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(), root)).isEmpty(), "Disabled integration retained a removed planting UUID");
            level.setBlock(root, before, 3);
            CompatConfig.enableStarfield = true;
            helper.assertTrue(!StarfieldCompat.planting(level, root).orElseThrow().plantingId().equals(first.plantingId()), "Reactivation confused a new plant with the old one");
            var other = helper.getLevel(); prepare(other, root);
            other.setBlock(root, ModBlocks.CORN_CROP.get().defaultBlockState(), 3);
            try {
                helper.assertTrue(!StarfieldCompat.planting(other, root).orElseThrow().plantingId()
                        .equals(StarfieldCompat.planting(level, root).orElseThrow().plantingId()), "Two dimensions share one planting identity");
            } finally { cleanup(other, root); }
            helper.succeed();
        } finally { CompatConfig.enableStarfield = true; cleanup(level, root); }
    }

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void trowelPreservesWholeNativeStateAndRejectsBlockedDestination(GameTestHelper helper) {
        var level = valley(helper); var source = root(level, 48); var target = source.offset(4, 0, 0);
        prepare(level, source); prepare(level, target);
        try {
            level.setBlock(source, ModBlocks.CORN_CROP.get().defaultBlockState(), 3);
            var first = StarfieldCompat.planting(level, source).orElseThrow();
            StarfieldCompat.setSticks(level, source, "obsidian");
            var manager = CropGrowthManager.get(level); var growth = manager.getState(level, source);
            growth.phase = 4; growth.dayInPhase = 1; growth.regrowing = true; growth.infertileProgress = 2;
            growth.lastDailyDay = 7; growth.lastDailyWatered = true; growth.lastGiantDay = 7;
            growth.sourcePhaseVersion = 1; growth.planterUuid = UUID.randomUUID();
            var lower = level.getBlockState(source); var upper = level.getBlockState(source.above());
            var tool = new ItemStack(ModItems.TROWEL.get());
            tool.set(DataComponents.CUSTOM_NAME, Component.literal("My trowel"));
            helper.assertTrue(use(level, source.above(), tool).consumesAction(), "Trowel pickup failed");
            helper.assertTrue(level.getBlockState(source).isAir() && level.getBlockState(source.above()).isAir()
                    && manager.getState(level, source) == null, "Pickup left old native registration");
            var held = tool.copy(); level.setBlock(target.above(), Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(use(level, target.below(), tool) == InteractionResult.FAIL
                    && level.getBlockState(target).isAir() && ItemStack.isSameItemSameComponents(tool, held), "Blocked destination lost tool state or changed world");
            level.setBlock(target.above(), Blocks.AIR.defaultBlockState(), 3);
            var soil = level.getBlockState(target.below());
            level.setBlock(target.below(), ModBlocks.GARDEN_POT.get().defaultBlockState(), 3);
            helper.assertTrue(use(level, target.below(), tool) == InteractionResult.FAIL && level.getBlockState(target).isAir()
                    && ItemStack.isSameItemSameComponents(tool, held), "Unsupported pot changed world/tool");
            level.setBlock(target.below(), soil, 3);
            java.util.function.Consumer<BlockEvent.EntityPlaceEvent> guard = event -> {
                if (event.getLevel() == level && event.getPos().equals(target)) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(guard);
            try {
                helper.assertTrue(use(level, target.below(), tool) == InteractionResult.FAIL && level.getBlockState(target).isAir()
                        && manager.getState(level, target) == null && ItemStack.isSameItemSameComponents(tool, held), "Canceled placement lost/duplicated native plant");
            } finally { NeoForge.EVENT_BUS.unregister(guard); }
            helper.assertTrue(use(level, target.below(), tool).consumesAction(), "Trowel replant failed");
            var moved = StarfieldCompat.planting(level, target).orElseThrow(); var after = manager.getState(level, target);
            helper.assertTrue(moved.plantingId().equals(first.plantingId()) && moved.genome().equals(first.genome())
                    && moved.sticks().equals("obsidian") && level.getBlockState(target) == lower && level.getBlockState(target.above()) == upper,
                    "Trowel lost identity/genome/geometry/sticks");
            helper.assertTrue(after.phase == growth.phase && after.dayInPhase == growth.dayInPhase && after.regrowing == growth.regrowing
                    && after.infertileProgress == growth.infertileProgress && after.lastDailyDay == growth.lastDailyDay
                    && after.lastDailyWatered == growth.lastDailyWatered && after.lastGiantDay == growth.lastGiantDay
                    && after.sourcePhases == growth.sourcePhases && after.sourcePhaseVersion == growth.sourcePhaseVersion
                    && after.planterUuid.equals(growth.planterUuid), "Trowel lost native growth fields");
            manager.settleCrops(level, List.of(GlobalPos.of(level.dimension(), target)), 7, 1, false);
            helper.assertTrue(after.phase == growth.phase && after.dayInPhase == growth.dayInPhase, "Transfer replayed the same day");
            helper.assertTrue(!tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(StarfieldGenomes.TRANSFER_KEY)
                    && tool.get(DataComponents.CUSTOM_NAME).getString().equals("My trowel"), "Tool lost foreign data or kept a duplicate plant");
            helper.succeed();
        } finally { cleanup(level, source); cleanup(level, target); }
    }

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void savedDataPreservesUnknownVersionsAndRecoversLegacyPlants(GameTestHelper helper) {
        var level = valley(helper); var root = root(level, 64); prepare(level, root);
        var storage = level.getServer().overworld().getDataStorage(); var original = StarfieldPlantData.get(level);
        try {
            level.setBlock(root, ModBlocks.POTATO_CROP.get().defaultBlockState(), 3);
            var first = StarfieldCompat.planting(level, root).orElseThrow();
            var saved = original.save(new CompoundTag(), level.registryAccess());
            var restored = StarfieldPlantData.load(saved, level.registryAccess());
            helper.assertTrue(restored.get(GlobalPos.of(level.dimension(), root)).orElseThrow().encode().equals(first.encode()), "Production sidecar round-trip lost data");
            var future = saved.copy(); future.putInt("schema", 99); future.putString("future:field", "preserve");
            var unsupported = StarfieldPlantData.load(future, level.registryAccess());
            helper.assertTrue(!unsupported.writable() && unsupported.save(new CompoundTag(), level.registryAccess()).equals(future), "Unknown schema was overwritten");
            var broken = saved.copy(); var rows = broken.getList("plants", 10); var bad = first.encode();
            bad.remove("plantingId"); bad.putString("dimension", level.dimension().location().toString()); bad.putLong("root", root.offset(1, 0, 0).asLong()); rows.add(bad);
            var recovered = StarfieldPlantData.load(broken, level.registryAccess());
            helper.assertTrue(recovered.save(new CompoundTag(), level.registryAccess()).getList("plants", 10).contains(bad), "Malformed entry was silently deleted");
            storage.set(StarfieldPlantData.STORAGE, recovered);
            var damagedRoot = root.offset(1, 0, 0); prepare(level, damagedRoot);
            level.setBlock(damagedRoot, ModBlocks.POTATO_CROP.get().defaultBlockState(), 3);
            helper.assertTrue(StarfieldCompat.planting(level, damagedRoot).isEmpty()
                    && level.getBlockState(damagedRoot).is(ModBlocks.POTATO_CROP.get()), "Malformed entry was replaced with a new baseline or destroyed the crop");
            cleanup(level, damagedRoot);
            helper.assertTrue(!recovered.save(new CompoundTag(), level.registryAccess()).getList("plants", 10).contains(bad), "Explicit removal retained a blocked stale entry");
            var empty = new CompoundTag(); empty.putInt("schema", 1);
            storage.set(StarfieldPlantData.STORAGE, StarfieldPlantData.load(empty, level.registryAccess()));
            var manager = CropGrowthManager.get(level); var growth = manager.getState(level, root); growth.phase = 2; growth.dayInPhase = 1; growth.lastDailyDay = 17;
            var block = level.getBlockState(root); var legacy = StarfieldCompat.planting(level, root).orElseThrow();
            helper.assertTrue(legacy.genome().equals(StarfieldGenomes.baseline(POTATO)) && legacy.plantingId().equals(StarfieldCompat.planting(level, root).orElseThrow().plantingId())
                    && level.getBlockState(root) == block && growth.phase == 2 && growth.dayInPhase == 1 && growth.lastDailyDay == 17, "Legacy migration rolled genes/reset native state");
            helper.succeed();
        } finally { storage.set(StarfieldPlantData.STORAGE, original); cleanup(level, root); }
    }

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void payloadAndClientCacheIsolateWorldDimensionAndRoot(GameTestHelper helper) {
        var type = POTATO; var entry = new StarfieldPlantData.Entry(UUID.randomUUID(), type, varied(type), new CompoundTag(), "iron");
        var dimension = ResourceLocation.parse("stardewcraft:stardew_valley"); var root = new BlockPos(32, 80, 16); var session = UUID.randomUUID();
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var payload = new StarfieldPlantSync.Payload(session, dimension, root, false, entry.encode());
            StarfieldPlantSync.Payload.CODEC.encode(buf, payload); var decoded = StarfieldPlantSync.Payload.CODEC.decode(buf);
            helper.assertTrue(decoded.session().equals(session) && decoded.root().equals(root) && decoded.entry().equals(entry.encode()), "Network codec lost genome/identity");
            var cache = new StarfieldPlantCache(); var world = new Object(); cache.bind(world, dimension);
            cache.apply(session, dimension, root, Optional.of(StarfieldPlantData.Entry.decode(decoded.entry())));
            helper.assertTrue(cache.get(root).orElseThrow().plantingId().equals(entry.plantingId()), "Client cache lost UUID");
            cache.apply(session, ResourceLocation.parse("minecraft:overworld"), root, Optional.empty());
            helper.assertTrue(cache.get(root).isPresent(), "Foreign dimension packet removed this plant");
            cache.forget(new net.minecraft.world.level.ChunkPos(root)); helper.assertTrue(cache.get(root).isEmpty(), "Chunk unload retained cache");
            cache.apply(session, dimension, root, Optional.of(entry)); cache.bind(new Object(), dimension);
            helper.assertTrue(cache.get(root).isEmpty(), "Next world inherited a stale root");
            cache.apply(session, dimension, root, Optional.of(entry)); cache.bind(world, ResourceLocation.parse("minecraft:overworld"));
            helper.assertTrue(cache.get(root).isEmpty(), "Next dimension inherited a stale root");
            cache.clear(); helper.assertTrue(cache.get(root).isEmpty(), "Logout retained client cache");
            helper.succeed();
        } finally { buf.release(); }
    }

    @GameTest(batch = "starfield_genetics", templateNamespace = "agricraft", template = "starfield_probe")
    public static void fullRestartKeepsProductionGenomeAndNativeDay(GameTestHelper helper) throws Exception {
        String phase = System.getProperty("agricraft.starfield.restartPhase", "");
        if (phase.isEmpty()) { helper.succeed(); return; }
        var level = valley(helper); var root = root(level, 96); var witnessPath = Path.of(System.getProperty("agricraft.starfield.restartWitness"));
        var manager = CropGrowthManager.get(level);
        if (phase.equals("write")) {
            cleanup(level, root); prepare(level, root);
            int season = StardewTimeManager.get().getCurrentSeason(); StardewTimeManager.get().setCurrentSeason(0);
            var seed = seed(POTATO); StarfieldGenomes.writeSeed(seed, POTATO, varied(POTATO));
            helper.assertTrue(use(level, root.below(), seed).consumesAction(), "Restart planting failed");
            StardewTimeManager.get().setCurrentSeason(season);
            StarfieldCompat.setSticks(level, root, "wooden");
            manager.settleCrops(level, List.of(GlobalPos.of(level.dimension(), root)), 1, 0, false);
            var entry = StarfieldCompat.planting(level, root).orElseThrow(); var growth = manager.getState(level, root);
            var witness = entry.encode(); witness.putInt("phase", growth.phase); witness.putInt("progress", growth.dayInPhase);
            witness.putInt("lastDay", growth.lastDailyDay); witness.putString("block", level.getBlockState(root).toString());
            Files.createDirectories(witnessPath.getParent()); NbtIo.writeCompressed(witness, witnessPath);
            // Leave the real plant and both production SavedData files to normal server shutdown.
        } else {
            level.getChunk(root);
            var witness = NbtIo.readCompressed(witnessPath, NbtAccounter.unlimitedHeap());
            var entry = StarfieldCompat.planting(level, root).orElseThrow(); var growth = manager.getState(level, root);
            helper.assertTrue(entry.encode().equals(StarfieldPlantData.Entry.decode(witness).encode())
                    && level.getBlockState(root).toString().equals(witness.getString("block")), "Real restart changed genome/UUID/seed/sticks/block");
            helper.assertTrue(growth.phase == witness.getInt("phase") && growth.dayInPhase == witness.getInt("progress")
                    && growth.lastDailyDay == witness.getInt("lastDay"), "Real restart reset native phase/date");
            manager.settleCrops(level, List.of(GlobalPos.of(level.dimension(), root)), 1, 0, false);
            helper.assertTrue(growth.phase == witness.getInt("phase") && growth.dayInPhase == witness.getInt("progress"), "Restart replayed the same day");
            manager.settleCrops(level, List.of(GlobalPos.of(level.dimension(), root)), 2, 0, false);
            helper.assertTrue(growth.lastDailyDay == 2, "Next day did not execute after restart");
            cleanup(level, root);
        }
        helper.succeed();
    }

    private static ServerLevel valley(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        if (level == null) throw new IllegalStateException("Native dimension missing"); return level;
    }
    private static BlockPos root(ServerLevel level, int offset) {
        var owner = FakePlayerFactory.getMinecraft(level).getUUID();
        var farms = FarmInstanceRegistry.get(level.getServer());
        var farm = farms.getFarm(owner);
        if (farm == null) farm = farms.createFarm(owner, "AgriCraft genotype probe", "Genetics probe", FarmType.STANDARD);
        int index = offset / 16;
        return farm.getOrigin().offset(40 + index % 4 * 8, 40, 40 + index / 4 * 8);
    }
    private static ItemStack seed(ResourceLocation type) { return new ItemStack(BuiltInRegistries.ITEM.get(StarfieldCompat.species(type).orElseThrow().seed())); }
    private static CompoundTag varied(ResourceLocation type) {
        var genome = StarfieldGenomes.baseline(type); var genes = genome.getCompound("genes");
        for (var stat : AgriStatRegistry.getInstance()) {
            genes.getCompound(stat.getId()).putInt("dom", stat.getMax());
            genes.getCompound(stat.getId()).putInt("rec", stat.getMin());
        }
        return genome;
    }
    private static UseOnContext context(ServerLevel level, BlockPos position, ItemStack stack) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 2.5);
        player.getAbilities().mayBuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
    }
    private static InteractionResult use(ServerLevel level, BlockPos position, ItemStack stack) {
        return stack.useOn(context(level, position, stack));
    }
    private static void prepare(ServerLevel level, BlockPos root) {
        level.getChunk(root); level.setBlock(root.below(), ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
        level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 3); level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
    }
    private static void cleanup(ServerLevel level, BlockPos root) {
        level.getChunk(root); level.setBlock(root, Blocks.AIR.defaultBlockState(), 3); level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(root.below(), Blocks.AIR.defaultBlockState(), 3);
    }
}
