package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.compat.ExternalHarvestSupport;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.compat.starfield.*;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.api.v1.machine.StardewMachineCycles;
import com.stardew.craft.api.v1.machine.StardewProductionPlan;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.blockentity.*;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.item.quality.QualityHelper;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.player.PlayerStardewDataAPI;
import com.stardew.craft.player.SkillType;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldAutomationGameTests {
    private record Plot(ServerLevel level, ServerPlayer player, BlockPos root, List<ItemEntity> drops,
                        Consumer<EntityJoinLevelEvent> observer, int season) implements AutoCloseable {
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(observer); drops.forEach(ItemEntity::discard);
            clean(level, root); clean(level, root.east(3));
            StardewTimeManager.get().setCurrentSeason(season);
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void manualSeedProduceMachineSeedCycleRetainsBothAlleles(GameTestHelper h) {
        try (var p = plot(h, "manual-cycle")) {
            var genome = varied("potato"); plant(h,p,"potato",genome); mature(h,p,0);
            h.assertTrue(StardewCropRuntime.harvestForPlayer(p.player,p.root,InteractionHand.MAIN_HAND,false,false).harvested(), "Manual harvest failed");
            var produce = p.drops.stream().filter(e -> e.getItem().is(item("potato"))).findFirst().orElseThrow().getItem().copy();
            h.assertTrue(envelope(produce).getCompound("genome").equals(genome), "Manual produce lost alleles");
            var maker = regularMaker(h,p,produce);
            var selected = maker.getProduct().copy(); long deadline = maker.stardewReadyAtAbsoluteMinute();
            var saved = maker.saveWithoutMetadata(p.level.registryAccess());
            var restored = maker(p,maker.getBlockPos()); restored.loadWithComponents(saved,p.level.registryAccess());
            h.assertTrue(restored.stardewReadyAtAbsoluteMinute()==deadline && same(selected,restored.getProduct()), "Machine reload changed selected seeds/deadline");
            restored.advanceDays(1); var seed = restored.harvestOne();
            h.assertTrue(StarfieldGenomes.readSeed(seed,id("potato_crop")).equals(genome) && restored.harvestOne().isEmpty(), "Seed extraction lost/duplicated genome");
            replant(h,p,seed,genome); h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void automationSeedMakerSimulationAndPartialExtractionAreStable(GameTestHelper h) {
        try (var p=plot(h,"automatic-cycle")) {
            var genome=varied("potato"); plant(h,p,"potato",genome); mature(h,p,0);
            var drops=new ArrayList<ItemStack>(); int xp=xp(p);
            h.assertTrue(AgriApi.harvest(p.level,p.root,drops::add) && xp(p)==xp, "Automation failed or granted XP");
            var produce=drops.getFirst().copyWithCount(5); var untouched=produce.copy();
            var location=regularMaker(h,p,produce).getBlockPos(); var maker=maker(p,location);
            h.assertTrue(maker.insertAutomation(produce,true).getCount()==4 && maker.getProduct().isEmpty() && same(produce,untouched), "Simulation consumed input or started cycle");
            h.assertTrue(maker.insertAutomation(produce,false).getCount()==4 && same(produce,untouched), "Automation consumption differs from one native input");
            var output=maker.getProduct().copy(); h.assertTrue(StarfieldGenomes.readSeed(output,id("potato_crop")).equals(genome), "Automated machine lost genome");
            var baseline=maker(p,location); CompatConfig.enableStarfield=false;
            try { baseline.insertAutomation(produce,false); } finally { CompatConfig.enableStarfield=true; }
            var comparable=output.copy(); StarfieldGenomes.clearHeredity(comparable);
            h.assertTrue(same(comparable,baseline.getProduct()), "Gain multiplied seed count or changed native quality");
            maker.advanceDays(1); var before=maker.getProduct().copy();
            var preview=maker.extractAutomation(1,true); h.assertTrue(preview.getCount()==1 && same(before,maker.getProduct()), "Extraction simulation mutated output");
            int total=0; ItemStack seed=ItemStack.EMPTY;
            while (!maker.getProduct().isEmpty()) { seed=maker.extractAutomation(1,false); total+=seed.getCount(); h.assertTrue(StarfieldGenomes.readSeed(seed,id("potato_crop")).equals(genome), "Partial extraction lost genotype"); }
            h.assertTrue(total==output.getCount() && maker.extractAutomation(64,false).isEmpty(), "Seed output duplicated");
            replant(h,p,seed,genome); h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void nativeAncientAndMixedOutcomesNeverReceiveForeignGenome(GameTestHelper h) {
        try(var p=plot(h,"exceptional-seeds")) {
            var produce=produce(p,"potato"); var seen=new HashSet<String>();
            for(int i=0;i<4096 && seen.size()<3;i++) {
                var pos=p.root.offset(i%64,4,i/64); var nativeMaker=maker(p,pos);
                CompatConfig.enableStarfield=false;
                try { nativeMaker.insertAutomation(produce,false); } finally { CompatConfig.enableStarfield=true; }
                var expected=nativeMaker.getProduct().copy(); var actual=maker(p,pos); actual.insertAutomation(produce,false);
                var result=actual.getProduct().copy(); var comparable=result.copy(); StarfieldGenomes.clearHeredity(comparable);
                h.assertTrue(same(expected,comparable), "Compatibility rerolled native seed output");
                var path=BuiltInRegistries.ITEM.getKey(result.getItem()).getPath(); seen.add(path);
                boolean matching=result.is(item("potato_seeds"));
                h.assertTrue(result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains(StarfieldGenomes.SEED_KEY)==matching, "Exceptional seeds inherited potato genome");
            }
            h.assertTrue(seen.containsAll(List.of("potato_seeds","ancient_fruit_seeds","mixed_seeds")), "Did not exercise all native seed-maker outcomes: "+seen);
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void unknownCorruptAndMismatchedProduceFailBeforeConsumption(GameTestHelper h) {
        try(var p=plot(h,"invalid-produce")) {
            for(int mode=0;mode<4;mode++) {
                var stack=produce(p,"potato"); var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
                var e=data.getCompound(StarfieldGenomes.PRODUCE_KEY);
                if(mode==0)e.putInt("schema",99);
                if(mode==1)e.putString("produce","stardewcraft:corn");
                if(mode==2)e.putString("type","stardewcraft:corn_crop");
                if(mode==3)e.getCompound("genome").getCompound("genes").remove("gain");
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data)); var before=stack.copy(); var maker=maker(p,p.root.above(5+mode));
                h.assertTrue(!maker.tryInsert(stack,null) && maker.getProduct().isEmpty() && same(stack,before), "Bad input was consumed or started a cycle");
                h.assertTrue(same(maker.insertAutomation(stack,true),before) && same(maker.insertAutomation(stack,false),before), "Automation accepted invalid heredity");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void ordinaryProduceAndNativeExceptionalMappingsStayNative(GameTestHelper h) {
        try(var p=plot(h,"ordinary-mappings")) {
            for(var name:List.of("potato","sweet_gem_berry","unmilled_rice","qi_fruit")) {
                var expected=com.stardew.craft.item.artisan.SeedMakerOutputResolver.resolve(item(name));
                h.assertTrue(expected!=null,"Missing pinned native seed mapping: "+name);
                var stack=new ItemStack(item(name)); var maker=regularMaker(h,p,stack);
                h.assertTrue(maker.getProduct().is(expected) && !maker.getProduct().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains(StarfieldGenomes.SEED_KEY),"Untagged native produce invented genotype");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void nativeAutomationKeepsQualityExtrasRandomnessAndNoExperience(GameTestHelper h) {
        try(var p=plot(h,"native-automation")) {
            for(var name:List.of("potato","sunflower","wheat")) {
                int season=name.equals("potato")?0:1;
                plant(h,p,name,StarfieldGenomes.baseline(id(name+"_crop"))); mature(h,p,season);
                var other=p.root.east(3); p.level.setBlock(other.below(),p.level.getBlockState(p.root.below()),3);
                p.level.setBlock(other,p.level.getBlockState(p.root),3);
                var growth=CropGrowthManager.get(p.level).getState(p.level,p.root);
                var copy = new CropGrowthManager.CropGrowthState(); copy.phase=growth.phase; copy.dayInPhase=growth.dayInPhase; copy.sourcePhases=growth.sourcePhases; copy.sourcePhaseVersion=growth.sourcePhaseVersion;
                CropGrowthManager.get(p.level).restoreGrowthState(p.level,other,copy);
                StarfieldCompat.planting(p.level,other).orElseThrow();
                StarfieldGeneTestData.stat(p.level,p.root,"gain",AgriStatRegistry.getInstance().gainStat().getMax());
                var expected=new ArrayList<ItemStack>(); var actual=new ArrayList<ItemStack>(); int xp=xp(p);
                p.level.random.setSeed(927); CompatConfig.enableStarfield=false;
                try { h.assertTrue(StardewCropRuntime.harvestForAutomation(p.level,other,0,expected::add).harvested(),"Native reference automation failed"); }
                finally { CompatConfig.enableStarfield=true; }
                long next=p.level.random.nextLong(); p.level.random.setSeed(927);
                var result=StardewCropRuntime.harvestForAutomation(p.level,p.root,0,actual::add);
                h.assertTrue(result.harvested() && result.farmingExperience()==0 && xp(p)==xp && p.level.random.nextLong()==next,"Automation changed XP/random rolls");
                var nativeCounts=counts(p,expected); var geneticCounts=counts(p,actual);
                h.assertTrue(nativeCounts.keySet().equals(geneticCounts.keySet()),"Automation changed native quality/composition");
                for(var key:nativeCounts.keySet()) {
                    boolean primary=key.contains("stardewcraft:"+name+"\"");
                    h.assertTrue(geneticCounts.get(key)==nativeCounts.get(key)*(primary?2:1),"Automation multiplied side products or missed gain: "+key);
                }
                for(var stack:actual) h.assertTrue(envelope(stack).isEmpty()!=stack.is(item(name)),"Side product received crop genotype");
                h.assertTrue(!AgriApi.harvest(p.level,p.root,actual::add),"Immediate repeat harvest succeeded");
                clean(p.level,other);
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void upperPartHarvestRegrowsExactlyOnceWithSameIdentity(GameTestHelper h) {
        try(var p=plot(h,"corn-regrow")) {
            plant(h,p,"corn",varied("corn")); mature(h,p,1);
            var identity=StarfieldCompat.planting(p.level,p.root).orElseThrow(); var output=new ArrayList<ItemStack>(); int xp=xp(p);
            h.assertTrue(AgriApi.harvest(p.level,p.root.above(),output::add),"Upper part automation failed");
            int count=output.size();
            h.assertTrue(!AgriApi.harvest(p.level,p.root,output::add) && !AgriApi.harvest(p.level,p.root.above(),output::add) && output.size()==count,"Multipart/regrow duplicated output");
            h.assertTrue(CropGrowthManager.get(p.level).getState(p.level,p.root).dayInPhase==2 && StarfieldCompat.planting(p.level,p.root).orElseThrow().equals(identity) && xp(p)==xp,"Regrow changed genome/identity/XP");
            settle(p,101,1); h.assertTrue(!AgriApi.harvest(p.level,p.root,output::add),"Early regrow harvest succeeded");
            settle(p,102,1); h.assertTrue(AgriApi.harvest(p.level,p.root.above(),output::add),"Native regrow did not mature");
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void immatureAndUnsupportedPlantsCannotBeCutThroughFallback(GameTestHelper h) {
        try(var p=plot(h,"refusals")) {
            plant(h,p,"potato",varied("potato")); var output=new ArrayList<ItemStack>();
            h.assertTrue(ExternalHarvestSupport.inspect(p.level,p.root)==ExternalHarvestSupport.Availability.NOT_READY && !AgriApi.harvest(p.level,p.root,output::add),"Immature crop harvested");
            for(var block:List.of(ModBlocks.FORAGE_WINTER_ROOT.get(),ModBlocks.GIANT_CAULIFLOWER.get())) {
                clean(p.level,p.root); p.level.setBlock(p.root.below(),Blocks.DIRT.defaultBlockState(),3); p.level.setBlock(p.root,block.defaultBlockState(),2);
                var before=p.level.getBlockState(p.root);
                h.assertTrue(ExternalHarvestSupport.inspect(p.level,p.root)==ExternalHarvestSupport.Availability.UNSUPPORTED && !AgriApi.harvest(p.level,p.root,output::add) && before.equals(p.level.getBlockState(p.root)) && output.isEmpty(),"Unsupported native tool path was bypassed");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void nativeMutationFailureRestoresCropWithoutPublishingStagedOutput(GameTestHelper h) {
        try(var p=plot(h,"automation-rollback")) {
            plant(h,p,"corn",varied("corn")); mature(h,p,1);
            var identity=StarfieldCompat.planting(p.level,p.root).orElseThrow(); var lower=p.level.getBlockState(p.root); var upper=p.level.getBlockState(p.root.above());
            var growth=CropGrowthManager.get(p.level).getState(p.level,p.root); int phase=growth.phase,day=growth.dayInPhase;
            var out=new ArrayList<ItemStack>();
            var result=StarfieldCompat.automation(p.level,p.root,out::add,sink -> {
                sink.accept(new ItemStack(item("corn"))); p.level.setBlock(p.root,Blocks.AIR.defaultBlockState(),3);
                throw new IllegalStateException("Injected automation failure after native mutation");
            });
            h.assertTrue(result.isEmpty() && out.isEmpty() && lower.equals(p.level.getBlockState(p.root)) && upper.equals(p.level.getBlockState(p.root.above())) && StarfieldCompat.planting(p.level,p.root).orElseThrow().equals(identity),"Failed automation lost parts/genome or published output");
            var restored=CropGrowthManager.get(p.level).getState(p.level,p.root);
            h.assertTrue(restored.phase==phase && restored.dayInPhase==day && AgriApi.harvest(p.level,p.root,out::add),"Rollback lost maturity or prevented retry"); h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void throwingInventorySinkNeverRestoresHarvestedPlantForDuplication(GameTestHelper h) {
        try(var p=plot(h,"sink-failure")) {
            plant(h,p,"potato",varied("potato")); mature(h,p,0); var accepted=new ArrayList<ItemStack>();
            boolean failed=false;
            try { AgriApi.harvest(p.level,p.root,stack -> { accepted.add(stack); throw new IllegalStateException("Injected inventory failure"); }); }
            catch(IllegalStateException expected) { failed=true; }
            h.assertTrue(failed && !accepted.isEmpty() && !AgriApi.harvest(p.level,p.root,accepted::add),"Throwing sink restored crop and permitted duplicate inventory output"); h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void finalAddonPlanCannotAttachPotatoGenomeToCornSeeds(GameTestHelper h) {
        try(var p=plot(h,"addon-plan")) {
            var marker="iteration12-foreign-plan";
            StardewMachineCycles.registerPlan(ResourceLocation.parse("agricraft:"+marker),0,(context,plan) -> {
                if(!context.input().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBoolean(marker))return Optional.of(plan);
                var out=new ItemStack(item("corn_seeds"),3); out.set(DataComponents.CUSTOM_DATA,context.input().get(DataComponents.CUSTOM_DATA));
                QualityHelper.setQuality(out,QualityHelper.GOLD); return Optional.of(new StardewProductionPlan(out,17));
            });
            var input=produce(p,"potato"); var data=input.get(DataComponents.CUSTOM_DATA).copyTag(); data.putBoolean(marker,true); input.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            var maker=maker(p,p.root.above(10)); h.assertTrue(maker.insertAutomation(input,false).isEmpty(),"Addon plan rejected");
            var out=maker.getProduct(); h.assertTrue(out.is(item("corn_seeds")) && out.getCount()==3 && envelope(out).isEmpty() && !out.get(DataComponents.CUSTOM_DATA).copyTag().contains(StarfieldGenomes.SEED_KEY) && QualityHelper.getQuality(out)==QualityHelper.GOLD && maker.getRemainingAbsMinutes()==17,"Final plan changed count/time/quality or carried foreign genome"); h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void wineAndDriedFruitKeepNativeQualityWithoutHeredity(GameTestHelper h) {
        try(var p=plot(h,"artisan-output")) {
            var input=produce(p,"grape"); input.setCount(5); QualityHelper.setQuality(input,QualityHelper.IRIDIUM);
            for(int mode=0;mode<2;mode++) {
                var pos=p.root.above(6+mode); TimedProductionBlockEntity actual= mode==0?new KegBlockEntity(pos,ModBlocks.KEG.get().defaultBlockState()):new DehydratorBlockEntity(pos,ModBlocks.DEHYDRATOR.get().defaultBlockState()); actual.setLevel(p.level);
                TimedProductionBlockEntity baseline= mode==0?new KegBlockEntity(pos,ModBlocks.KEG.get().defaultBlockState()):new DehydratorBlockEntity(pos,ModBlocks.DEHYDRATOR.get().defaultBlockState()); baseline.setLevel(p.level);
                p.level.random.setSeed(472); CompatConfig.enableStarfield=false;
                try { if(mode==0)((KegBlockEntity)baseline).insertAutomation(input,false); else ((DehydratorBlockEntity)baseline).insertAutomation(input,false); } finally{CompatConfig.enableStarfield=true;}
                p.level.random.setSeed(472);
                if(mode==0)((KegBlockEntity)actual).insertAutomation(input,false); else ((DehydratorBlockEntity)actual).insertAutomation(input,false);
                h.assertTrue(!actual.stardewOutput().isEmpty() && same(actual.stardewOutput(),baseline.stardewOutput()) && envelope(actual.stardewOutput()).isEmpty() && actual.stardewReadyAtAbsoluteMinute()==baseline.stardewReadyAtAbsoluteMinute(),"Artisan machine changed native output/quality/time or inherited genotype");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_automation", templateNamespace="agricraft", template="starfield_probe")
    public static void plantedWildForageSeedMakerKeepsSourceSeasonGenome(GameTestHelper h) {
        try(var p=plot(h,"forage-cycle")) {
            var type=id("spring_wild_seed_crop"); var genome=StarfieldGenomes.baseline(type);
            genome.getCompound("genes").getCompound("gain").putInt("rec",AgriStatRegistry.getInstance().gainStat().getMax());
            var seed=new ItemStack(item("spring_seeds")); StarfieldGenomes.writeSeed(seed,type,genome);
            StardewTimeManager.get().setCurrentSeason(0);clean(p.level,p.root);plantSeed(h,p,seed,p.root);
            for(int day=1;day<=7;day++)settle(p,day,0);
            h.assertTrue(StarfieldCompat.planting(p.level,p.root).orElseThrow().genome().equals(genome),"Wild transformation lost source genome");
            h.assertTrue(p.level.getBlockState(p.root).useWithoutItem(p.level,p.player,new BlockHitResult(Vec3.atCenterOf(p.root),Direction.UP,p.root,false)).consumesAction(),"Planted forage pickup failed");
            var produce=p.drops.stream().filter(e -> !e.isRemoved() && !envelope(e.getItem()).isEmpty()).findFirst().orElseThrow().getItem();
            var maker=regularMaker(h,p,produce);maker.advanceDays(1);var back=maker.harvestOne();
            h.assertTrue(back.is(item("spring_seeds")) && StarfieldGenomes.readSeed(back,type).equals(genome),"Forage seeds inherited wrong season or lost alleles");
            clean(p.level,p.root);plantSeed(h,p,back.copyWithCount(1),p.root);
            h.assertTrue(StarfieldCompat.planting(p.level,p.root).orElseThrow().type().equals(type) && StarfieldCompat.planting(p.level,p.root).orElseThrow().genome().equals(genome),"Forage-seed loop changed source species");h.succeed();
        }
    }

    @GameTest(batch="starfield_automation_restart", templateNamespace="agricraft", template="starfield_probe", timeoutTicks=200)
    public static void realJvmRestartPreservesNativeMachineOutputDeadlineAndSingleSeedExtraction(GameTestHelper h) {
        var phase=System.getProperty("agricraft.starfield.restartPhase","");if(phase.isEmpty()){h.succeed();return;}
        var p=plot(h,"restart-machine"); var chunk=new net.minecraft.world.level.ChunkPos(p.root);
        boolean alreadyForced=p.level.getForcedChunks().contains(chunk.toLong()); if(!alreadyForced)p.level.setChunkForced(chunk.x,chunk.z,true);
        var witness=java.nio.file.Path.of(System.getProperty("agricraft.starfield.restartWitness")).resolveSibling("iteration12-restart-witness.nbt");
        h.runAfterDelay(40,() -> {try {
            if(phase.equals("write")) {
                var genome=varied("potato");plant(h,p,"potato",genome);mature(h,p,0);var harvested=new ArrayList<ItemStack>();
                h.assertTrue(AgriApi.harvest(p.level,p.root,harvested::add),"Restart fixture automation failed");
                var selected=regularMaker(h,p,harvested.getFirst());var pos=selected.getBlockPos();p.level.getChunk(pos);
                p.level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                p.level.setBlock(pos,ModBlocks.SEED_MAKER.get().defaultBlockState(),3);var machine=(SeedMakerBlockEntity)p.level.getBlockEntity(pos);
                h.assertTrue(machine!=null && machine.insertAutomation(harvested.getFirst(),false).getCount()==harvested.getFirst().getCount()-1
                        && !machine.getProduct().isEmpty(),"Actual machine fixture did not start or consume one input");
                var expected=new CompoundTag();expected.putLong("position",pos.asLong());expected.putLong("deadline",machine.stardewReadyAtAbsoluteMinute());expected.put("product",machine.getProduct().save(p.level.registryAccess()));expected.put("genome",genome);
                net.minecraft.nbt.NbtIo.writeCompressed(expected,witness);
                h.assertTrue(StarfieldCompat.planting(p.level,p.root).isEmpty(),"Automated one-shot crop retained sidecar");
            } else {
                var expected=net.minecraft.nbt.NbtIo.readCompressed(witness,net.minecraft.nbt.NbtAccounter.unlimitedHeap());var pos=BlockPos.of(expected.getLong("position"));p.level.getChunk(pos);
                var block=p.level.getBlockEntity(pos);h.assertTrue(block instanceof SeedMakerBlockEntity,"Saved world lost native seed maker");var machine=(SeedMakerBlockEntity)block;
                h.assertTrue(machine.stardewReadyAtAbsoluteMinute()==expected.getLong("deadline") && machine.getProduct().save(p.level.registryAccess()).equals(expected.get("product")),"Real restart rerolled seeds/genome/count/deadline");
                h.assertTrue(StarfieldCompat.planting(p.level,p.root).isEmpty(),"Real restart resurrected harvested source");machine.advanceDays(1);var seed=machine.harvestOne();
                h.assertTrue(StarfieldGenomes.readSeed(seed,id("potato_crop")).equals(expected.getCompound("genome")) && machine.harvestOne().isEmpty(),"Restart lost inherited genome or duplicated extraction");
                StardewTimeManager.get().setCurrentSeason(0);replant(h,p,seed,expected.getCompound("genome"));p.level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            }
            h.succeed();
        } catch(Exception failure){throw new RuntimeException(failure);}
        finally {
            if(!alreadyForced)p.level.setChunkForced(chunk.x,chunk.z,false);
            if(phase.equals("write")){NeoForge.EVENT_BUS.unregister(p.observer);StardewTimeManager.get().setCurrentSeason(p.season);}else p.close();
        }});
    }

    private static Plot plot(GameTestHelper h,String name) {
        var level=h.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        var owner=UUID.nameUUIDFromBytes(("agricraft:automation:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var farms=FarmInstanceRegistry.get(level.getServer()); var farm=farms.getFarm(owner);
        if(farm==null)farm=farms.createFarm(owner,name,"Automation probe",FarmType.STANDARD);
        farm.markInitialized(); farm.setGraceDaysLeft(0); var root=farm.getOrigin().offset(40,30,40); level.getChunk(root);
        var player=FakePlayerFactory.get(level,new GameProfile(owner,"AutomationProbe")); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var drops=new ArrayList<ItemEntity>(); Consumer<EntityJoinLevelEvent> observer=e -> { if(e.getLevel()==level && e.getEntity() instanceof ItemEntity item && new AABB(root).inflate(4).contains(item.position()))drops.add(item); };
        NeoForge.EVENT_BUS.addListener(observer); return new Plot(level,player,root,drops,observer,StardewTimeManager.get().getCurrentSeason());
    }
    private static void plant(GameTestHelper h,Plot p,String name,CompoundTag genome) {
        clean(p.level,p.root); StardewTimeManager.get().setCurrentSeason(name.equals("potato")?0:1);
        var seed=new ItemStack(BuiltInRegistries.ITEM.get(StarfieldCompat.species(id(name+"_crop")).orElseThrow().seed())); StarfieldGenomes.writeSeed(seed,id(name+"_crop"),genome);
        plantSeed(h,p,seed,p.root);
    }
    private static void plantSeed(GameTestHelper h,Plot p,ItemStack seed,BlockPos root) {
        p.level.setBlock(root.below(),ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
        p.player.setItemInHand(InteractionHand.MAIN_HAND,seed); p.player.setPos(root.getX()+0.5,root.getY(),root.getZ()+2.5);
        var context=new UseOnContext(p.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(root.below()),Direction.UP,root.below(),false));
        var result=seed.useOn(context);
        h.assertTrue(result.consumesAction(),"Native seed planting failed: "+result+" at "+root+" item="+seed+" season="+StardewTimeManager.get().getCurrentSeason()+" soil="+p.level.getBlockState(root.below())+" root="+p.level.getBlockState(root));
        p.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
    private static void replant(GameTestHelper h,Plot p,ItemStack seed,CompoundTag genome) {
        clean(p.level,p.root); plantSeed(h,p,seed.copyWithCount(1),p.root);
        h.assertTrue(StarfieldCompat.planting(p.level,p.root).orElseThrow().genome().equals(genome),"Produce-machine-seed cycle lost both alleles");
    }
    private static void mature(GameTestHelper h,Plot p,int season) {
        for(int day=1;day<=50 && !StarfieldCompat.inspect(p.level,p.root).orElseThrow().mature();day++)settle(p,day,season);
        h.assertTrue(StarfieldCompat.inspect(p.level,p.root).orElseThrow().mature(),"Native crop did not mature");
    }
    private static void settle(Plot p,int day,int season) {
        p.level.setBlock(p.root.below(),p.level.getBlockState(p.root.below()).setValue(FarmBlock.MOISTURE,7),3);
        CropGrowthManager.get(p.level).settleCrops(p.level,List.of(GlobalPos.of(p.level.dimension(),p.root)),day,season,false);
    }
    private static CompoundTag varied(String name) {
        var genome=StarfieldGenomes.baseline(id(name+"_crop"));
        for(var stat:AgriStatRegistry.getInstance()) { var pair=genome.getCompound("genes").getCompound(stat.getId()); pair.putInt("dom",stat.getMax()); pair.putInt("rec",stat.getMin()); }
        return genome;
    }
    private static ItemStack produce(Plot p,String name) {
        var stack=new ItemStack(item(name)); var type=id(name+"_crop");
        StarfieldGenomes.writeProduce(stack,new StarfieldPlantData.Entry(UUID.randomUUID(),type,varied(name),new CompoundTag(),"")); return stack;
    }
    private static SeedMakerBlockEntity maker(Plot p,BlockPos pos) { var result=new SeedMakerBlockEntity(pos,ModBlocks.SEED_MAKER.get().defaultBlockState()); result.setLevel(p.level); return result; }
    private static SeedMakerBlockEntity regularMaker(GameTestHelper h,Plot p,ItemStack input) {
        var expected=com.stardew.craft.item.artisan.SeedMakerOutputResolver.resolve(input.getItem());
        for(int i=0;i<128;i++) { var result=maker(p,p.root.offset(i,8,0)); result.insertAutomation(input,false); if(result.getProduct().is(expected))return result; }
        throw new IllegalStateException("No corresponding native seeds for "+input);
    }
    private static Map<String,Integer> counts(Plot p,List<ItemStack> stacks) {
        var result=new HashMap<String,Integer>();
        for(var stack:stacks) { var copy=stack.copyWithCount(1); StarfieldGenomes.clearHeredity(copy); result.merge(copy.save(p.level.registryAccess()).toString(),stack.getCount(),Integer::sum); } return result;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.parse("stardewcraft:"+path); }
    private static net.minecraft.world.item.Item item(String name) { return BuiltInRegistries.ITEM.get(id(name)); }
    private static boolean same(ItemStack a,ItemStack b) { return a.getCount()==b.getCount() && ItemStack.isSameItemSameComponents(a,b); }
    private static CompoundTag envelope(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(StarfieldGenomes.PRODUCE_KEY); }
    private static int xp(Plot p) { return PlayerStardewDataAPI.getSkillExperience(p.player,SkillType.FARMING); }
    private static void clean(ServerLevel level,BlockPos root) { level.getChunk(root); level.setBlock(root,Blocks.AIR.defaultBlockState(),3); level.setBlock(root.above(),Blocks.AIR.defaultBlockState(),3); level.setBlock(root.below(),Blocks.AIR.defaultBlockState(),3); }
}
