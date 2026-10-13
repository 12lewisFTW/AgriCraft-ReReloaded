package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.api.v1.internal.giant.GiantCropGrowth;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.nio.file.Path;
import java.io.IOException;

/** Compare unchanged native blocks, with and without independently persisted stick metadata. */
@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class NativeRepresentationGameTests {
    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void potatoNativeCycleAndDryDay(GameTestHelper helper) {
        compareCycle(helper, (StardewCropBlock) ModBlocks.POTATO_CROP.get(), 0, 6, 0, 0);
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void broccoliNativeCycleAndRegrowth(GameTestHelper helper) {
        compareCycle(helper, (StardewCropBlock) ModBlocks.BROCCOLI_CROP.get(), 2, 8, 4, 16);
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void cornNativeCycleUpperRootAndRegrowth(GameTestHelper helper) {
        compareCycle(helper, (StardewCropBlock) ModBlocks.CORN_CROP.get(), 1, 14, 4, 32);
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void cauliflowerNativeCycle(GameTestHelper helper) {
        compareCycle(helper, (StardewCropBlock) ModBlocks.CAULIFLOWER_CROP.get(), 0, 12, 0, 48);
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeGiantAcceptsSidecarPlants(GameTestHelper helper) {
        var level = nativeLevel(helper);
        var farms = FarmInstanceRegistry.get(level.getServer());
        var owner = UUID.randomUUID();
        var farm = farms.createFarm(owner, "AgriCraft probe", "Giant probe", FarmType.STANDARD);
        var anchor = farm.getOrigin().offset(40, 40, 40);
        var roots = new ArrayList<BlockPos>();
        var crop = (StardewCropBlock) ModBlocks.CAULIFLOWER_CROP.get();
        var giant = ModBlocks.GIANT_CAULIFLOWER.get();
        try {
            for (int z = 0; z < 3; z++) for (int x = 0; x < 3; x++) {
                var root = anchor.offset(x, 0, z);
                roots.add(root);
                place(level, root, crop, true);
                NativeStickAttachments.get(level).attach(level, root);
            }
            var manager = CropGrowthManager.get(level);
            var growth = manager.getOrCreateState(level, anchor);
            growth.phase = crop.getPhaseDaysForDisplay().length;
            level.setBlock(anchor, crop.defaultBlockState().setValue(StardewCropBlock.AGE, StardewCropBlock.MAX_AGE), 2);
            crop.syncVisualStage(level, anchor, growth);
            helper.assertTrue(StardewCropRuntime.inspect(level, anchor).mature(), "Giant trigger must be mature");
            // Select a winning native 1% roll without changing the chance or rerolling a date.
            int day = 100;
            while (day < 10000 && !GiantCropGrowth.wins(level.getSeed(), level.dimension().location(),
                    anchor, BuiltInRegistries.BLOCK.getKey(giant), day, .01)) day++;
            helper.assertTrue(day < 10000, "Could not find a deterministic winning native roll");
            var result = manager.settleCrops(level, roots.stream()
                    .map(p -> GlobalPos.of(level.dimension(), p)).toList(), day, 0, false);
            helper.assertTrue(result.grown() == 1, "Sidecar prevented a native giant: " + result);
            for (var root : roots) {
                helper.assertTrue(level.getBlockState(root).is(giant), "Native giant footprint incomplete");
                helper.assertTrue(!manager.getAllCropPositions().contains(GlobalPos.of(level.dimension(), root)),
                        "Consumed root remained scheduled");
                helper.assertTrue(NativeStickAttachments.get(level).inspect(level, root).isEmpty(),
                        "Consumed root still exposes an active planting");
            }
            helper.succeed();
        } finally {
            for (var root : roots) for (int y = 0; y < 3; y++)
                level.setBlock(root.above(y), Blocks.AIR.defaultBlockState(), 3);
            cleanup(level, roots);
            farms.deleteFarm(owner);
        }
    }

    /** Opt-in, two distinct JVM launches. The second reads real world SavedData and chunks. */
    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativePlantSurvivesServerRestart(GameTestHelper helper) throws IOException {
        var phase = System.getProperty("agricraft.starfield.restartPhase", "");
        if (phase.isEmpty()) { helper.succeed(); return; }
        var witness = Path.of(System.getProperty("agricraft.starfield.restartWitness"));
        var level = nativeLevel(helper);
        var root = new BlockPos(50128, 80, 50000);
        level.getChunk(root);
        var manager = CropGrowthManager.get(level);
        var store = NativeStickAttachments.get(level);
        if (phase.equals("write")) {
            cleanup(level, List.of(root));
            place(level, root, (StardewCropBlock) ModBlocks.POTATO_CROP.get(), true);
            var attachment = store.attach(level, root);
            settle(manager, level, List.of(root), 7, 0);
            var growth = manager.getState(level, root);
            var expected = new CompoundTag();
            expected.putUUID("plantingId", attachment.plantingId());
            expected.put("genome", attachment.genome());
            expected.putInt("phase", growth.phase);
            expected.putInt("progress", growth.dayInPhase);
            expected.putInt("lastDailyDay", growth.lastDailyDay);
            expected.putString("block", level.getBlockState(root).toString());
            NbtIo.writeCompressed(expected, witness);
            // Leave this planting for the normal server shutdown to save to disk.
        } else {
            var expected = NbtIo.readCompressed(witness, NbtAccounter.unlimitedHeap());
            var attachment = store.inspect(level, root).orElseThrow();
            var growth = manager.getState(level, root);
            helper.assertTrue(level.getBlockState(root).toString().equals(expected.getString("block"))
                            && attachment.plantingId().equals(expected.getUUID("plantingId"))
                            && attachment.genome().equals(expected.getCompound("genome"))
                            && growth.phase == expected.getInt("phase")
                            && growth.dayInPhase == expected.getInt("progress")
                            && growth.lastDailyDay == expected.getInt("lastDailyDay"),
                    "World state, identity, genotype or calendar changed across a JVM restart");
            settle(manager, level, List.of(root), 7, 0);
            helper.assertTrue(growth.phase == expected.getInt("phase")
                            && growth.dayInPhase == expected.getInt("progress"), "Restart replayed date 7");
            settle(manager, level, List.of(root), 8, 0);
            helper.assertTrue(growth.phase == expected.getInt("phase") + 1 && growth.lastDailyDay == 8,
                    "First new date after restart did not advance exactly once");
            cleanup(level, List.of(root));
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeSchedulerAndSidecarReload(GameTestHelper helper) {
        ServerLevel level = nativeLevel(helper);
        BlockPos root = new BlockPos(50064, 80, 50000);
        place(level, root, (StardewCropBlock) ModBlocks.POTATO_CROP.get(), true);
        var store = NativeStickAttachments.get(level);
        var attached = store.attach(level, root);
        var manager = CropGrowthManager.get(level);
        try {
            settle(manager, level, List.of(root), 1, 0);
            var growthBefore = manager.getState(level, root);
            int phase = growthBefore.phase, progress = growthBefore.dayInPhase;
            var restoredStore = NativeStickAttachments.load(store.save(new CompoundTag(), level.registryAccess()),
                    level.registryAccess());
            var restoredManager = CropGrowthManager.load(manager.save(new CompoundTag(), level.registryAccess()),
                    level.registryAccess());
            level.getServer().overworld().getDataStorage().set("stardew_crop_manager", restoredManager);
            level.getServer().overworld().getDataStorage().set(NativeStickAttachments.STORAGE, restoredStore);
            var after = restoredStore.inspect(level, root).orElseThrow();
            helper.assertTrue(attached.plantingId().equals(after.plantingId())
                            && attached.genome().equals(after.genome()), "Plant identity/genome changed on reload");
            var genome = AgriGenome.fromNBT(after.genome());
            helper.assertTrue(genome.getStatGenes().size() == 6, "Expected the six existing traits");
            for (var stat : AgriStatRegistry.getInstance())
                helper.assertTrue(genome.getStatGene(stat).getTrait() == stat.getMin(),
                        "Baseline must be the configured minimum: " + stat.getId());
            // The native executor itself retrieves the manager from SavedData.
            settle(restoredManager, level, List.of(root), 1, 0);
            var restoredGrowth = restoredManager.getState(level, root);
            helper.assertTrue(restoredGrowth.phase == phase && restoredGrowth.dayInPhase == progress,
                    "Reload replayed the same native date");
            settle(restoredManager, level, List.of(root), 2, 0);
            helper.assertTrue(restoredGrowth.lastDailyDay == 2 && restoredGrowth.phase == phase + 1,
                    "The next date must advance exactly once after reload");
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(restoredStore.inspect(level, root).isEmpty(), "Removed crop still exposes its attachment");
            helper.succeed();
        } finally {
            level.getServer().overworld().getDataStorage().set("stardew_crop_manager", manager);
            level.getServer().overworld().getDataStorage().set(NativeStickAttachments.STORAGE, store);
            cleanup(level, List.of(root));
        }
    }

    private static void compareCycle(GameTestHelper helper, StardewCropBlock block,
                                     int season, int days, int regrowDays, int offset) {
        ServerLevel level = nativeLevel(helper);
        BlockPos ordinary = new BlockPos(50000 + offset, 80, 50000);
        BlockPos sticks = ordinary.offset(4, 0, 0);
        var roots = List.of(ordinary, sticks);
        place(level, ordinary, block, false);
        place(level, sticks, block, false);
        var attachment = NativeStickAttachments.get(level).attach(level, sticks);
        var manager = CropGrowthManager.get(level);
        try {
            helper.assertTrue(manager.getAllCropPositions().contains(GlobalPos.of(level.dimension(), sticks)),
                    "The native placement must register in the native scheduler");
            settle(manager, level, roots, 1, season);
            helper.assertTrue(manager.getState(level, sticks).phase == 0
                            && manager.getState(level, sticks).dayInPhase == 0, "Dry day advanced the crop");
            for (var root : roots) level.setBlock(root.below(), level.getBlockState(root.below())
                    .setValue(FarmBlock.MOISTURE, 7), 3);
            for (int day = 2; day <= days + 1; day++) {
                settle(manager, level, roots, day, season);
                var beforeRepeat = level.getBlockState(sticks);
                var growth = manager.getState(level, sticks);
                int phase = growth.phase, progress = growth.dayInPhase;
                settle(manager, level, roots, day, season);
                helper.assertTrue(beforeRepeat == level.getBlockState(sticks)
                                && growth.phase == phase && growth.dayInPhase == progress,
                        "Native date replay changed the crop");
                assertEquivalent(helper, level, ordinary, sticks);
                helper.assertTrue(StardewCropRuntime.inspect(level, sticks).mature() == (day == days + 1),
                        "Native maturity differs from the unboosted schedule on day " + day);
            }
            if (block == ModBlocks.CORN_CROP.get()) {
                var upper = StardewCropRuntime.inspect(level, sticks.above());
                helper.assertTrue(upper != null && upper.root().equals(sticks)
                                && NativeStickAttachments.get(level).inspect(level, sticks.above()).orElseThrow()
                                .plantingId().equals(attachment.plantingId()), "Upper part created another planting");
            }
            for (var root : roots) helper.assertTrue(StardewCropRuntime.harvestForAutomation(
                    level, root, 0, stack -> {}).harvested(), "Native harvest failed");
            if (regrowDays == 0) {
                for (var root : roots) helper.assertTrue(StardewCropRuntime.inspect(level, root) == null,
                        "Single-harvest crop remained alive");
            } else {
                for (int day = days + 2; day <= days + 1 + regrowDays; day++) {
                    settle(manager, level, roots, day, season);
                    assertEquivalent(helper, level, ordinary, sticks);
                    helper.assertTrue(StardewCropRuntime.inspect(level, sticks).mature()
                                    == (day == days + 1 + regrowDays), "Regrowth finished on the wrong day");
                }
            }
            helper.succeed();
        } finally {
            cleanup(level, roots);
        }
    }

    static ServerLevel nativeLevel(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        helper.assertTrue(level != null, "Test preset did not create the real Starfield dimension");
        return level;
    }

    static void place(ServerLevel level, BlockPos root, StardewCropBlock crop, boolean wet) {
        level.getChunk(root);
        level.setBlock(root.below(), ModBlocks.FARMLAND.get().defaultBlockState()
                .setValue(FarmBlock.MOISTURE, wet ? 7 : 0), 3);
        level.setBlock(root, crop.defaultBlockState(), 3);
        crop.syncVisualStage(level, root, CropGrowthManager.get(level).getOrCreateState(level, root));
    }

    static void settle(CropGrowthManager manager, ServerLevel level, List<BlockPos> roots, int day, int season) {
        manager.settleCrops(level, roots.stream().map(p -> GlobalPos.of(level.dimension(), p)).toList(),
                day, season, false);
    }

    private static void assertEquivalent(GameTestHelper helper, ServerLevel level, BlockPos a, BlockPos b) {
        var manager = CropGrowthManager.get(level);
        var left = manager.getState(level, a);
        var right = manager.getState(level, b);
        helper.assertTrue(level.getBlockState(a) == level.getBlockState(b)
                        && level.getBlockState(a.above()) == level.getBlockState(b.above())
                        && left.phase == right.phase && left.dayInPhase == right.dayInPhase
                        && left.regrowing == right.regrowing && left.lastDailyDay == right.lastDailyDay,
                "Stick attachment changed native progression or geometry");
        helper.assertTrue(!level.getBlockState(b).hasBlockEntity() && level.getBlockEntity(b) == null,
                "Stick metadata introduced a block entity");
    }

    static void cleanup(ServerLevel level, List<BlockPos> roots) {
        for (var root : roots) {
            NativeStickAttachments.get(level).detach(level, root);
            StardewCropRuntime.untrack(level, root);
            level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(root.below(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
