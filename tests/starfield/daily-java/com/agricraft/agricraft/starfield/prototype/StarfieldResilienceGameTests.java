package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.compat.starfield.*;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.api.v1.internal.giant.GiantCropGrowth;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.farming.SeasonLocationRules;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldResilienceGameTests {
    private static final Set<GlobalPos> PROTECTED = new HashSet<>();
    private static final Set<GlobalPos> SUMMER_METADATA = new HashSet<>();
    private static boolean ruleRegistered;
    private static boolean providerRegistered;

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void boundedGraceAndDeterministicSoilTable(GameTestHelper helper) {
        int[] days = {0,0,0,1,1,1,2,2,2,3};
        for (int i=1; i<=10; i++) helper.assertTrue(NativeCropGeneRules.resistanceDays(NativeCropGeneRules.normalized(i,1,10))==days[i-1], "Grace table differs at "+i);
        helper.assertTrue(NativeCropGeneRules.graceRemaining(29,3)==3 && NativeCropGeneRules.graceRemaining(31,3)==1
                && NativeCropGeneRules.graceRemaining(32,3)==0 && NativeCropGeneRules.graceRemaining(113,3)==3, "Historical/year-wrap boundary wrong");
        int medium=0;
        for (int day=1; day<=10000; day++) {
            helper.assertTrue(!NativeCropGeneRules.recoverSoilSkip(day,0) && NativeCropGeneRules.recoverSoilSkip(day,1), "Endpoints changed");
            boolean low = NativeCropGeneRules.recoverSoilSkip(day,0.25), mid = NativeCropGeneRules.recoverSoilSkip(day,0.5), high = NativeCropGeneRules.recoverSoilSkip(day,0.75);
            helper.assertTrue((!low || mid) && (!mid || high) && mid==NativeCropGeneRules.recoverSoilSkip(day,0.5), "Non-monotonic or rerolled soil sample");
            if(mid) medium++;
        }
        helper.assertTrue(medium>4700 && medium<5300, "Daily soil samples not balanced: "+medium); helper.succeed();
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void strengthRemovesOnlyInfertilePenaltyAndKeepsDryDates(GameTestHelper helper) {
        try(var plot=new Plot(helper,"soil")) {
            for (boolean maximum : List.of(false,true)) {
                plot.plant("potato",0,true);
                if(maximum) plot.maximum("strength");
                int expected=maximum?6:9;
                helper.assertTrue(StarfieldCompat.inspection(plot.level,plot.attached).orElseThrow().getInt("remaining")==expected,"Soil forecast ignores strength");
                var start=plot.progress(plot.attached);
                plot.day(1,0,false,false); helper.assertTrue(start.equals(plot.progress(plot.attached)),"Dry day advanced soil/progress");
                for(int wet=1;wet<=expected;wet++) {
                    plot.day(wet+1,0,true,false); plot.equal(helper);
                    helper.assertTrue(plot.mature(plot.attached)==(wet==expected),"Infertile maturity wrong: "+maximum+" day="+wet);
                    var before=plot.checkpoint(plot.attached); plot.day(wet+1,0,true,false);
                    helper.assertTrue(before.equals(plot.checkpoint(plot.attached)),"Strength repeated a date");
                }
            }
            plot.plant("potato",0,false); plot.maximum("strength");
            for(int day=1;day<=6;day++){plot.day(day,0,true,false);helper.assertTrue(plot.mature(plot.plain)==(day==6),"Strength accelerated normal soil");}
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void intermediateSoilMatchesForecastAndReloadWithoutWorldRng(GameTestHelper helper) {
        try(var plot=new Plot(helper,"soil-reload")) {
            plot.plant("cauliflower",0,true); plot.stat("strength",5);
            int expected=StarfieldCompat.inspection(plot.level,plot.attached).orElseThrow().getInt("remaining");
            helper.assertTrue(expected>=12 && expected<=18,"Intermediate soil forecast outside native/max bounds");
            for(int day=1;day<=expected;day++) {
                plot.level.random.setSeed(412); var expectedRandom=net.minecraft.util.RandomSource.create(412).nextLong();
                plot.day(day,0,true,false); plot.equal(helper);
                helper.assertTrue(plot.level.random.nextLong()==expectedRandom,"Strength consumed world randomness");
                helper.assertTrue(plot.mature(plot.attached)==(day==expected),"Intermediate forecast differs from real days");
                plot.reloadSidecar(); var before=plot.checkpoint(plot.attached); plot.day(day,0,true,true);
                helper.assertTrue(before.equals(plot.checkpoint(plot.attached)),"Reload rerolled soil bonus");
            }
            helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void resistancePausesAndDiesOnFourthDayWithoutResetBySticks(GameTestHelper helper) {
        try(var plot=new Plot(helper,"season")) {
            plot.plant("potato",0,false); plot.maximum("resistance"); plot.day(28,0,true,false);
            var before=plot.progress(plot.attached); var identity=StarfieldCompat.planting(plot.level,plot.attached).orElseThrow().plantingId();
            for(int day=29;day<=31;day++) {
                plot.clock(day); plot.day(day,1,day!=30,false); plot.equal(helper);
                helper.assertTrue(before.equals(plot.progress(plot.attached)),"Grace advanced growth or infertile progress");
                var report=StarfieldCompat.inspection(plot.level,plot.attached).orElseThrow();
                helper.assertTrue(report.getBoolean("seasonPaused") && report.getInt("graceRemaining")==32-day,"Wrong grace report");
                StarfieldCompat.setSticks(plot.level,plot.attached,"");
                StarfieldCompat.setSticks(plot.level,plot.attached,day==29?"obsidian":day==30?"wooden":"iron"); plot.reloadSidecar();
                helper.assertTrue(StarfieldCompat.planting(plot.level,plot.attached).orElseThrow().plantingId().equals(identity),"Stick change replaced identity");
            }
            plot.day(32,1,true,false); plot.dead(helper); helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void matureCropCanBeHarvestedDuringGraceOnce(GameTestHelper helper) {
        try(var plot=new Plot(helper,"grace-harvest")) {
            plot.plant("potato",0,false); plot.maximum("resistance");
            for(int day=23;day<=28;day++)plot.day(day,0,true,false);
            plot.clock(29); plot.day(29,1,false,false);
            var player=plot.player();
            helper.assertTrue(StardewCropRuntime.harvestForPlayer(player,plot.attached,InteractionHand.MAIN_HAND,false,false).harvested(),"Mature grace crop could not harvest");
            helper.assertTrue(!StardewCropRuntime.harvestForPlayer(player,plot.attached,InteractionHand.MAIN_HAND,false,false).harvested()
                    && StarfieldCompat.planting(plot.level,plot.attached).isEmpty(),"Grace harvest duplicated or retained genome"); helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void regrowAndTallCropPauseAcrossWinterBoundary(GameTestHelper helper) {
        try(var plot=new Plot(helper,"regrow")) {
            plot.plant("corn",1,true); plot.maximum("strength"); plot.maximum("resistance");
            for(int day=43;day<=56;day++)plot.day(day,1,true,false);
            var player=plot.player();
            for(var root:plot.roots())helper.assertTrue(StardewCropRuntime.harvestForPlayer(player,root.above(),InteractionHand.MAIN_HAND,false,false).harvested(),"Corn harvest failed");
            plot.day(57,2,false,false); helper.assertTrue(plot.growth(plot.attached).dayInPhase==4,"Dry fall day advanced regrow");
            plot.day(58,2,true,false); helper.assertTrue(plot.growth(plot.attached).dayInPhase==3,"Maximum strength did not remove regrow soil skip");
            var before=plot.progress(plot.attached);
            for(int day=85;day<=87;day++){plot.clock(day);plot.day(day,3,true,false);plot.equal(helper);helper.assertTrue(before.equals(plot.progress(plot.attached)),"Winter grace advanced regrow");}
            plot.day(88,3,true,false); plot.dead(helper); helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void nativeProtectedPlacesAndFarmGraceKeepOriginalRules(GameTestHelper helper) {
        try(var plot=new Plot(helper,"protected")) {
            plot.plant("potato",0,false); plot.maximum("resistance");
            plot.farm.setGraceDaysLeft(2); plot.clock(29); plot.day(29,1,true,false);
            helper.assertTrue(plot.growth(plot.attached).phase==1,"Native farm grace was replaced by gene pause");
            plot.farm.setGraceDaysLeft(0); var before=plot.progress(plot.attached);
            plot.day(31,1,true,false);helper.assertTrue(before.equals(plot.progress(plot.attached)),"Gene grace restarted after native grace");
            plot.day(32,1,true,false);plot.dead(helper);
            plot.plant("potato",0,false);plot.maximum("resistance");
            if(!ruleRegistered){SeasonLocationRules.registerIgnoreSeasonsRule((level,pos)->PROTECTED.contains(GlobalPos.of(level.dimension(),pos)));ruleRegistered=true;}
            plot.roots().forEach(root->PROTECTED.add(GlobalPos.of(plot.level.dimension(),root)));
            plot.clock(40);plot.day(40,1,true,false);
            helper.assertTrue(plot.growth(plot.attached).phase==1 && !StarfieldCompat.inspection(plot.level,plot.attached).orElseThrow().getBoolean("seasonPaused"),"Protected location changed native growth");
            PROTECTED.clear();
            plot.plant("fiber",3,false);plot.maximum("resistance");plot.day(85,3,true,false);
            helper.assertTrue(plot.growth(plot.attached).phase>0 || plot.growth(plot.attached).dayInPhase>0,"Year-round crop received seasonal pause");helper.succeed();
        } finally {PROTECTED.clear();}
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void historicalDatesDoNotUseCurrentSeasonOrStartLateGrace(GameTestHelper helper) {
        try(var plot=new Plot(helper,"historical")) {
            plot.plant("potato",0,false);plot.maximum("resistance");plot.day(28,0,true,true);
            plot.clock(99);var before=plot.progress(plot.attached);
            for(int day=29;day<=31;day++){plot.day(day,1,true,true);plot.equal(helper);helper.assertTrue(before.equals(plot.progress(plot.attached)),"Offline grace used current winter");}
            plot.day(57,2,true,true);plot.dead(helper);helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void wildWinterSeedsRespectYearWrapAndBaselineDiesImmediately(GameTestHelper helper) {
        try(var plot=new Plot(helper,"winter-wild")) {
            plot.plant("winter_wild_seed",3,false);plot.maximum("resistance");plot.day(112,3,true,false);
            var before=plot.progress(plot.attached);
            for(int day=113;day<=115;day++){plot.clock(day);plot.day(day,0,true,false);helper.assertTrue(before.equals(plot.progress(plot.attached)),"Wild year-wrap crop grew during grace");}
            plot.day(116,0,true,false);plot.dead(helper);
            plot.plant("potato",0,false);plot.day(29,1,true,false);plot.dead(helper);helper.succeed();
        }
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void pausedRootsCannotParticipateInNativeGiantPass(GameTestHelper helper) {
        try(var plot=new Plot(helper,"giant")) {
            plot.plant("cauliflower",0,false);plot.maximum("resistance");
            for(int day=17;day<=28;day++)plot.day(day,0,true,false);
            plot.clock(29);plot.day(29,1,true,false);
            if(!providerRegistered) {
                com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi.registerCropProvider(ResourceLocation.parse("agricraft:resilience_probe"),1000,
                    (level,pos,state)->SUMMER_METADATA.contains(GlobalPos.of(level.dimension(),pos))
                        ? new com.stardew.craft.api.v1.agriculture.StardewCropData(List.of("summer"),List.of(1,2,4,4,1),-1,0,
                            ResourceLocation.parse("stardewcraft:grab"),ResourceLocation.parse("stardewcraft:cauliflower"),ResourceLocation.parse("stardewcraft:cauliflower_seeds")) : null);
                providerRegistered=true;
            }
            // Online native growth uses the block rule; offline growth uses provider metadata.
            plot.roots().forEach(root->SUMMER_METADATA.add(GlobalPos.of(plot.level.dimension(),root)));
            var roots=plot.roots();var statistics=GiantCropGrowth.process(plot.level,roots,new HashSet<>(roots),29,false);
            helper.assertTrue(statistics.candidates()==0 && statistics.rolls()==0 && roots.stream().allMatch(root->plot.growth(root).lastGiantDay<29),"Paused crop entered giant roll");
            var historical=GiantCropGrowth.process(plot.level,roots,new HashSet<>(roots),29,true);
            helper.assertTrue(historical.candidates()==2 && historical.rolls()>0,"Historical metadata season was replaced by current block rule");
            helper.succeed();
        } finally {SUMMER_METADATA.clear();}
    }

    @GameTest(batch="starfield_resilience", templateNamespace="agricraft", template="starfield_probe")
    public static void disabledEffectsPreserveNativeSoilAndSeasonDeath(GameTestHelper helper) {
        try(var plot=new Plot(helper,"disabled")) {
            plot.plant("potato",0,true);plot.maximum("strength");plot.maximum("resistance");
            CompatConfig.enableStarfield=false;
            plot.day(1,0,true,false);helper.assertTrue(plot.growth(plot.attached).phase==0 && plot.growth(plot.attached).infertileProgress==2,"Disabled strength changed native skip");
            plot.day(29,1,true,false);helper.assertTrue(plot.level.getBlockState(plot.attached).is(com.stardew.craft.block.ModBlocks.DEAD_CROP.get()),"Disabled resistance prevented death");helper.succeed();
        }finally{CompatConfig.enableStarfield=true;}
    }

    @GameTest(batch="starfield_resilience_restart", templateNamespace="agricraft", template="starfield_probe")
    public static void realJvmRestartKeepsSoilRemainderAndSeasonAnchor(GameTestHelper helper) throws Exception {
        String phase=System.getProperty("agricraft.starfield.restartPhase","");
        if(phase.isEmpty()){helper.succeed();return;}
        var soil=new Plot(helper,"restart-soil"); var season=new Plot(helper,"restart-season");
        var path=Path.of(System.getProperty("agricraft.starfield.dailyRestartWitness")).resolveSibling("iteration9-restart-witness.nbt");
        try {
            if(phase.equals("write")) {
                soil.plant("cauliflower",0,true);soil.stat("strength",5);soil.day(1,0,true,false);soil.day(2,0,true,false);
                season.plant("potato",0,false);season.maximum("resistance");season.day(28,0,true,false);
                season.clock(29);season.day(29,1,true,false);
                helper.assertTrue(season.growth(season.attached)!=null && soil.growth(soil.attached).infertileProgress==1,"Restart fixture missing live grace/soil remainder");
                var witness=new CompoundTag();
                for(int i=0;i<2;i++){witness.put("soil"+i,soil.checkpoint(soil.roots().get(i)));witness.put("season"+i,season.checkpoint(season.roots().get(i)));}
                Files.createDirectories(path.getParent());NbtIo.writeCompressed(witness,path);
            } else {
                var witness=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());
                for(int i=0;i<2;i++) helper.assertTrue(witness.getCompound("soil"+i).equals(soil.checkpoint(soil.roots().get(i)))
                        && witness.getCompound("season"+i).equals(season.checkpoint(season.roots().get(i))),"Real saved resilience data changed after JVM restart");
                var soilBefore=soil.checkpoint(soil.attached);var seasonBefore=season.checkpoint(season.attached);
                soil.day(2,0,true,true);season.day(29,1,true,true);
                helper.assertTrue(soilBefore.equals(soil.checkpoint(soil.attached)) && seasonBefore.equals(season.checkpoint(season.attached)),"Restart replayed processed soil/season date");
                soil.clock(3);soil.day(3,0,true,true);soil.equal(helper);
                helper.assertTrue(soil.growth(soil.attached).lastDailyDay==3 && soil.growth(soil.attached).infertileProgress==0
                        && !soilBefore.equals(soil.checkpoint(soil.attached)),"Restart lost soil remainder or next day");
                var paused=season.progress(season.attached);season.clock(30);season.day(30,1,true,true);season.equal(helper);
                helper.assertTrue(paused.equals(season.progress(season.attached)) && StarfieldCompat.inspection(season.level,season.attached).orElseThrow().getInt("graceRemaining")==2,"Restart reset season grace");
                season.clock(32);season.day(32,1,true,true);season.dead(helper);
            }
            helper.succeed();
        } finally {
            // The write process leaves real roots for normal server shutdown, never restores a witness.
            if(!phase.equals("write")){soil.close();season.close();}
            else {soil.restoreClock();}
        }
    }

    private static final class Plot implements AutoCloseable {
        final ServerLevel level;final FarmInstance farm;final UUID owner;final BlockPos plain,attached;final int year,season,day;
        Plot(GameTestHelper helper,String name){
            level=helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
            owner=UUID.nameUUIDFromBytes(("agricraft:resilience:"+name).getBytes(StandardCharsets.UTF_8));
            var registry=FarmInstanceRegistry.get(level.getServer());var existing=registry.getFarm(owner);
            farm=existing==null?registry.createFarm(owner,name,"Resilience probe",FarmType.STANDARD):existing;
            farm.markInitialized();farm.setGraceDaysLeft(0);plain=farm.getOrigin().offset(88,10,88);attached=plain.east();level.getChunk(plain);level.getChunk(attached);
            var clock=StardewTimeManager.get();year=clock.getCurrentYear();season=clock.getCurrentSeason();day=clock.getCurrentDay();
        }
        List<BlockPos> roots(){return List.of(plain,attached);}
        void plant(String species,int season,boolean poor){cleanup();StardewTimeManager.get().setCurrentSeason(season);
            for(var root:roots()) {level.setBlock(root.below(),(poor?com.stardew.craft.block.ModBlocks.INFERTILE_FARMLAND.get():com.stardew.craft.block.ModBlocks.FARMLAND.get()).defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
                level.setBlock(root,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("stardewcraft:"+species+"_crop")).defaultBlockState(),3);StarfieldCompat.planting(level,root).orElseThrow();}
            StarfieldCompat.setSticks(level,attached,"iron");}
        void stat(String id,int value){for(var root:roots())StarfieldGeneTestData.stat(level,root,id,value);}
        void maximum(String id){stat(id,AgriStatRegistry.getInstance().stream().filter(stat->stat.getId().equals(id)).findFirst().orElseThrow().getMax());}
        void day(int date,int season,boolean wet,boolean offline){for(var root:roots())level.setBlock(root.below(),level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE,wet?7:0),3);
            CropGrowthManager.get(level).settleCrops(level,roots().stream().map(root->GlobalPos.of(level.dimension(),root)).toList(),date,season,offline);}
        void clock(int date){var clock=StardewTimeManager.get();clock.setCurrentYear((date-1)/112+1);clock.setCurrentSeason(Math.floorMod((date-1)/28,4));clock.setCurrentDay(Math.floorMod(date-1,28)+1);}
        CropGrowthManager.CropGrowthState growth(BlockPos root){return CropGrowthManager.get(level).getState(level,root);}
        boolean mature(BlockPos root){return StarfieldCompat.inspect(level,root).orElseThrow().mature();}
        CompoundTag progress(BlockPos root){var growth=growth(root);var tag=new CompoundTag();tag.putInt("phase",growth.phase);tag.putInt("day",growth.dayInPhase);tag.putInt("soil",growth.infertileProgress);tag.putBoolean("regrow",growth.regrowing);return tag;}
        CompoundTag checkpoint(BlockPos root){var tag=progress(root);tag.putInt("lastDaily",growth(root).lastDailyDay);tag.put("entry",StarfieldCompat.planting(level,root).orElseThrow().encode());tag.putString("block",level.getBlockState(root).toString());tag.putString("upper",level.getBlockState(root.above()).toString());
            var rows=CropGrowthManager.get(level).save(new CompoundTag(),level.registryAccess()).getList("Crops",Tag.TAG_COMPOUND);
            for(var value:rows){var row=(CompoundTag)value;if(row.getString("Dimension").equals(level.dimension().location().toString()) && NbtUtils.readBlockPos(row,"Pos").orElse(BlockPos.ZERO).equals(root)){tag.put("native",row.copy());break;}}
            if(!tag.contains("native"))throw new IllegalStateException("Missing actual native saved row");return tag;}
        void equal(GameTestHelper helper){helper.assertTrue(progress(plain).equals(progress(attached)) && level.getBlockState(plain).equals(level.getBlockState(attached)),"Paired resilience differs");}
        void reloadSidecar(){var saved=StarfieldPlantData.get(level).save(new CompoundTag(),level.registryAccess());level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE,StarfieldPlantData.load(saved,level.registryAccess()));}
        ServerPlayer player(){var player=FakePlayerFactory.get(level,new GameProfile(owner,"ResilienceProbe"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);return player;}
        void dead(GameTestHelper helper){for(var root:roots())helper.assertTrue(level.getBlockState(root).is(com.stardew.craft.block.ModBlocks.DEAD_CROP.get()) && StarfieldCompat.planting(level,root).isEmpty()
                && !(level.getBlockState(root.above()).getBlock() instanceof StardewCropBlock),"Death retained crop/upper/sidecar");}
        void cleanup(){for(var root:roots()){level.setBlock(root,Blocks.AIR.defaultBlockState(),3);level.setBlock(root.above(),Blocks.AIR.defaultBlockState(),3);FertilizerManager.get(level).removeFertilizer(level,root.below());level.setBlock(root.below(),Blocks.AIR.defaultBlockState(),3);}}
        void restoreClock(){var clock=StardewTimeManager.get();clock.setCurrentYear(year);clock.setCurrentSeason(season);clock.setCurrentDay(day);}
        @Override public void close(){cleanup();farm.setGraceDaysLeft(0);restoreClock();}
    }
}
