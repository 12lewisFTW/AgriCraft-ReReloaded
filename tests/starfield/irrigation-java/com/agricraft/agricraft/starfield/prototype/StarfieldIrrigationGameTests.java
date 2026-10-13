package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.compat.ExternalIrrigationSupport;
import com.agricraft.agricraft.api.config.IrrigationConfig;
import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationChannelBlockEntity;
import com.agricraft.agricraft.common.block.entity.IrrigationTankBlockEntity;
import com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.block.FertilizerType;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.utility.SprinklerTier;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmDailyProcessHelper;
import com.stardew.craft.farm.FarmInstance;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.item.tool.WateringCanItem;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Uses real paid block-entity sweeps and native watering/daily operations, without a second executor. */
@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldIrrigationGameTests {
    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void wateringCanNativeSprinklerAndPaidAgriSweepsMatureTogether(GameTestHelper helper) {
        try (var plot = plot(helper, "sources"); var config = new Config()) {
            var roots = List.of(plot.base.west(8), plot.base, plot.base.east(8), plot.base.east(9));
            for (var root : roots) plant(plot, root, "potato", soil(0), !root.equals(roots.get(0)));
            var rig = rig(plot, plot.base.east(8).above(3)); rig.channel.setContent(500);
            var identity = StarfieldCompat.planting(plot.level, roots.get(3)).orElseThrow().encode();
            for (int day = 1; day <= 6; day++) {
                for (var root : roots) dry(plot, root);
                var before = snapshot(plot, roots.get(3));
                manualWater(helper, plot, roots.get(0));
                com.stardew.craft.block.utility.SprinklerBlock.waterNow(plot.level, roots.get(1).east(), SprinklerTier.BASIC, false);
                rig.channel.setContent(500); tick(plot, rig, 49);
                for (var root : roots) helper.assertTrue(wet(plot, root), "Water source did not reach native soil at " + root);
                helper.assertTrue(snapshot(plot, roots.get(3)).equals(before), "Irrigation advanced native progress before settlement");
                settle(plot, roots, day, 0);
                for (var root : roots) helper.assertTrue(snapshot(plot, root).equals(snapshot(plot, roots.get(0))), "Water sources produced different native daily results");
                helper.assertTrue(StarfieldCompat.inspect(plot.level, roots.get(0)).orElseThrow().mature() == (day == 6), "Potato maturity calendar changed");
            }
            helper.assertTrue(StarfieldCompat.planting(plot.level, roots.get(3)).orElseThrow().encode().equals(identity), "Paid irrigation lost native planting/genome");
            helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void emptyClosedAndDisconnectedSystemsCannotWaterButTankFlowCan(GameTestHelper helper) {
        try (var plot = plot(helper, "supply"); var config = new Config()) {
            plant(plot, plot.base, "potato", soil(0), true); var rig = rig(plot, plot.base.above(3));
            tick(plot, rig, 120);
            helper.assertTrue(!wet(plot, plot.base) && !active(plot, rig), "Empty system watered a crop");
            plot.set(rig.pos.above(), plot.level.getBlockState(rig.pos.above()).setValue(IrrigationChannelBlock.VALVE, IrrigationChannelBlock.Valve.CLOSED));
            rig.channel.setContent(500); tick(plot, rig, 100);
            helper.assertTrue(!wet(plot, plot.base) && rig.channel.getContent() == 500, "Closed valve supplied irrigation");
            rig.channel.setContent(0);
            var disconnected = rig.pos.above().west(3);
            plot.set(disconnected, ModBlocks.IRRIGATION_TANK.get().defaultBlockState());
            var detachedTank = (IrrigationTankBlockEntity) plot.level.getBlockEntity(disconnected); detachedTank.setContent(2000);
            for (int i = 0; i < 70; i++) { IrrigationTankBlockEntity.tick(plot.level, disconnected, plot.level.getBlockState(disconnected), detachedTank); tick(plot, rig, 1); }
            helper.assertTrue(!wet(plot, plot.base) && detachedTank.getContent() == 2000, "Disconnected tank supplied irrigation");
            plot.set(rig.pos.above(), plot.level.getBlockState(rig.pos.above()).setValue(IrrigationChannelBlock.VALVE, IrrigationChannelBlock.Valve.OPEN));
            var tankPos = rig.pos.above().west();
            plot.set(tankPos, ModBlocks.IRRIGATION_TANK.get().defaultBlockState().setValue(IrrigationTankBlock.EAST, IrrigationTankBlock.Connection.CHANNEL));
            var tank = (IrrigationTankBlockEntity) plot.level.getBlockEntity(tankPos); tank.setContent(3000);
            int supplied = tank.getContent() + rig.channel.getContent();
            for (int i = 0; i < 49; i++) {
                IrrigationTankBlockEntity.tick(plot.level, tankPos, plot.level.getBlockState(tankPos), tank);
                IrrigationChannelBlockEntity.tick(plot.level, rig.pos.above(), plot.level.getBlockState(rig.pos.above()), rig.channel);
                tick(plot, rig, 1);
            }
            int remaining = tank.getContent() + rig.channel.getContent() + buffer(plot, rig);
            helper.assertTrue(wet(plot, plot.base) && supplied - remaining == 98, "Tank/channel sweep did not conserve the configured 2 mB per column: " + (supplied - remaining));
            helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void radiusHeightObstaclesAndTallCropSoilKeepTheOrdinarySweep(GameTestHelper helper) {
        try (var plot = plot(helper, "geometry"); var config = new Config()) {
            var rig = rig(plot, plot.base.above(3));
            var tall = plot.base; var edge = plot.base.east(3); var outside = plot.base.west(4);
            var depthEdge = plot.base.south(2).below(); var tooDeep = plot.base.north(2).below(2); var blocked = plot.base.west(2);
            for (var root : List.of(edge, outside, depthEdge, tooDeep, blocked)) plant(plot, root, "potato", soil(0), true);
            plant(plot, tall, "corn", soil(0), true);
            var tallState = plot.level.getBlockState(tall).setValue(StardewCropBlock.AGE, StardewCropBlock.MAX_AGE);
            plot.set(tall, tallState); plot.set(tall.above(), tallState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            plot.set(blocked.above(), Blocks.STONE.defaultBlockState());
            var before = snapshot(plot, tall); rig.channel.setContent(500); tick(plot, rig, 49);
            helper.assertTrue(wet(plot, tall) && wet(plot, edge) && wet(plot, depthEdge), "Tall parts or radius/height edge blocked reachable soil");
            helper.assertTrue(!wet(plot, outside) && !wet(plot, tooDeep) && !wet(plot, blocked), "Irrigation bypassed radius, height or a solid obstacle");
            helper.assertTrue(snapshot(plot, tall).equals(before) && !plot.level.getBlockState(tall.above()).isAir(), "Tall-crop irrigation changed native progress/geometry");
            helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeSoilRestrictionsAndGardenPotMatchTheNativeSprinkler(GameTestHelper helper) {
        try (var plot = plot(helper, "soils"); var config = new Config()) {
            var roots = List.of(plot.base, plot.base.east(), plot.base.west(), plot.base.south());
            var soils = List.of(soil(0), com.stardew.craft.block.ModBlocks.SANDY_FARMLAND.get().defaultBlockState(),
                    com.stardew.craft.block.ModBlocks.INFERTILE_FARMLAND.get().defaultBlockState(), com.stardew.craft.block.ModBlocks.GARDEN_POT.get().defaultBlockState());
            for (int i = 0; i < roots.size(); i++) plant(plot, roots.get(i), "potato", soils.get(i).setValue(FarmBlock.MOISTURE, 0), true);
            var rig = rig(plot, plot.base.above(3)); rig.channel.setContent(500); tick(plot, rig, 49);
            var agri = roots.stream().map(root -> wet(plot, root)).toList();
            helper.assertTrue(agri.equals(List.of(true, false, false, true)), "Native sprinkler soil restrictions were bypassed: " + agri);
            for (var root : roots) dry(plot, root);
            com.stardew.craft.block.utility.SprinklerBlock.waterNow(plot.level, plot.base, SprinklerTier.IRIDIUM, false);
            // The native sprinkler excludes its own center; water that control tile separately.
            com.stardew.craft.block.utility.SprinklerBlock.waterNow(plot.level, plot.base.east(), SprinklerTier.BASIC, false);
            helper.assertTrue(roots.stream().map(root -> wet(plot, root)).toList().equals(agri), "Native and Agri sprinkler soil policies differ");
            manualWater(helper, plot, roots.get(1)); manualWater(helper, plot, roots.get(2));
            helper.assertTrue(wet(plot, roots.get(1)) && wet(plot, roots.get(2)), "Manual watering of restricted soils was altered");
            helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void repeatedAndOverlappingSweepsCannotAdvanceDaysOrRegrow(GameTestHelper helper) {
        try (var plot = plot(helper, "overlap"); var config = new Config()) {
            var roots = List.of(plot.base, plot.base.east());
            for (var root : roots) plant(plot, root, "corn", soil(0), root.equals(plot.base.east()));
            var first = rig(plot, plot.base.above(3)); var second = rig(plot, plot.base.east(2).above(3));
            var before = snapshot(plot, plot.base);
            feedAndSweep(plot, first, second, 5);
            helper.assertTrue(snapshot(plot, plot.base).equals(before), "Multiple paid sprinklers advanced a calendar day");
            int duration = StarfieldCompat.species(ResourceLocation.parse("stardewcraft:corn_crop")).orElseThrow().nativePhaseDays().stream().mapToInt(Integer::intValue).sum();
            for (int day = 1; day <= duration; day++) {
                feedAndSweep(plot, first, second, 2); settle(plot, roots, day, 1);
                helper.assertTrue(snapshot(plot, roots.get(0)).equals(snapshot(plot, roots.get(1))), "Plain/sticks overlap changed native growth");
                var once = snapshot(plot, roots.get(0)); feedAndSweep(plot, first, second, 2); settle(plot, roots, day, 1);
                helper.assertTrue(snapshot(plot, roots.get(0)).equals(once), "Repeated same-day watering bypassed settlement deduplication");
            }
            for (var root : roots) {
                helper.assertTrue(StarfieldCompat.inspect(plot.level, root).orElseThrow().mature(), "Corn did not mature natively");
                plot.player.getInventory().clearContent(); stand(plot.player, root);
                plot.level.getBlockState(root).useWithoutItem(plot.level, plot.player, new BlockHitResult(Vec3.atCenterOf(root), Direction.UP, root, false));
                helper.assertTrue(CropGrowthManager.get(plot.level).getState(plot.level, root).regrowing, "Native corn harvest did not start regrowth");
            }
            var regrow = snapshot(plot, roots.get(0)); feedAndSweep(plot, first, second, 5);
            helper.assertTrue(snapshot(plot, roots.get(0)).equals(regrow), "Irrigation skipped native regrow days");
            for (int day = 1; day <= 4; day++) {
                feedAndSweep(plot, first, second, 2); settle(plot, roots, duration + day, 1);
                helper.assertTrue(snapshot(plot, roots.get(0)).equals(snapshot(plot, roots.get(1))), "Overlap changed repeat harvest timing");
                helper.assertTrue(StarfieldCompat.inspect(plot.level, roots.get(0)).orElseThrow().mature() == (day == 4), "Corn regrow calendar changed");
            }
            helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void dailySettlementConsumesPaidWaterAndNativeFertilizerRetainsIt(GameTestHelper helper) {
        try (var plot = plot(helper, "retention"); var config = new Config()) {
            var clock = StardewTimeManager.get(); int year = clock.getCurrentYear(), season = clock.getCurrentSeason(), day = clock.getCurrentDay();
            try {
                var roots = List.of(plot.base, plot.base.east());
                for (var root : roots) plant(plot, root, "potato", soil(0), root.equals(plot.base.east()));
                var rig = rig(plot, plot.base.above(3)); rig.channel.setContent(500); tick(plot, rig, 49);
                helper.assertTrue(FertilizerManager.get(plot.level).tryApplyFertilizer(plot.level, roots.get(1).below(), FertilizerType.DELUXE_RETAINING_SOIL), "Retaining fertilizer could not be applied");
                clock.setCurrentYear(1); clock.setCurrentSeason(0); clock.setCurrentDay(2); plot.farm.markActiveOnDay(1);
                nativeDaily(plot);
                helper.assertTrue(!wet(plot, roots.get(0)) && wet(plot, roots.get(1)), "Paid water bypassed native overnight consumption/retention");
                helper.assertTrue(snapshot(plot, roots.get(0)).equals(snapshot(plot, roots.get(1))), "Retaining soil changed the first daily growth");
                // Empty channel + no ticks: retained moisture alone is available for the next native day.
                var dryProgress = snapshot(plot, roots.get(0)); clock.setCurrentDay(3); plot.farm.markActiveOnDay(2); nativeDaily(plot);
                var dryGrowth = CropGrowthManager.get(plot.level).getState(plot.level, roots.get(0));
                var retainedGrowth = CropGrowthManager.get(plot.level).getState(plot.level, roots.get(1));
                helper.assertTrue(dryGrowth.phase == dryProgress.getInt("phase") && dryGrowth.dayInPhase == dryProgress.getInt("progress") && !dryGrowth.lastDailyWatered,
                        "Unwatered next day advanced a crop");
                helper.assertTrue(retainedGrowth.lastDailyWatered && retainedGrowth.phase != dryGrowth.phase && wet(plot, roots.get(1)), "Native retained water was not reused");
                helper.succeed();
            } finally { clock.setCurrentYear(year); clock.setCurrentSeason(season); clock.setCurrentDay(day); }
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void paidBufferAndPartialSweepPersistWithoutRefundOrDuplicateGrowth(GameTestHelper helper) {
        try (var plot = plot(helper, "buffer"); var config = new Config()) {
            plant(plot, plot.base, "potato", soil(0), true); var rig = rig(plot, plot.base.above(3)); rig.channel.setContent(500);
            tick(plot, rig, 5); var saved = rig.sprinkler.saveWithoutMetadata(plot.level.registryAccess());
            helper.assertTrue(saved.getInt("water_buffer") == 78 && saved.getInt("column_counter") == 5 && rig.channel.getContent() == 412,
                    "Partial sweep did not account for pulled/paid water: " + saved + " channel=" + rig.channel.getContent());
            rig.channel.setContent(0);
            var restored = new SprinklerBlockEntity(rig.pos, plot.level.getBlockState(rig.pos));
            restored.loadWithComponents(saved, plot.level.registryAccess()); plot.level.setBlockEntity(restored);
            rig = new Rig(rig.pos, rig.channel, restored);
            var chunk = plot.level.getChunkAt(rig.pos); chunk.setUnsaved(false); tick(plot, rig, 1);
            helper.assertTrue(chunk.isUnsaved(), "Continuously active water consumption did not mark the sprinkler dirty");
            tick(plot, rig, 100);
            var exhausted = rig.sprinkler.saveWithoutMetadata(plot.level.registryAccess());
            helper.assertTrue(wet(plot, plot.base) && exhausted.getInt("water_buffer") == 0 && exhausted.getInt("column_counter") == 44 && !active(plot, rig),
                    "Restored sweep refunded water, restarted or irrigated after exhaustion: " + exhausted);
            helper.assertTrue(CropGrowthManager.get(plot.level).getState(plot.level, plot.base).phase == 0, "Buffer restore advanced the crop calendar");
            helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void everyNativeSpeciesAcceptsPaidWaterWithoutGenotypeOrProgressChanges(GameTestHelper helper) {
        try (var plot = plot(helper, "species"); var config = new Config()) {
            var rig = rig(plot, plot.base.above(3)); int count = 0;
            for (var species : StarfieldCompat.species()) {
                removePlant(plot, plot.base); plant(plot, plot.base, species.id().getPath().replaceFirst("_crop$", ""), soil(0), true);
                var before = snapshot(plot, plot.base); var identity = StarfieldCompat.planting(plot.level, plot.base).orElseThrow().encode();
                rig.channel.setContent(500); tick(plot, rig, 49);
                helper.assertTrue(wet(plot, plot.base) && snapshot(plot, plot.base).equals(before)
                        && identity.equals(StarfieldCompat.planting(plot.level, plot.base).orElseThrow().encode()), "Paid irrigation changed species state: " + species.id());
                count++;
            }
            helper.assertTrue(count == 50, "The native irrigation catalog no longer covers 50 species"); helper.succeed();
        }
    }

    @GameTest(batch = "starfield_irrigation", templateNamespace = "agricraft", template = "starfield_probe")
    public static void sweepDoesNotLoadAdjacentChunksAndLeavesOrdinaryFarmlandSupported(GameTestHelper helper) {
        try (var plot = plot(helper, "chunks"); var config = new Config()) {
            var chunk = new ChunkPos(plot.base); var center = new BlockPos(chunk.getMinBlockX() + 13, plot.base.getY(), chunk.getMinBlockZ() + 8);
            plant(plot, center, "potato", soil(0), true); var rig = rig(plot, center.above(3));
            var ordinary = center.west(); plot.set(ordinary.below(), Blocks.FARMLAND.defaultBlockState());
            plot.set(ordinary, Blocks.WHEAT.defaultBlockState());
            var unloaded = new BlockPos(chunk.getMaxBlockX() + 1, center.getY(), center.getZ());
            helper.assertTrue(!plot.level.hasChunkAt(unloaded), "Chunk-boundary fixture unexpectedly loaded its neighbour");
            IrrigationConfig.sprinklerGrowthChance = 0; rig.channel.setContent(500); tick(plot, rig, 49);
            helper.assertTrue(!plot.level.hasChunkAt(unloaded), "Irrigation force-loaded an adjacent chunk");
            helper.assertTrue(wet(plot, center) && wet(plot, ordinary), "Loaded native or ordinary soil was not irrigated");
            helper.assertTrue(ExternalIrrigationSupport.irrigate(plot.level, ordinary, plot.level.getBlockState(ordinary)) == ExternalIrrigationSupport.Result.UNHANDLED,
                    "Native integration intercepted an ordinary crop");
            helper.succeed();
        }
    }

    private record Rig(BlockPos pos, IrrigationChannelBlockEntity channel, SprinklerBlockEntity sprinkler) {}
    private static final class Config implements AutoCloseable {
        final int consumption = IrrigationConfig.sprinklerWaterConsumption, interval = IrrigationConfig.sprinklerInterval;
        final double chance = IrrigationConfig.sprinklerGrowthChance;
        Config() { IrrigationConfig.sprinklerWaterConsumption = 40; IrrigationConfig.sprinklerInterval = 1; IrrigationConfig.sprinklerGrowthChance = 1; }
        @Override public void close() { IrrigationConfig.sprinklerWaterConsumption = consumption; IrrigationConfig.sprinklerInterval = interval; IrrigationConfig.sprinklerGrowthChance = chance; }
    }
    private static final class Plot implements AutoCloseable {
        final ServerLevel level; final FarmInstance farm; final BlockPos base; final ServerPlayer player;
        final ArrayList<BlockPos> placed = new ArrayList<>();
        Plot(ServerLevel level, FarmInstance farm, BlockPos base, ServerPlayer player) { this.level = level; this.farm = farm; this.base = base; this.player = player; }
        void set(BlockPos pos, BlockState state) { level.getChunk(pos); level.setBlock(pos, state, 3); if (!placed.contains(pos)) placed.add(pos); }
        @Override public void close() {
            player.getInventory().clearContent(); player.stopUsingItem();
            // Remove crops before soils, and hanging sprinklers before their supporting channels.
            for (var pos : placed) if (level.getBlockState(pos).getBlock() instanceof StardewCropBlock) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (var pos : placed) { FertilizerManager.get(level).removeFertilizer(level, pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(15)).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        }
    }
    private static Plot plot(GameTestHelper helper, String name) {
        var level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        var id = UUID.nameUUIDFromBytes(("agricraft:irrigation:" + name).getBytes(StandardCharsets.UTF_8));
        var registry = FarmInstanceRegistry.get(level.getServer()); var farm = registry.getFarm(id);
        if (farm == null) farm = registry.createFarm(id, name, "Irrigation probe", FarmType.STANDARD);
        farm.markInitialized(); farm.setGraceDaysLeft(0);
        var player = FakePlayerFactory.get(level, new GameProfile(id, "WaterProbe")); player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setShiftKeyDown(false);
        // The test world persists across JVMs; each probe starts with payable native energy.
        com.stardew.craft.player.PlayerStardewDataAPI.restoreEnergy(player,
                com.stardew.craft.player.PlayerStardewDataAPI.getMaxEnergy(player));
        return new Plot(level, farm, farm.getOrigin().offset(104, 10, 104), player);
    }
    private static BlockState soil(int moisture) { return com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, moisture); }
    private static void plant(Plot plot, BlockPos root, String species, BlockState soil, boolean sticks) {
        plot.set(root.below(), soil); plot.set(root, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("stardewcraft:" + species + "_crop")).defaultBlockState());
        StarfieldCompat.planting(plot.level, root).orElseThrow(); if (sticks && !StarfieldCompat.setSticks(plot.level, root, "iron")) throw new IllegalStateException("No irrigation stick attachment");
    }
    private static Rig rig(Plot plot, BlockPos pos) {
        plot.set(pos.above(), ModBlocks.IRRIGATION_CHANNEL.get().defaultBlockState()); plot.set(pos, ModBlocks.SPRINKLER.get().defaultBlockState());
        return new Rig(pos, (IrrigationChannelBlockEntity) plot.level.getBlockEntity(pos.above()), (SprinklerBlockEntity) plot.level.getBlockEntity(pos));
    }
    private static void tick(Plot plot, Rig rig, int ticks) { for (int i = 0; i < ticks; i++) SprinklerBlockEntity.tick(plot.level, rig.pos, plot.level.getBlockState(rig.pos), rig.sprinkler); }
    private static void feedAndSweep(Plot plot, Rig first, Rig second, int sweeps) {
        first.channel.setContent(500); second.channel.setContent(500); for (int i = 0; i < sweeps * 49; i++) { tick(plot, first, 1); tick(plot, second, 1); }
    }
    private static boolean active(Plot plot, Rig rig) { return plot.level.getBlockState(rig.pos).getValue(com.agricraft.agricraft.common.block.SprinklerBlock.ACTIVE); }
    private static int buffer(Plot plot, Rig rig) { return rig.sprinkler.saveWithoutMetadata(plot.level.registryAccess()).getInt("water_buffer"); }
    private static void dry(Plot plot, BlockPos root) { plot.set(root.below(), plot.level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE, 0)); }
    private static boolean wet(Plot plot, BlockPos root) { return plot.level.getBlockState(root.below()).getValue(FarmBlock.MOISTURE) > 0; }
    private static void settle(Plot plot, List<BlockPos> roots, int day, int season) { CropGrowthManager.get(plot.level).settleCrops(plot.level, roots.stream().map(root -> GlobalPos.of(plot.level.dimension(), root)).toList(), day, season, false); }
    private static void nativeDaily(Plot plot) { FarmDailyProcessHelper.beginDailyProcess(plot.level); try { CropGrowthManager.get(plot.level).growDaily(plot.level); } finally { FarmDailyProcessHelper.endDailyProcess(plot.level); } }
    private static CompoundTag snapshot(Plot plot, BlockPos root) {
        var growth = CropGrowthManager.get(plot.level).getState(plot.level, root); var tag = new CompoundTag();
        tag.putInt("phase", growth.phase); tag.putInt("progress", growth.dayInPhase); tag.putInt("soilProgress", growth.infertileProgress);
        tag.putBoolean("regrowing", growth.regrowing); tag.putInt("lastDay", growth.lastDailyDay); tag.putBoolean("watered", growth.lastDailyWatered);
        tag.putInt("lastGiant", growth.lastGiantDay); tag.putBoolean("source", growth.sourcePhases); tag.putInt("sourceVersion", growth.sourcePhaseVersion);
        tag.putString("block", plot.level.getBlockState(root).toString()); return tag;
    }
    private static void stand(ServerPlayer player, BlockPos root) { player.setPos(root.getX() + .5, root.getY() + 1, root.getZ() + .5); player.setXRot(90); player.setYRot(0); }
    private static void manualWater(GameTestHelper helper, Plot plot, BlockPos root) {
        var stack = new ItemStack(com.stardew.craft.item.ModItems.WATERING_CAN.get()); var can = (WateringCanItem) stack.getItem();
        can.setWater(stack, 10); plot.player.setItemInHand(InteractionHand.MAIN_HAND, stack); plot.player.getCooldowns().removeCooldown(can); stand(plot.player, root);
        can.useOn(new UseOnContext(plot.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(root), Direction.UP, root, false)));
        can.releaseUsing(stack, plot.level, plot.player, can.getUseDuration(stack, plot.player)); plot.player.stopUsingItem();
        helper.assertTrue(wet(plot, root) && can.getWater(stack) == 9, "Real native watering-can action did not wet/pay for the tile");
        plot.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
    private static void removePlant(Plot plot, BlockPos root) { plot.set(root, Blocks.AIR.defaultBlockState()); plot.set(root.above(), Blocks.AIR.defaultBlockState()); }
}
