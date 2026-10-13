package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropState;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.agricraft.agricraft.compat.starfield.StarfieldGenomes;
import com.stardew.craft.block.FertilizerType;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstance;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.farm.FarmDailyProcessHelper;
import com.stardew.craft.farm.OfflineFarmCatchUp;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

/** Real native blocks and production sidecars. No addon runtime/phase executor is registered. */
@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldDailyGameTests {
    private static final ResourceLocation POTATO = ResourceLocation.parse("stardewcraft:potato_crop");
    private static final String[] VARIANTS = {"wooden", "iron", "obsidian"};

    @GameTestGenerator
    public static Collection<TestFunction> normalDailyEntryPoint() {
        // growDaily intentionally visits all eligible native roots. Keep that global pass out
        // of the restart pair: it would legitimately advance another test's saved checkpoint.
        if (!System.getProperty("agricraft.starfield.restartPhase", "").isEmpty()) return List.of();
        return List.of(new TestFunction("starfield_daily", "nativedailyentrypointconsumeswaterandkeepsretention",
                "agricraft:starfield_probe", 100, 0, true,
                StarfieldDailyGameTests::nativeDailyEntryPointConsumesWaterAndKeepsRetention));
    }

    private static void nativeDailyEntryPointConsumesWaterAndKeepsRetention(GameTestHelper helper) {
        var level = valley(helper); var pair = pair(level, "daily-entry", 0); var farm = farm(level, "daily-entry");
        var date = Date.capture(); var clock = StardewTimeManager.get(); var manager = CropGrowthManager.get(level);
        try {
            plantPair(helper, pair, nativeSoil(7), "wooden"); farm.markInitialized(); farm.setGraceDaysLeft(0);
            clock.setCurrentYear(1); clock.setCurrentSeason(0); clock.setCurrentDay(2); farm.markActiveOnDay(1);
            FarmDailyProcessHelper.beginDailyProcess(level);
            try { manager.growDaily(level); } finally { FarmDailyProcessHelper.endDailyProcess(level); }
            equal(helper, pair);
            helper.assertTrue(growth(pair, pair.sticks).lastDailyDay == 2 && growth(pair, pair.sticks).phase == 1,
                    "Normal daily scheduler did not execute the attached root");
            for (var root : pair.roots()) helper.assertTrue(level.getBlockState(root.below()).getValue(FarmBlock.MOISTURE) == 0,
                    "Native daily settlement did not consume soil water");
            var once = snapshot(level, pair.sticks);
            clock.setCurrentDay(3); farm.markActiveOnDay(2);
            FarmDailyProcessHelper.beginDailyProcess(level);
            try { manager.growDaily(level); } finally { FarmDailyProcessHelper.endDailyProcess(level); }
            equal(helper, pair);
            helper.assertTrue(growth(pair, pair.sticks).phase == once.getInt("phase")
                    && growth(pair, pair.sticks).dayInPhase == once.getInt("progress")
                    && !growth(pair, pair.sticks).lastDailyWatered, "Unwatered normal day advanced growth");
            for (var root : pair.roots()) helper.assertTrue(FertilizerManager.get(level).tryApplyFertilizer(level, root.below(), FertilizerType.DELUXE_RETAINING_SOIL),
                    "Native retaining soil could not be applied");
            water(pair, 7); clock.setCurrentDay(4); farm.markActiveOnDay(3);
            FarmDailyProcessHelper.beginDailyProcess(level);
            try { manager.growDaily(level); } finally { FarmDailyProcessHelper.endDailyProcess(level); }
            equal(helper, pair);
            for (var root : pair.roots()) helper.assertTrue(level.getBlockState(root.below()).getValue(FarmBlock.MOISTURE) == 7,
                    "Stick attachment bypassed native water retention");
            helper.succeed();
        } finally { date.restore(); cleanup(pair); }
    }

    @GameTest(batch = "starfield_daily", templateNamespace = "agricraft", template = "starfield_probe")
    public static void baselinePairsMatureTogetherAndPauseWithoutWater(GameTestHelper helper) {
        var level = valley(helper); var pair = pair(level, "watering", 0);
        try {
            plantPair(helper, pair, nativeSoil(7), "wooden");
            for (int day = 1; day <= 3; day++) {
                water(pair, 0); settle(pair, day, 0, false);
                equal(helper, pair);
                helper.assertTrue(growth(pair, pair.plain).phase == 0 && growth(pair, pair.plain).dayInPhase == 0,
                        "A dry day advanced growth");
            }
            int expected = StarfieldCompat.species(POTATO).orElseThrow().nativePhaseDays().stream().mapToInt(Integer::intValue).sum();
            int matureOn = -1;
            for (int wetDay = 1; wetDay <= expected + 1; wetDay++) {
                water(pair, 7); settle(pair, 3 + wetDay, 0, false); equal(helper, pair);
                if (mature(pair.level, pair.plain)) { matureOn = wetDay; break; }
            }
            helper.assertTrue(matureOn == expected, "Baseline changed native potato duration: " + matureOn + " != " + expected);
            helper.assertTrue(StarfieldCompat.planting(level, pair.sticks).orElseThrow().sticks().equals("wooden"), "Maturity lost the stick attachment");
            helper.succeed();
        } finally { cleanup(pair); }
    }

    @GameTest(batch = "starfield_daily", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeSoilRulesAndEveryStickVariantKeepTheSameSchedule(GameTestHelper helper) {
        var level = valley(helper);
        var soils = List.of(nativeSoil(7), com.stardew.craft.block.ModBlocks.SANDY_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7),
                com.stardew.craft.block.ModBlocks.INFERTILE_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        for (int i = 0; i < soils.size(); i++) {
            var pair = pair(level, "soils", i * 4);
            try {
                plantPair(helper, pair, soils.get(i), VARIANTS[i]);
                for (int day = 1; day <= 12; day++) { water(pair, 7); settle(pair, day, 0, false); equal(helper, pair); }
                helper.assertTrue(mature(level, pair.plain), "Native supported soil never matured potato: " + soils.get(i));
                helper.assertTrue(StarfieldCompat.planting(level, pair.sticks).orElseThrow().genome().equals(StarfieldGenomes.baseline(POTATO)), "Soil changed baseline genes");
            } finally { cleanup(pair); }
        }
        var unsupported = pair(level, "soils", 12);
        try {
            plantPair(helper, unsupported, nativeSoil(7), "iron");
            for (var root : unsupported.roots()) level.setBlock(root.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
            settle(unsupported, 1, 0, false); equal(helper, unsupported);
            for (var root : unsupported.roots()) helper.assertTrue(level.getBlockState(root).isAir()
                    && StarfieldCompat.planting(level, root).isEmpty(), "Unsupported vanilla soil bypassed native suitability/cleanup");
            helper.succeed();
        } finally { cleanup(unsupported); }
    }

    @GameTest(batch = "starfield_daily", templateNamespace = "agricraft", template = "starfield_probe")
    public static void historicalSeasonAndNativeGraceRuleApplyToBothRoots(GameTestHelper helper) {
        var level = valley(helper); var pair = pair(level, "season", 0); var farm = farm(level, "season");
        var clock = StardewTimeManager.get(); int season = clock.getCurrentSeason(); int grace = farm.getGraceDaysLeft();
        try {
            farm.setGraceDaysLeft(0);
            plantPair(helper, pair, nativeSoil(7), "obsidian");
            clock.setCurrentSeason(3); // Current winter must not replace the historical spring.
            settle(pair, 28, 0, true); equal(helper, pair);
            helper.assertTrue(level.getBlockState(pair.plain).is(com.stardew.craft.block.ModBlocks.POTATO_CROP.get())
                    && growth(pair, pair.plain).phase == 1, "Historical spring used the current winter");
            clock.setCurrentSeason(0);
            settle(pair, 29, 1, true); equal(helper, pair);
            helper.assertTrue(level.getBlockState(pair.plain).is(com.stardew.craft.block.ModBlocks.DEAD_CROP.get())
                    && StarfieldCompat.planting(level, pair.sticks).isEmpty(), "Historical summer ignored native seasonal death/cleanup");
            cleanup(pair); plantPair(helper, pair, nativeSoil(7), "obsidian");
            farm.setGraceDaysLeft(2);
            settle(pair, 29, 1, true); equal(helper, pair);
            helper.assertTrue(level.getBlockState(pair.plain).is(com.stardew.craft.block.ModBlocks.POTATO_CROP.get())
                    && growth(pair, pair.plain).phase == 1, "Native farm grace was lost on sticks");
            farm.setGraceDaysLeft(0);
            settle(pair, 30, 1, false); equal(helper, pair);
            helper.assertTrue(level.getBlockState(pair.sticks).is(com.stardew.craft.block.ModBlocks.DEAD_CROP.get()), "Expired grace kept sticks alive out of season");
            helper.succeed();
        } finally { clock.setCurrentSeason(season); farm.setGraceDaysLeft(grace); cleanup(pair); }
    }

    @GameTest(batch = "starfield_daily", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeSpeedGroKeepsIdenticalTimingWithoutExtraDailyGrowth(GameTestHelper helper) {
        var level = valley(helper); var fertilizer = FertilizerManager.get(level);
        var pair = pair(level, "fertilizer", 0);
        try {
            plantPair(helper, pair, nativeSoil(7), "iron");
            helper.assertTrue(fertilizer.tryApplyFertilizer(level, pair.plain.below(), FertilizerType.DELUXE_SPEED_GRO)
                    && fertilizer.tryApplyFertilizer(level, pair.sticks.below(), FertilizerType.DELUXE_SPEED_GRO), "Native fertilizer application failed");
            int matureOn = -1;
            for (int day = 1; day <= 10; day++) {
                water(pair, 7); settle(pair, day, 0, false); equal(helper, pair);
                var before = snapshot(level, pair.sticks);
                settle(pair, day, 0, false);
                helper.assertTrue(before.equals(snapshot(level, pair.sticks)), "Fertilizer/sticks replayed one date");
                if (mature(level, pair.plain)) { matureOn = day; break; }
            }
            helper.assertTrue(matureOn > 0 && matureOn < 6, "Native speed fertilizer had no effect: " + matureOn);
            helper.succeed();
        } finally { cleanup(pair); }
    }

    @GameTest(batch = "starfield_daily", templateNamespace = "agricraft", template = "starfield_probe")
    public static void randomTicksAndBoneMealCannotCreateCalendarDays(GameTestHelper helper) {
        var level = valley(helper); var pair = pair(level, "accelerators", 0);
        var empty = pair.plain.offset(4, 0, 0); var wheat = helper.absolutePos(new BlockPos(1, 2, 1));
        var overworld = helper.getLevel();
        try {
            plantPair(helper, pair, nativeSoil(7), "wooden");
            var before = snapshot(level, pair.sticks);
            for (var root : pair.roots()) {
                var state = level.getBlockState(root);
                helper.assertTrue(!state.isRandomlyTicking() && AgriApi.getCrop(level, root).isEmpty(), "Native root entered AgriCraft's random growth route");
                for (int tick = 0; tick < 200; tick++) state.randomTick(level, root, level.random);
                var boneMeal = new ItemStack(Items.BONE_MEAL, 5);
                helper.assertTrue(!BoneMealItem.growCrop(boneMeal, level, root) && boneMeal.getCount() == 5, "Bone meal bypassed native calendar/consumed item");
            }
            helper.assertTrue(before.equals(snapshot(level, pair.sticks)), "Random ticks changed phase/date");
            equal(helper, pair);
            level.setBlock(empty.below(), Blocks.FARMLAND.defaultBlockState(), 3);
            level.setBlock(empty, ModBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.DOUBLE_STICKS), 3);
            helper.assertTrue(level.getBlockState(empty).isRandomlyTicking() && AgriApi.getCrop(level, empty).isPresent(), "Empty double sticks lost normal tick processing");
            level.getBlockState(empty).randomTick(level, empty, level.random);
            overworld.setBlock(wheat.below(), Blocks.FARMLAND.defaultBlockState(), 3);
            overworld.setBlock(wheat, Blocks.WHEAT.defaultBlockState(), 3);
            helper.assertTrue(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), overworld, wheat), "Native compatibility disabled overworld vanilla bone meal");
            helper.succeed();
        } finally { cleanup(pair); level.removeBlock(empty, false); level.removeBlock(empty.below(), false); overworld.removeBlock(wheat, false); overworld.removeBlock(wheat.below(), false); }
    }

    @GameTest(batch = "starfield_daily_unload", templateNamespace = "agricraft", template = "starfield_probe", timeoutTicks = 600)
    public static void actualChunkUnloadAndNativeFarmCatchUpKeepDatesAndIdentity(GameTestHelper helper) {
        var level = valley(helper); var pair = pair(level, "unload", 0); var farm = farm(level, "unload");
        var clock = StardewTimeManager.get(); var date = Date.capture();
        plantPair(helper, pair, nativeSoil(7), "wooden");
        farm.markInitialized(); farm.setGraceDaysLeft(0); farm.setLastOnlineSeason(0);
        settle(pair, 1, 0, false);
        var plainEntry = StarfieldCompat.planting(level, pair.plain).orElseThrow().encode();
        var stickEntry = StarfieldCompat.planting(level, pair.sticks).orElseThrow().encode();
        var unloadedProgress = growthSnapshot(growth(pair, pair.sticks));
        // Neither root has a forced ticket. Let the actual server release its temporary load tickets.
        var chunk = new net.minecraft.world.level.ChunkPos(pair.plain);
        helper.assertTrue(!level.getForcedChunks().contains(chunk.toLong()), "Test root was force-loaded before unloading");
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(!level.hasChunkAt(pair.plain) && !level.hasChunkAt(pair.sticks), "Waiting for the real farm chunk to unload");
        }).thenExecute(() -> {
            try {
                helper.assertTrue(unloadedProgress.equals(growthSnapshot(growth(pair, pair.sticks))), "Unloaded crop grew without native farm catch-up");
                // Direct settlement must skip an unloaded root and must not force-load it.
                settle(pair, 2, 0, false);
                helper.assertTrue(!level.hasChunkAt(pair.sticks) && growth(pair, pair.sticks).lastDailyDay == 1, "Daily pass forced/advanced an unloaded root");
                clock.setCurrentYear(1); clock.setCurrentSeason(0); clock.setCurrentDay(2);
                farm.setLastOnlineDay(1); OfflineFarmCatchUp.catchUp(level, farm.getOwnerUUID());
                helper.assertTrue(growth(pair, pair.sticks).lastDailyDay == 2, "One missed native day was not caught up");
                equal(helper, pair);
                clock.setCurrentDay(5); OfflineFarmCatchUp.catchUp(level, farm.getOwnerUUID());
                equal(helper, pair);
                helper.assertTrue(growth(pair, pair.sticks).lastDailyDay == 5 && farm.getLastOnlineDay() == 5, "Several missed days did not settle individually");
                helper.assertTrue(growth(pair, pair.sticks).phase == 4 && growth(pair, pair.sticks).dayInPhase == 0,
                        "Native catch-up did not apply exactly five potato growth days");
                helper.assertTrue(plainEntry.equals(StarfieldCompat.planting(level, pair.plain).orElseThrow().encode())
                        && stickEntry.equals(StarfieldCompat.planting(level, pair.sticks).orElseThrow().encode()), "Chunk reload changed planting identity/genome/sticks");
                var once = snapshot(level, pair.sticks);
                OfflineFarmCatchUp.catchUp(level, farm.getOwnerUUID()); settle(pair, 5, 0, false);
                helper.assertTrue(once.equals(snapshot(level, pair.sticks)), "Catch-up/reload replayed processed days");
            } finally { date.restore(); cleanup(pair); }
        }).thenSucceed();
    }

    @GameTest(batch = "starfield_daily_restart", templateNamespace = "agricraft", template = "starfield_probe")
    public static void fullJvmRestartDoesNotRepeatThePairedDay(GameTestHelper helper) throws Exception {
        String phase = System.getProperty("agricraft.starfield.restartPhase", "");
        if (phase.isEmpty()) { helper.succeed(); return; }
        var level = valley(helper); var pair = pair(level, "daily-restart", 0);
        var path = Path.of(System.getProperty("agricraft.starfield.dailyRestartWitness"));
        if (phase.equals("write")) {
            plantPair(helper, pair, nativeSoil(7), "obsidian");
            settle(pair, 1, 0, false); equal(helper, pair);
            var witness = new CompoundTag();
            for (int i = 0; i < pair.roots().size(); i++) {
                var root = pair.roots().get(i); var row = snapshot(level, root);
                row.put("entry", StarfieldCompat.planting(level, root).orElseThrow().encode()); witness.put("root" + i, row);
            }
            Files.createDirectories(path.getParent()); NbtIo.writeCompressed(witness, path);
            // Real world files, native SavedData and production sidecar are saved by server shutdown.
        } else {
            level.getChunk(pair.plain); level.getChunk(pair.sticks);
            var witness = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            try {
                for (int i = 0; i < pair.roots().size(); i++) {
                    var root = pair.roots().get(i); var expected = witness.getCompound("root" + i); var state = expected.copy(); state.remove("entry");
                    helper.assertTrue(snapshot(level, root).equals(state)
                            && StarfieldCompat.planting(level, root).orElseThrow().encode().equals(expected.getCompound("entry")), "Restart changed real native/sidecar files");
                }
                var before = snapshot(level, pair.sticks); settle(pair, 1, 0, false);
                helper.assertTrue(before.equals(snapshot(level, pair.sticks)), "JVM restart repeated the already processed day");
                settle(pair, 2, 0, false); equal(helper, pair);
                helper.assertTrue(growth(pair, pair.sticks).lastDailyDay == 2 && !before.equals(snapshot(level, pair.sticks)), "Next day did not advance once after restart");
            } finally { cleanup(pair); }
        }
        helper.succeed();
    }

    private record Pair(ServerLevel level, BlockPos plain, BlockPos sticks) {
        List<BlockPos> roots() { return List.of(plain, sticks); }
    }
    private record Date(int year, int season, int day) {
        static Date capture() { var clock = StardewTimeManager.get(); return new Date(clock.getCurrentYear(), clock.getCurrentSeason(), clock.getCurrentDay()); }
        void restore() { var clock = StardewTimeManager.get(); clock.setCurrentYear(year); clock.setCurrentSeason(season); clock.setCurrentDay(day); }
    }
    private static ServerLevel valley(GameTestHelper helper) { return helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY); }
    private static FarmInstance farm(ServerLevel level, String test) {
        var owner = UUID.nameUUIDFromBytes(("agricraft:daily:" + test).getBytes(StandardCharsets.UTF_8));
        var registry = FarmInstanceRegistry.get(level.getServer()); var farm = registry.getFarm(owner);
        return farm == null ? registry.createFarm(owner, "AgriCraft daily " + test, "Daily probe", FarmType.STANDARD) : farm;
    }
    private static Pair pair(ServerLevel level, String test, int offset) {
        var root = farm(level, test).getOrigin().offset(40 + offset, 10, 40);
        return new Pair(level, root, root.east());
    }
    private static BlockState nativeSoil(int moisture) { return com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, moisture); }
    private static void plantPair(GameTestHelper helper, Pair pair, BlockState soil, String variant) {
        for (var root : pair.roots()) {
            pair.level.getChunk(root); pair.level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
            pair.level.setBlock(root.below(), soil, 3);
            pair.level.setBlock(root, com.stardew.craft.block.ModBlocks.POTATO_CROP.get().defaultBlockState(), 3);
            var entry = StarfieldCompat.planting(pair.level, root).orElseThrow(() -> new IllegalStateException("Missing planting on " + soil + " at " + root + ": " + pair.level.getBlockState(root)));
            helper.assertTrue(entry.genome().equals(StarfieldGenomes.baseline(POTATO)), "Pair did not start with the baseline genome");
        }
        helper.assertTrue(StarfieldCompat.setSticks(pair.level, pair.sticks, variant), "Could not attach production sticks");
        helper.assertTrue(StarfieldCompat.planting(pair.level, pair.plain).orElseThrow().sticks().isEmpty(), "Plain planting unexpectedly has sticks");
    }
    private static void water(Pair pair, int moisture) {
        for (var root : pair.roots()) pair.level.setBlock(root.below(), pair.level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE, moisture), 3);
    }
    private static void settle(Pair pair, int day, int season, boolean offline) {
        CropGrowthManager.get(pair.level).settleCrops(pair.level, pair.roots().stream().map(root -> GlobalPos.of(pair.level.dimension(), root)).toList(), day, season, offline);
    }
    private static CropGrowthManager.CropGrowthState growth(Pair pair, BlockPos root) { return CropGrowthManager.get(pair.level).getState(pair.level, root); }
    private static boolean mature(ServerLevel level, BlockPos root) { return StarfieldCompat.inspect(level, root).orElseThrow().mature(); }
    private static CompoundTag growthSnapshot(CropGrowthManager.CropGrowthState growth) {
        var tag = new CompoundTag();
        if (growth == null) return tag;
        tag.putInt("phase", growth.phase); tag.putInt("progress", growth.dayInPhase); tag.putInt("soilProgress", growth.infertileProgress);
        tag.putBoolean("regrowing", growth.regrowing); tag.putInt("lastDay", growth.lastDailyDay); tag.putBoolean("watered", growth.lastDailyWatered);
        tag.putInt("lastGiant", growth.lastGiantDay); tag.putBoolean("sourcePhases", growth.sourcePhases); tag.putInt("sourceVersion", growth.sourcePhaseVersion);
        return tag;
    }
    private static CompoundTag snapshot(ServerLevel level, BlockPos root) {
        var tag = growthSnapshot(CropGrowthManager.get(level).getState(level, root));
        // Dead-crop variants are native random decoration, not a calendar difference.
        var state = level.getBlockState(root);
        if (!state.is(com.stardew.craft.block.ModBlocks.DEAD_CROP.get())) tag.putString("block", state.toString());
        else tag.putString("block", "stardewcraft:dead_crop");
        return tag;
    }
    private static void equal(GameTestHelper helper, Pair pair) {
        helper.assertTrue(snapshot(pair.level, pair.plain).equals(snapshot(pair.level, pair.sticks)),
                "Plain/sticks diverged: " + snapshot(pair.level, pair.plain) + " / " + snapshot(pair.level, pair.sticks));
    }
    private static void cleanup(Pair pair) {
        for (var root : pair.roots()) {
            pair.level.getChunk(root); pair.level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
            FertilizerManager.get(pair.level).removeFertilizer(pair.level, root.below());
            pair.level.setBlock(root.below(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
