package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.compat.starfield.*;
import com.stardew.craft.block.FertilizerType;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldGeneGameTests {
    static void stat(ServerLevel level, BlockPos pos, String id, int value) {
        StarfieldGeneTestData.stat(level, pos, id, value);
    }

    @GameTest(batch="starfield_genes", templateNamespace="agricraft", template="starfield_probe")
    public static void growthTableAndCustomBoundsKeepServicePhases(GameTestHelper helper) {
        // Fixed examples from the documented balance table, including extreme/custom ranges.
        int[] expected = {10, 10, 9, 9, 8, 8, 7, 7, 6, 5};
        for (int i = 1; i <= 10; i++) helper.assertTrue(NativeCropGeneRules.duration(10, NativeCropGeneRules.normalized(i, 1, 10)) == expected[i-1], "Growth table differs at " + i);
        int[] yields = {10,11,12,13,14,15,16,17,18,20};
        for (int i = 1; i <= 10; i++) helper.assertTrue(NativeCropGeneRules.yield(10, NativeCropGeneRules.normalized(i, 1, 10)) == yields[i-1], "Gain table differs at " + i);
        helper.assertTrue(NativeCropGeneRules.normalized(12, 2, 22) == 0.5
                && NativeCropGeneRules.normalized(8, 8, 8) == 0
                && NativeCropGeneRules.normalized(Integer.MIN_VALUE, 1, 10) == 0
                && NativeCropGeneRules.normalized(Integer.MAX_VALUE, 1, 10) == 1, "Bounds/equal range not normalized");
        helper.assertTrue(Arrays.equals(NativeCropGeneRules.phases(new int[]{0, 1, 2, 99999}, 1), new int[]{0, 1, 1, 99999}), "Native service phases changed");
        helper.succeed();
    }

    @GameTest(batch="starfield_genes", templateNamespace="agricraft", template="starfield_probe")
    public static void maximumGrowthUsesCalendarAndInspectionWithoutReplay(GameTestHelper helper) {
        try (var plot = new Plot(helper, "duration")) {
            plot.plant("cauliflower", 0);
            for (var root : plot.roots()) stat(plot.level, root, "growth", AgriStatRegistry.getInstance().growthStat().getMax());
            var identity = StarfieldCompat.planting(plot.level, plot.attached).orElseThrow();
            helper.assertTrue(StarfieldCompat.inspection(plot.level, plot.attached).orElseThrow().getInt("remaining") == 7, "Inspection ignored genetic schedule");
            plot.day(1, 0, false);
            helper.assertTrue(CropGrowthManager.get(plot.level).getState(plot.level, plot.attached).phase == 0, "Dry high-gene crop advanced");
            for (int wet = 1; wet <= 7; wet++) {
                plot.day(wet+1, 0, true); plot.equal(helper);
                helper.assertTrue(StarfieldCompat.inspect(plot.level, plot.attached).orElseThrow().mature() == (wet == 7), "Cauliflower did not mature in 7 watered days");
                var before = CropGrowthManager.get(plot.level).getState(plot.level, plot.attached).dayInPhase;
                plot.day(wet+1, 0, true);
                helper.assertTrue(before == CropGrowthManager.get(plot.level).getState(plot.level, plot.attached).dayInPhase, "Same date advanced twice");
                var saved = StarfieldPlantData.get(plot.level).save(new CompoundTag(), plot.level.registryAccess());
                plot.level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE, StarfieldPlantData.load(saved, plot.level.registryAccess()));
            }
            helper.assertTrue(identity.equals(StarfieldCompat.planting(plot.level, plot.attached).orElseThrow()), "Schedule/sidecar reload changed genes or identity");
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_genes", templateNamespace="agricraft", template="starfield_probe")
    public static void fertilizerCombinesOnceAndDisabledModeKeepsNativeDays(GameTestHelper helper) {
        try (var plot = new Plot(helper, "fertilizer")) {
            plot.plant("cauliflower", 0);
            for (var root : plot.roots()) {
                stat(plot.level, root, "growth", AgriStatRegistry.getInstance().growthStat().getMax());
                helper.assertTrue(FertilizerManager.get(plot.level).tryApplyFertilizer(plot.level, root.below(), FertilizerType.DELUXE_SPEED_GRO), "Fertilizer rejected");
            }
            // Native [1,2,4,4,1] -> Deluxe [1,1,3,3,1] -> genetic [1,1,2,2,1].
            for (int day = 1; day <= 7; day++) { plot.day(day, 0, true); plot.equal(helper);
                helper.assertTrue(StarfieldCompat.inspect(plot.level, plot.plain).orElseThrow().mature() == (day==7), "Fertilizer/gene schedule changed"); }
            plot.plant("cauliflower", 0);
            for (var root : plot.roots()) stat(plot.level, root, "growth", AgriStatRegistry.getInstance().growthStat().getMax());
            CompatConfig.enableStarfield = false;
            for (int day = 1; day <= 12; day++) { plot.day(day, 0, true);
                helper.assertTrue(plot.level.getBlockState(plot.plain).getValue(StardewCropBlock.AGE) == StardewCropBlock.MAX_AGE == (day == 12), "Disabled mode changed native duration"); }
            helper.succeed();
        } finally { CompatConfig.enableStarfield = true; }
    }

    @GameTest(batch="starfield_genes", templateNamespace="agricraft", template="starfield_probe")
    public static void allFiftySpeciesUseBaselineAndGeneticSchedules(GameTestHelper helper) {
        try (var plot = new Plot(helper, "all-species")) {
            int count = 0;
            for (var species : StarfieldCompat.species()) {
                plot.plant(species.id().getPath().replace("_crop", ""), 0);
                var block = (StardewCropBlock) BuiltInRegistries.BLOCK.get(species.id());
                int season = 0;
                for (; season < 4; season++) { StardewTimeManager.get().setCurrentSeason(season); if (block.canPlantAt(plot.level, plot.plain)) break; }
                helper.assertTrue(season < 4, "No native season for " + species.id());
                stat(plot.level, plot.attached, "growth", AgriStatRegistry.getInstance().growthStat().getMax());
                int nativeDays = species.nativePhaseDays().stream().mapToInt(Integer::intValue).sum();
                int geneticDays = species.nativePhaseDays().stream().mapToInt(days -> days <= 0 ? days : Math.max(1, (days+1)/2)).sum();
                for (int day = 1; day <= nativeDays; day++) {
                    plot.day(day, season, true);
                    helper.assertTrue(ready(plot.level, plot.plain, species) == (day >= nativeDays), "Baseline duration changed for " + species.id()+" day="+day);
                    helper.assertTrue(ready(plot.level, plot.attached, species) == (day >= geneticDays), "Genetic duration changed for " + species.id()+" day="+day);
                }
                count++;
            }
            helper.assertTrue(count == 50, "Incomplete crop coverage"); helper.succeed();
        }
    }

    @GameTest(batch="starfield_gene_restart", templateNamespace="agricraft", template="starfield_probe")
    public static void realJvmRestartKeepsGeneticPhasesAndRegrowCountdown(GameTestHelper helper) throws Exception {
        String phase = System.getProperty("agricraft.starfield.restartPhase", "");
        if (phase.isEmpty()) { helper.succeed(); return; }
        var growing = new Plot(helper, "restart-growing"); var regrowing = new Plot(helper, "restart-regrowing");
        var witness = java.nio.file.Path.of(System.getProperty("agricraft.starfield.dailyRestartWitness")).resolveSibling("iteration8-restart-witness.nbt");
        try {
            if (phase.equals("write")) {
                growing.plant("cauliflower", 0); regrowing.plant("corn", 1);
                for (var plot : List.of(growing, regrowing)) for (var root : plot.roots()) stat(plot.level, root, "growth", AgriStatRegistry.getInstance().growthStat().getMax());
                growing.day(1, 0, true); growing.day(2, 0, true);
                for (int day = 1; day <= 9; day++) regrowing.day(day, 1, true);
                for (var root : regrowing.roots()) {
                    var state = regrowing.level.getBlockState(root);
                    var crop = (StardewCropBlock) state.getBlock();
                    helper.assertTrue(!crop.tryHarvestByJunimo(regrowing.level, root, state, 0, stack -> {}).isEmpty(), "Native regrow checkpoint harvest failed");
                    helper.assertTrue(CropGrowthManager.get(regrowing.level).getState(regrowing.level, root).dayInPhase == 2, "Checkpoint does not contain genetic countdown");
                }
                var saved = new CompoundTag();
                int index = 0;
                for (var plot : List.of(growing, regrowing)) for (var root : plot.roots()) saved.put("root"+index++, checkpoint(plot.level, root));
                NbtIo.writeCompressed(saved, witness);
                // Leave real blocks and both SavedData instances for ordinary server shutdown.
            } else {
                var saved = NbtIo.readCompressed(witness, NbtAccounter.unlimitedHeap()); int index = 0;
                for (var plot : List.of(growing, regrowing)) for (var root : plot.roots())
                    helper.assertTrue(checkpoint(plot.level, root).equals(saved.getCompound("root"+index++)), "JVM restart changed native/genetic checkpoint");
                growing.day(2, 0, true); regrowing.day(9, 1, true); index = 0;
                for (var plot : List.of(growing, regrowing)) for (var root : plot.roots())
                    helper.assertTrue(checkpoint(plot.level, root).equals(saved.getCompound("root"+index++)), "Restart repeated processed day or reapplied growth bonus");
                regrowing.day(10, 1, true);
                helper.assertTrue(CropGrowthManager.get(regrowing.level).getState(regrowing.level, regrowing.attached).dayInPhase == 1, "Next regrow day did not subtract exactly one");
                growing.day(3, 0, true); growing.equal(helper); regrowing.equal(helper);
            }
            helper.succeed();
        } finally {
            if (!phase.equals("write")) { growing.close(); regrowing.close(); }
            else { StardewTimeManager.get().setCurrentSeason(growing.previousSeason); }
        }
    }

    private static CompoundTag checkpoint(ServerLevel level, BlockPos root) {
        var tag = new CompoundTag(); tag.putString("lower", level.getBlockState(root).toString()); tag.putString("upper", level.getBlockState(root.above()).toString());
        tag.put("entry", StarfieldCompat.planting(level, root).orElseThrow().encode());
        var nativeData = CropGrowthManager.get(level).save(new CompoundTag(), level.registryAccess());
        for (var value : nativeData.getList("Crops", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            if (row.getString("Dimension").equals(level.dimension().location().toString()) && NbtUtils.readBlockPos(row, "Pos").orElseThrow().equals(root)) { tag.put("native", row.copy()); return tag; }
        }
        throw new IllegalStateException("Missing native checkpoint");
    }

    private static boolean ready(ServerLevel level, BlockPos root, StarfieldCompat.Species species) {
        // Wild seeds intentionally replace their mature crop with native forage.
        if (species.seasonalForage()) return BuiltInRegistries.BLOCK.getKey(level.getBlockState(root).getBlock()).getPath().startsWith("forage_");
        return StarfieldCompat.inspect(level, root).orElseThrow(() -> new IllegalStateException("Missing native root " + species.id()+": "+level.getBlockState(root))).mature();
    }

    private static final class Plot implements AutoCloseable {
        final ServerLevel level; final BlockPos plain, attached; final int previousSeason;
        Plot(GameTestHelper helper, String name) {
            level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
            var uuid = UUID.nameUUIDFromBytes(("agricraft:genes:"+name).getBytes(StandardCharsets.UTF_8));
            var registry = FarmInstanceRegistry.get(level.getServer()); var farm = registry.getFarm(uuid);
            if (farm == null) farm = registry.createFarm(uuid, name, "Gene probe", FarmType.STANDARD);
            farm.markInitialized(); farm.setGraceDaysLeft(0);
            plain = farm.getOrigin().offset(72, 10, 72); attached = plain.east(); level.getChunk(plain); level.getChunk(attached);
            previousSeason = StardewTimeManager.get().getCurrentSeason();
        }
        List<BlockPos> roots() { return List.of(plain, attached); }
        void plant(String name, int season) {
            cleanup(); StardewTimeManager.get().setCurrentSeason(season);
            for (var root : roots()) {
                level.setBlock(root.below(), com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
                level.setBlock(root, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("stardewcraft:"+name+"_crop")).defaultBlockState(), 3);
                StarfieldCompat.planting(level, root).orElseThrow();
            }
            StarfieldCompat.setSticks(level, attached, "iron");
        }
        void day(int day, int season, boolean wet) {
            for (var root : roots()) level.setBlock(root.below(), level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE, wet ? 7 : 0), 3);
            CropGrowthManager.get(level).settleCrops(level, roots().stream().map(root -> GlobalPos.of(level.dimension(), root)).toList(), day, season, false);
        }
        void equal(GameTestHelper helper) {
            var a = CropGrowthManager.get(level).getState(level, plain); var b = CropGrowthManager.get(level).getState(level, attached);
            helper.assertTrue(a.phase==b.phase && a.dayInPhase==b.dayInPhase && a.lastDailyDay==b.lastDailyDay && level.getBlockState(plain).equals(level.getBlockState(attached)), "Plain/sticks schedules differ");
        }
        void cleanup() { for (var root : roots()) { level.setBlock(root, Blocks.AIR.defaultBlockState(), 3); level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 3); FertilizerManager.get(level).removeFertilizer(level, root.below()); level.setBlock(root.below(), Blocks.AIR.defaultBlockState(), 3); } }
        @Override public void close() { cleanup(); StardewTimeManager.get().setCurrentSeason(previousSeason); }
    }
}
