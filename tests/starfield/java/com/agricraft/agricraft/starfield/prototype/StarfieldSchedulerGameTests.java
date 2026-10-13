package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.stardew.craft.api.v1.agriculture.*;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Scheduler/persistence probe only. This deliberately does not implement native crop growth.
 * Probe markers live on empty AgriCraft sticks; ordinary crop and weed logic is untouched.
 */
@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldSchedulerGameTests {
    private static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(
            "agricraft", "starfield_scheduler_probe");
    private static final String MARKER = "agricraft:starfield_scheduler_probe";

    private static synchronized void registerProbe() {
        if (StardewCropTypes.definition(TYPE) != null) return;
        StardewCropTypes.register(new StardewCropType(TYPE, "starfield.scheduler.probe", 1,
                List.of(ResourceLocation.fromNamespaceAndPath("agricraft", "crop")), null),
                0, new ProbeAdapter());
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void dailyReplayAndHistoricalContext(GameTestHelper helper) {
        BlockPos root = place(helper, false);
        ServerLevel level = helper.getLevel();
        CropGrowthManager manager = CropGrowthManager.get(level);
        try {
            helper.assertTrue(manager.getAllCropPositions().contains(GlobalPos.of(level.dimension(), root)),
                    "track() must register AgriCraft sticks");
            settle(manager, level, root, 28, 0, false);
            helper.assertTrue(data(level, root).getInt("calls") == 1
                            && data(level, root).getInt("wetDays") == 0,
                    "a dry day must dispatch once without water credit");
            level.setBlock(root.below(), level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE, 7), 3);
            settle(manager, level, root, 29, 1, false);
            settle(manager, level, root, 29, 1, false);
            helper.assertTrue(data(level, root).getInt("calls") == 2
                            && data(level, root).getInt("wetDays") == 1
                            && data(level, root).getInt("season") == 1,
                    "same date must not repeat; supplied historical season must reach the adapter");
            settle(manager, level, root, 28, 0, false);
            helper.assertTrue(data(level, root).getInt("calls") == 2,
                    "older dates must not roll back or replay the plant");
            settle(manager, level, root, 30, 1, true);
            helper.assertTrue(data(level, root).getBoolean("offline")
                            && data(level, root).getInt("absoluteDay") == 30,
                    "offline processing must carry the supplied historical date");
            helper.succeed();
        } finally {
            StardewCropRuntime.untrack(level, root);
        }
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void serializedPlantAndSchedulerDoNotReplay(GameTestHelper helper) {
        BlockPos root = place(helper, true);
        ServerLevel level = helper.getLevel();
        CropGrowthManager original = CropGrowthManager.get(level);
        try {
            settle(original, level, root, 7, 0, false);
            CropBlockEntity before = (CropBlockEntity) level.getBlockEntity(root);
            CompoundTag plantTag = before.saveWithFullMetadata(level.registryAccess());
            CompoundTag schedulerTag = original.save(new CompoundTag(), level.registryAccess());
            CropGrowthManager restored = CropGrowthManager.load(schedulerTag, level.registryAccess());
            level.removeBlockEntity(root);
            CropBlockEntity after = new CropBlockEntity(root, level.getBlockState(root));
            after.loadWithComponents(plantTag, level.registryAccess());
            level.setBlockEntity(after);
            helper.assertTrue(data(level, root).getInt("wetDays") == 1,
                    "block entity reload must restore the probe state");
            helper.assertTrue(restored.getAllCropPositions().contains(GlobalPos.of(level.dimension(), root))
                            && restored.getState(level, root).lastDailyDay == 7,
                    "scheduler reload must restore root identity and processed date");
            settle(restored, level, root, 7, 0, false);
            helper.assertTrue(data(level, root).getInt("calls") == 1,
                    "reload followed by same date must not dispatch twice");
            settle(restored, level, root, 8, 0, false);
            helper.assertTrue(data(level, root).getInt("calls") == 2,
                    "the next date after reload must dispatch once");
            helper.succeed();
        } finally {
            StardewCropRuntime.untrack(level, root);
        }
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void descriptorDoesNotSupplyNativePhaseEngine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(1, 2, 1));
        ResourceLocation type = ResourceLocation.fromNamespaceAndPath("agricraft", "metadata_only_probe");
        StardewCropData potato = new StardewCropData(List.of("spring"), List.of(1, 1, 1, 2, 1), -1,
                14, ResourceLocation.fromNamespaceAndPath("stardewcraft", "grab"),
                ResourceLocation.fromNamespaceAndPath("stardewcraft", "potato"),
                ResourceLocation.fromNamespaceAndPath("stardewcraft", "potato_seeds"));
        if (StardewCropTypes.definition(type) == null) {
            // The default growOneDay() is UNCHANGED, even when the descriptor has real crop metadata.
            StardewCropTypes.register(new StardewCropType(type, "starfield.metadata.probe", 1,
                    List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "gold_block")), potato), 0,
                    (reader, pos) -> reader.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
                            ? new StardewCropState(type, pos, StardewCropState.Part.ROOT, 0, false,
                                    List.of(pos.below())) : null);
        }
        level.setBlock(root, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        StardewCropRuntime.track(level, root);
        try {
            helper.assertTrue(StardewCropRuntime.growOneDay(level, root, true, false)
                            == StardewCropRuntimeAdapter.DailyResult.UNCHANGED,
                    "metadata alone unexpectedly supplied an addon phase executor; revisit the architecture");
            helper.assertTrue(!StardewCropRuntime.inspect(level, root).mature(),
                    "metadata-only crop must remain unchanged");
            helper.succeed();
        } finally {
            StardewCropRuntime.untrack(level, root);
        }
    }

    private static BlockPos place(GameTestHelper helper, boolean wet) {
        registerProbe();
        ServerLevel level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(1, 2, 1));
        // Soil registration belongs to iteration 1. Use already-supported vanilla farmland
        // to isolate the real scheduler's FarmBlock moisture inputs from that pending feature.
        level.setBlock(root.below(), net.minecraft.world.level.block.Blocks.FARMLAND
                .defaultBlockState().setValue(FarmBlock.MOISTURE, wet ? 7 : 0), 3);
        level.setBlock(root, ModBlocks.CROP.get().defaultBlockState(), 3);
        CropBlockEntity entity = (CropBlockEntity) level.getBlockEntity(root);
        entity.getPersistentData().put(MARKER, new CompoundTag());
        entity.setChanged();
        StardewCropRuntime.track(level, root);
        return root;
    }

    private static CompoundTag data(ServerLevel level, BlockPos root) {
        return level.getBlockEntity(root).getPersistentData().getCompound(MARKER);
    }

    private static void settle(CropGrowthManager manager, ServerLevel level, BlockPos root,
                               int day, int season, boolean offline) {
        manager.settleCrops(level, List.of(GlobalPos.of(level.dimension(), root)), day, season, offline);
    }

    private static final class ProbeAdapter implements StardewCropRuntimeAdapter {
        @Override
        public StardewCropState inspect(LevelReader level, BlockPos position) {
            if (!(level.getBlockState(position).getBlock() instanceof CropBlock)
                    || !(level.getBlockEntity(position) instanceof CropBlockEntity entity)
                    || !entity.getPersistentData().contains(MARKER)) return null;
            return new StardewCropState(TYPE, position, StardewCropState.Part.ROOT, 0, false,
                    List.of(position.below()));
        }

        @Override
        public int dailyNeighborhoodRadius() { return 0; }

        @Override
        public DailyResult growOneDay(ServerLevel level, StardewCropState crop,
                                      StardewCropDailyContext context) {
            CompoundTag state = data(level, crop.root());
            state.putInt("calls", state.getInt("calls") + 1);
            state.putInt("wetDays", state.getInt("wetDays") + (context.watered() ? 1 : 0));
            state.putInt("absoluteDay", context.absoluteDay());
            state.putInt("season", context.season());
            state.putBoolean("offline", context.offlineCatchUp());
            level.getBlockEntity(crop.root()).setChanged();
            return DailyResult.CHANGED;
        }
    }
}
