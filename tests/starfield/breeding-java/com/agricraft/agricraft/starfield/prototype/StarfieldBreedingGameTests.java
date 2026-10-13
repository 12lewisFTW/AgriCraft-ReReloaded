package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.genetic.AgriMutationHandler;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.block.*;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.compat.starfield.*;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldBreedingGameTests {
    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void actualEmptyDoubleTickProducesFreshNativeChild(GameTestHelper helper) {
        try (var plot = new Plot(helper, "actual-tick", 0)) {
            var a = plot.root.west(); var b = plot.root.east();
            plot.parent(a, "potato", true, 6, 4); plot.parent(b, "potato", false, 5, 3);
            FertilizerManager.get(plot.level).tryApplyFertilizer(plot.level, a.below(), com.stardew.craft.block.FertilizerType.SPEED_GRO);
            var beforeA = snapshot(plot.level, a); var beforeB = snapshot(plot.level, b);
            var target = plot.target(CropStickVariant.IRON);
            plot.level.setBlock(plot.root.below(), soil(0), 3);
            boolean weeds = CoreConfig.disableWeeds; CoreConfig.disableWeeds = true;
            try { target.applyGrowthTick(); } finally { CoreConfig.disableWeeds = weeds; }
            helper.assertTrue(plot.level.getBlockState(plot.root).is(BuiltInRegistries.BLOCK.get(id("potato"))), "Empty-double tick did not spawn native child");
            var child = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
            helper.assertTrue(child.sticks().equals("iron") && !child.plantingId().equals(StarfieldCompat.planting(plot.level, a).orElseThrow().plantingId()), "Child lost variant or inherited UUID");
            var state = CropGrowthManager.get(plot.level).getState(plot.level, plot.root);
            helper.assertTrue(state.phase == 0 && state.dayInPhase == 0 && !state.regrowing && state.lastDailyDay != 40,
                    "Child inherited mature native progress");
            helper.assertTrue(plot.level.getBlockEntity(plot.root) == null && !StarfieldCompat.inspect(plot.level, plot.root).orElseThrow().mature(), "Virtual crop or mature child created");
            helper.assertTrue(plot.level.getBlockState(plot.root.below()).getValue(FarmBlock.MOISTURE) == 0
                    && FertilizerManager.get(plot.level).getFertilizer(plot.level, plot.root.below()) == null, "Parent water/fertilizer inherited");
            helper.assertTrue(beforeA.equals(snapshot(plot.level, a)) && beforeB.equals(snapshot(plot.level, b)), "Breeding changed parent genome/progress: " + beforeA + " -> " + snapshot(plot.level, a) + " / " + beforeB + " -> " + snapshot(plot.level, b));
            target.applyGrowthTick(); // Removed target cannot breed twice.
            helper.assertTrue(StarfieldCompat.planting(plot.level, plot.root).orElseThrow().plantingId().equals(child.plantingId()), "Stale target produced another child");
            // A dry native day keeps the child's initial progress, then normal watered days mature it.
            var manager = CropGrowthManager.get(plot.level);
            manager.settleCrops(plot.level, List.of(GlobalPos.of(plot.level.dimension(), plot.root)), 41, 0, false);
            helper.assertTrue(!StarfieldCompat.inspect(plot.level, plot.root).orElseThrow().mature() && state.dayInPhase == 0, "Dry newborn advanced");
            for (int day = 42; day < 48; day++) {
                plot.level.setBlock(plot.root.below(), soil(7), 3);
                manager.settleCrops(plot.level, List.of(GlobalPos.of(plot.level.dimension(), plot.root)), day, 0, false);
            }
            helper.assertTrue(StarfieldCompat.inspect(plot.level, plot.root).orElseThrow().mature(), "Newborn did not follow native six-day cycle");
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void singleParentUsesConfiguredCloneAndBothAlleles(GameTestHelper helper) {
        try (var plot = new Plot(helper, "clone", 0)) {
            plot.parent(plot.root.west(), "potato", false, 6, 2);
            boolean old = CoreConfig.cloneMutations; CoreConfig.cloneMutations = false;
            try {
                var genome = StarfieldCompat.planting(plot.level, plot.root.west()).orElseThrow().genome();
                boolean spawned = false;
                for (int attempt = 0; attempt < 200 && !spawned; attempt++) spawned = breed(plot.target(CropStickVariant.WOODEN), attempt);
                helper.assertTrue(spawned && StarfieldCompat.planting(plot.level, plot.root).orElseThrow().genome().equals(genome), "Clone lost recessive alleles or ignored cloneMutations=false");
                helper.succeed();
            } finally { CoreConfig.cloneMutations = old; }
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void fertilityAndMultiplierUseExistingRolls(GameTestHelper helper) {
        try (var plot = new Plot(helper, "fertility", 0)) {
            plot.parent(plot.root.west(), "potato", true, 4, 2); plot.parent(plot.root.east(), "potato", false, 4, 2);
            double multiplier = CoreConfig.mutationChanceMultiplier;
            try {
                var target = plot.target(CropStickVariant.WOODEN); CoreConfig.mutationChanceMultiplier = 0;
                for (int attempt = 0; attempt < 32; attempt++) helper.assertTrue(!breed(target, attempt), "Zero fertility multiplier bred offspring");
                CoreConfig.mutationChanceMultiplier = 1;
                setFertility(plot.level, plot.root.west(), 1); setFertility(plot.level, plot.root.east(), 1);
                int low = countSuccesses(plot, 128);
                setFertility(plot.level, plot.root.west(), AgriStatRegistry.getInstance().fertilityStat().getMax());
                setFertility(plot.level, plot.root.east(), AgriStatRegistry.getInstance().fertilityStat().getMax());
                int high = countSuccesses(plot, 128);
                helper.assertTrue(high == 128 && low < high / 2, "Fertility did not change standard selection: " + low + "/" + high);
                helper.succeed();
            } finally { CoreConfig.mutationChanceMultiplier = multiplier; }
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void mutativityChangesInheritedStatsWithinConfiguredBounds(GameTestHelper helper) {
        try (var plot = new Plot(helper, "mutativity", 0)) {
            plot.parent(plot.root.west(), "potato", true, 4, 2); plot.parent(plot.root.east(), "potato", false, 4, 2);
            var stat = AgriStatRegistry.getInstance().mutativityStat();
            int lowUnchanged = 0;
            for (int i = 0; i < 48; i++) {
                setStat(plot.level, plot.root.west(), stat.getId(), stat.getMin(), stat.getMin());
                setStat(plot.level, plot.root.east(), stat.getId(), stat.getMin(), stat.getMin());
                helper.assertTrue(breed(plot.target(CropStickVariant.WOODEN), i), "Guaranteed fertile parents failed");
                var pair = AgriGenome.fromNBT(StarfieldCompat.planting(plot.level, plot.root).orElseThrow().genome()).getStatGene(AgriStatRegistry.getInstance().growthStat());
                if (List.of(2, 4).contains(pair.getDominant().trait())) lowUnchanged++;
                if (List.of(2, 4).contains(pair.getRecessive().trait())) lowUnchanged++;
                plot.clearChild();
                setStat(plot.level, plot.root.west(), stat.getId(), stat.getMax(), stat.getMax());
                setStat(plot.level, plot.root.east(), stat.getId(), stat.getMax(), stat.getMax());
                helper.assertTrue(breed(plot.target(CropStickVariant.WOODEN), i), "Maximum mutativity failed");
                var child = AgriGenome.fromNBT(StarfieldCompat.planting(plot.level, plot.root).orElseThrow().genome());
                var growth = child.getStatGene(AgriStatRegistry.getInstance().growthStat());
                helper.assertTrue(List.of(3, 5).contains(growth.getDominant().trait()) && List.of(3, 5).contains(growth.getRecessive().trait()), "Maximum mutativity did not use existing +1 inheritance rule");
                for (var bound : AgriStatRegistry.getInstance()) {
                    var value = child.getStatGene(bound);
                    helper.assertTrue(value.getDominant().trait() <= bound.getMax() && value.getRecessive().trait() >= bound.getMin(), "Stat escaped configured bounds");
                }
                plot.clearChild();
            }
            helper.assertTrue(lowUnchanged > 0, "Minimum mutativity behaved like guaranteed mutation");
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void immatureParentsAndUpperCarrierDoNotBreed(GameTestHelper helper) {
        try (var plot = new Plot(helper, "maturity", 1)) {
            plot.parent(plot.root.west(), "corn", true, 4, 2); plot.parent(plot.root.east(), "corn", false, 4, 2);
            // Use real immature initial blocks instead of trusting an attachment's genome.
            for (var pos : List.of(plot.root.west(), plot.root.east())) plot.level.setBlock(pos, plot.level.getBlockState(pos).setValue(StardewCropBlock.AGE, 0), 3);
            var target = plot.target(CropStickVariant.IRON);
            for (int i = 0; i < 64; i++) helper.assertTrue(!breed(target, i), "Immature native parent contributed");
            plot.clearChild();
            // An upper part next to empty doubles one block higher is not a root-height neighbour.
            var upperTargetPos = plot.root.above();
            plot.level.setBlock(plot.root, Blocks.STONE.defaultBlockState(), 3);
            plot.level.setBlock(upperTargetPos, com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.DOUBLE_STICKS), 3);
            var upperTarget = (CropBlockEntity) plot.level.getBlockEntity(upperTargetPos);
            for (int i = 0; i < 64; i++) helper.assertTrue(!breed(upperTarget, i), "Upper carrier counted as parent");
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void differentSpeciesRequireExplicitRecipe(GameTestHelper helper) {
        try (var plot = new Plot(helper, "recipe", 0)) {
            plot.parent(plot.root.west(), "potato", false, 4, 2); plot.parent(plot.root.east(), "parsnip", true, 4, 2);
            var target = plot.target(CropStickVariant.WOODEN); var before = target.saveWithoutMetadata(plot.level.registryAccess());
            for (int i = 0; i < 32; i++) helper.assertTrue(!breed(target, i), "Different native species mixed without a recipe");
            helper.assertTrue(before.equals(target.saveWithoutMetadata(plot.level.registryAccess())), "Recipe rejection mutated doubles");
            plot.remove(plot.root.east()); plot.parent(plot.root.east(), "carrot", true, 4, 2);
            helper.assertTrue(AgriApi.getMutationsFromParents(id("potato").toString(), id("carrot").toString()).anyMatch(m -> m.child().equals(id("cauliflower"))), "Test-only recipe did not load");
            var recipe = AgriApi.getMutationsFromParents(id("potato").toString(), id("carrot").toString()).findFirst().orElseThrow();
            helper.assertTrue(recipe.isValidForBreeding() && !recipe.isValid()
                    && AgriApi.getMutationHandler().complexity(id("cauliflower").toString()) > AgriApi.getMutationHandler().complexity(id("carrot").toString()),
                    "Native recipe lost ordinary gene dominance or leaked virtual ingredients into GUI recipes");
            boolean result = breed(target, 12);
            helper.assertTrue(result && StarfieldCompat.planting(plot.level, plot.root).orElseThrow().type().equals(id("cauliflower")), "Explicit mutation recipe did not produce its native child: " + result + " " + StarfieldCompat.planting(plot.level, plot.root));
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void seasonSoilSpaceAndSingleSticksRejectSafely(GameTestHelper helper) {
        try (var plot = new Plot(helper, "gates", 0)) {
            plot.parent(plot.root.west(), "potato", true, 4, 2); plot.parent(plot.root.east(), "potato", false, 4, 2);
            var target = plot.target(CropStickVariant.OBSIDIAN);
            StardewTimeManager.get().setCurrentSeason(3);
            helper.assertTrue(!breed(target, 8), "Wrong season ignored"); StardewTimeManager.get().setCurrentSeason(0);
            plot.level.setBlock(plot.root.below(), Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(!breed(target, 8), "Unsuitable native soil ignored"); plot.level.setBlock(plot.root.below(), soil(0), 3);
            plot.level.setBlock(plot.root, plot.level.getBlockState(plot.root).setValue(CropBlock.CROP_STATE, CropState.SINGLE_STICKS), 3);
            helper.assertTrue(!breed((CropBlockEntity) plot.level.getBlockEntity(plot.root), 8), "Single sticks bred native child");
            plot.clearChild(); plot.target(CropStickVariant.OBSIDIAN);
            plot.remove(plot.root.west()); plot.remove(plot.root.east());
            StardewTimeManager.get().setCurrentSeason(1);
            plot.parent(plot.root.west(), "corn", true, 4, 2); plot.parent(plot.root.east(), "corn", false, 4, 2);
            plot.level.setBlock(plot.root.above(), Blocks.STONE.defaultBlockState(), 3);
            target = (CropBlockEntity) plot.level.getBlockEntity(plot.root); var before = target.saveWithoutMetadata(plot.level.registryAccess());
            helper.assertTrue(!breed(target, 8) && plot.level.getBlockState(plot.root.above()).is(Blocks.STONE)
                    && before.equals(target.saveWithoutMetadata(plot.level.registryAccess())), "Tall child overwrote upper obstruction/doubles");
            plot.level.setBlock(plot.root.above(), Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(breed(target, 8) && StarfieldCompat.planting(plot.level, plot.root.above()).orElseThrow().plantingId().equals(StarfieldCompat.planting(plot.level, plot.root).orElseThrow().plantingId()), "Tall child has invalid halves/UUID");
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void canceledAndThrowingPlacementRestoreDoubleSticks(GameTestHelper helper) {
        try (var plot = new Plot(helper, "rollback", 1)) {
            plot.parent(plot.root.west(), "corn", true, 4, 2); plot.parent(plot.root.east(), "corn", false, 4, 2);
            var original = plot.target(CropStickVariant.IRON).saveWithoutMetadata(plot.level.registryAccess());
            for (boolean throwsFailure : List.of(false, true)) {
                Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> {
                    if (event.getLevel() == plot.level && event.getPos().equals(plot.root)) {
                        if (throwsFailure) throw new IllegalStateException("Expected breeding placement failure");
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(cancel);
                try { helper.assertTrue(!breed((CropBlockEntity) plot.level.getBlockEntity(plot.root), 13), "Rejected placement succeeded"); }
                finally { NeoForge.EVENT_BUS.unregister(cancel); }
                var restored = (CropBlockEntity) plot.level.getBlockEntity(plot.root);
                helper.assertTrue(restored != null && restored.isCrossCropSticks() && original.equals(restored.saveWithoutMetadata(plot.level.registryAccess()))
                        && plot.level.getBlockState(plot.root.above()).isAir()
                        && StarfieldPlantData.get(plot.level).get(GlobalPos.of(plot.level.dimension(), plot.root)).isEmpty()
                        && CropGrowthManager.get(plot.level).getState(plot.level, plot.root) == null, "Rejected placement lost doubles or leaked native progress/identity");
            }
            helper.assertTrue(breed((CropBlockEntity) plot.level.getBlockEntity(plot.root), 13), "Rollback did not allow later valid offspring");
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void allFiftySpeciesCloneAsNativeBlocksAndPersistGenome(GameTestHelper helper) {
        try (var plot = new Plot(helper, "catalog", 0)) {
            String locations = com.stardew.craft.interior.InteriorRegionRegistry.getCachedJson();
            var table = com.google.gson.JsonParser.parseString(locations).getAsJsonObject();
            var indoor = new com.google.gson.JsonObject();
            indoor.add("min", com.google.gson.JsonParser.parseString("["+(plot.root.getX()-2)+","+(plot.root.getY()-2)+","+(plot.root.getZ()-2)+"]"));
            indoor.add("max", com.google.gson.JsonParser.parseString("["+(plot.root.getX()+2)+","+(plot.root.getY()+3)+","+(plot.root.getZ()+2)+"]"));
            indoor.addProperty("indoor",true);indoor.addProperty("priority",1000000);
            table.add("agricraft:breeding_cactus_probe",indoor);
            com.stardew.craft.interior.InteriorRegionRegistry.applyFromJson(table.toString());
            boolean old = CoreConfig.cloneMutations; CoreConfig.cloneMutations = false;
            try {
                for (var species : StarfieldCompat.species()) {
                    plot.remove(plot.root.west()); plot.clearChild();
                    var block = (StardewCropBlock) BuiltInRegistries.BLOCK.get(species.id());
                    boolean allowed = false;
                    for (int season = 0; season < 4 && !allowed; season++) { StardewTimeManager.get().setCurrentSeason(season); allowed = block.canPlantAt(plot.level, plot.root); }
                    helper.assertTrue(allowed, "No native season for " + species.id());
                    plot.nativeParent(plot.root.west(), species.id(), false, 5, 2);
                    var parent = StarfieldCompat.planting(plot.level, plot.root.west()).orElseThrow();
                    boolean spawned = false;
                    for (int i = 0; i < 200 && !spawned; i++) spawned = breed(plot.target(CropStickVariant.OBSIDIAN), i);
                    helper.assertTrue(spawned, "Species could not clone: " + species.id());
                    var child = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
                    helper.assertTrue(child.type().equals(species.id()) && child.genome().equals(parent.genome()) && child.sticks().equals("obsidian"), "Clone lost species/genes: " + species.id());
                    var restored = StarfieldPlantData.load(StarfieldPlantData.get(plot.level).save(new CompoundTag(), plot.level.registryAccess()), plot.level.registryAccess());
                    helper.assertTrue(restored.get(GlobalPos.of(plot.level.dimension(), plot.root)).orElseThrow().encode().equals(child.encode()), "SavedData round-trip lost offspring");
                    var seed = StarfieldCompat.seedFromPlant(plot.level, plot.root).orElseThrow();
                    helper.assertTrue(StarfieldGenomes.readSeed(seed, species.id()).equals(parent.genome()), "Offspring seed lost genes");
                }
                helper.succeed();
            } finally { CoreConfig.cloneMutations = old; com.stardew.craft.interior.InteriorRegionRegistry.applyFromJson(locations); }
        }
    }

    @GameTest(batch="starfield_breeding", templateNamespace="agricraft", template="starfield_probe")
    public static void ordinaryWheatBreedsAndDisabledIntegrationDoesNotSpawnNative(GameTestHelper helper) {
        try (var plot = new Plot(helper, "ordinary", 0)) {
            plot.parent(plot.root.west(), "potato", false, 4, 2); plot.parent(plot.root.east(), "potato", true, 4, 2);
            boolean enabled = CompatConfig.enableStarfield; CompatConfig.enableStarfield = false;
            try { helper.assertTrue(!breed(plot.target(CropStickVariant.WOODEN), 5), "Disabled integration bred native crop"); }
            finally { CompatConfig.enableStarfield = enabled; }
            plot.remove(plot.root.west()); plot.remove(plot.root.east()); plot.clearChild();
            for (var pos : List.of(plot.root.west(), plot.root.east())) {
                plot.level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
                plot.level.setBlock(pos, com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState(), 3);
                var crop = (CropBlockEntity) plot.level.getBlockEntity(pos);
                var genome = StarfieldGenomes.baseline(ResourceLocation.parse("minecraft:wheat"));
                genome.getCompound("genes").getCompound(AgriStatRegistry.getInstance().fertilityStat().getId()).putInt("dom", 10);
                crop.plantGenome(AgriGenome.fromNBT(genome));
                crop.setGrowthStage(new com.agricraft.agricraft.api.crop.AgriGrowthStage(7, 8));
            }
            plot.level.setBlock(plot.root.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
            var target = plot.target(CropStickVariant.IRON);
            helper.assertTrue(breed(target, 5) && target.hasPlant() && target.getPlantId().equals("minecraft:wheat"), "Ordinary wheat crossbreeding regressed");
            helper.succeed();
        }
    }

    private static boolean breed(CropBlockEntity target, long seed) {
        return AgriApi.getMutationHandler().getActiveCrossBreedEngine().handleCrossBreedTick(target, target.streamNeighbours(), RandomSource.create(seed));
    }
    private static int countSuccesses(Plot plot, int attempts) {
        int count = 0;
        for (int i = 0; i < attempts; i++) {
            if (breed(plot.target(CropStickVariant.WOODEN), i)) count++;
            plot.clearChild();
        }
        return count;
    }
    private static void setFertility(ServerLevel level, BlockPos pos, int value) { setStat(level, pos, AgriStatRegistry.getInstance().fertilityStat().getId(), value, value); }
    private static void setStat(ServerLevel level, BlockPos pos, String stat, int dom, int rec) {
        var data = StarfieldPlantData.get(level); var entry = StarfieldCompat.planting(level, pos).orElseThrow();
        var genome = entry.genome(); genome.getCompound("genes").getCompound(stat).putInt("dom", dom); genome.getCompound("genes").getCompound(stat).putInt("rec", rec);
        // Exercise the public native-seed/trowel-independent persistence surface through SavedData reload.
        var saved = data.save(new CompoundTag(), level.registryAccess());
        for (var value : saved.getList("plants", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            if (row.getLong("root") == pos.asLong() && row.getString("dimension").equals(level.dimension().location().toString())) row.put("genome", genome);
        }
        level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE, StarfieldPlantData.load(saved, level.registryAccess()));
    }
    private static ResourceLocation id(String species) { return ResourceLocation.parse("stardewcraft:" + species + "_crop"); }
    private static BlockState soil(int moisture) { return com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, moisture); }
    private static CompoundTag snapshot(ServerLevel level, BlockPos pos) {
        var tag = StarfieldCompat.planting(level, pos).orElseThrow().encode();
        var state = CropGrowthManager.get(level).getState(level, pos);
        tag.putInt("phase", state.phase); tag.putInt("day", state.dayInPhase); tag.putInt("last", state.lastDailyDay);
        tag.putBoolean("regrowing", state.regrowing); tag.putBoolean("watered", state.lastDailyWatered);
        return tag;
    }
    private static final class Plot implements AutoCloseable {
        final ServerLevel level; final BlockPos root; final GameTestHelper helper;
        final int previousSeason; final double previousMultiplier;
        Plot(GameTestHelper helper, String name, int season) {
            this.helper = helper; level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
            var uuid = UUID.nameUUIDFromBytes(("agricraft:breeding:" + name).getBytes(StandardCharsets.UTF_8));
            var farms = FarmInstanceRegistry.get(level.getServer()); var farm = farms.getFarm(uuid);
            if (farm == null) farm = farms.createFarm(uuid, name, "Breeding probe", FarmType.STANDARD);
            farm.markInitialized(); farm.setGraceDaysLeft(0);
            root = farm.getOrigin().offset(104, 10, 104);
            previousSeason = StardewTimeManager.get().getCurrentSeason(); StardewTimeManager.get().setCurrentSeason(season);
            previousMultiplier = CoreConfig.mutationChanceMultiplier; CoreConfig.mutationChanceMultiplier = 1;
            level.getChunk(root); clearChild();
            for (var direction : Direction.Plane.HORIZONTAL) remove(root.relative(direction));
            level.setBlock(root.below(), soil(0), 3);
        }
        void parent(BlockPos pos, String species, boolean sticks, int dom, int rec) { nativeParent(pos, id(species), sticks, dom, rec); }
        void nativeParent(BlockPos pos, ResourceLocation species, boolean sticks, int dom, int rec) {
            level.setBlock(pos.below(), soil(7), 3); level.setBlock(pos, BuiltInRegistries.BLOCK.get(species).defaultBlockState(), 3);
            StarfieldCompat.planting(level, pos).orElseThrow();
            for (var stat : AgriStatRegistry.getInstance()) setStat(level, pos, stat.getId(), Math.min(dom, stat.getMax()), Math.max(rec, stat.getMin()));
            setFertility(level, pos, AgriStatRegistry.getInstance().fertilityStat().getMax());
            if (sticks) StarfieldCompat.setSticks(level, pos, "wooden");
            level.setBlock(pos, level.getBlockState(pos).setValue(StardewCropBlock.AGE, StardewCropBlock.MAX_AGE), 3);
            var growth = CropGrowthManager.get(level).getState(level, pos); growth.phase = 4; growth.dayInPhase = 2; growth.lastDailyDay = 40; growth.lastDailyWatered = true;
        }
        CropBlockEntity target(CropStickVariant variant) {
            var existing = level.getBlockEntity(root);
            if (existing instanceof CropBlockEntity crop && crop.isCrossCropSticks()) return crop;
            helper.assertTrue(level.getBlockState(root).isAir(), "Target reuse would delete offspring");
            level.setBlock(root, com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState()
                    .setValue(CropBlock.CROP_STATE, CropState.DOUBLE_STICKS).setValue(CropBlock.STICK_VARIANT, variant), 3);
            return (CropBlockEntity) level.getBlockEntity(root);
        }
        void clearChild() { remove(root); }
        void remove(BlockPos pos) { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3); }
        @Override public void close() {
            clearChild(); level.setBlock(root.below(), Blocks.AIR.defaultBlockState(), 3);
            for (var direction : Direction.Plane.HORIZONTAL) { var pos = root.relative(direction); remove(pos); FertilizerManager.get(level).removeFertilizer(level, pos.below()); level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3); }
            StardewTimeManager.get().setCurrentSeason(previousSeason); CoreConfig.mutationChanceMultiplier = previousMultiplier;
        }
    }
}
