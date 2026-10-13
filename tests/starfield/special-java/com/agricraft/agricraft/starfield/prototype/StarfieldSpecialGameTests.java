package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.*;
import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.crop.*;
import com.stardew.craft.block.nature.ForageBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.farming.CropPlanting;
import com.stardew.craft.greenhouse.GreenhouseManager;
import com.stardew.craft.manager.*;
import com.stardew.craft.player.*;
import com.stardew.craft.item.quality.QualityHelper;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldSpecialGameTests {
    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void allMultipartNativeModelsCyclesAndUpperHarvestOnlyOnce(GameTestHelper h) {
        try(var p=new Plot(h,"multipart")) {
            int checked=0;
            for(var species:StarfieldCompat.species().stream().filter(StarfieldCompat.Species::twoParts).toList()) {
                p.plant(species.id(),p.allowedSeason(species.id()),true);
                for(var root:p.roots()) StarfieldGeneTestData.stat(p.level,root,"growth",AgriStatRegistry.getInstance().growthStat().getMax());
                var original=p.entry(p.attached);
                h.assertTrue(StarfieldCompat.inspect(p.level,p.attached.above()).orElseThrow().root().equals(p.attached)
                        && StarfieldCompat.planting(p.level,p.attached.above()).orElseThrow().plantingId().equals(original.plantingId()),"Upper is a second planting: "+species.id());
                for(String material:List.of("obsidian","","wooden","iron")) {
                    h.assertTrue(StarfieldCompat.setSticks(p.level,p.attached.above(),material),"Upper stick action failed");
                    h.assertTrue(p.entry(p.attached).plantingId().equals(original.plantingId()) && p.entry(p.attached).genome().equals(original.genome()),"Stick action changed native identity");
                }
                int days=species.nativePhaseDays().stream().mapToInt(d->NativeCropGeneRules.duration(d,1)).sum();
                for(int day=1;day<=days;day++) {
                    p.day(day,true);p.equal(h);p.geometry(h);
                    h.assertTrue(StarfieldCompat.inspect(p.level,p.attached.above()).orElseThrow().mature()==(day==days),"Upper maturity wrong: "+species.id()+" day="+day);
                }
                var tool=species.requiresScythe()?new ItemStack(com.stardew.craft.item.ModItems.SCYTHE.get()):ItemStack.EMPTY;
                p.player.setItemInHand(InteractionHand.MAIN_HAND,tool);
                PlayerStardewDataAPI.setExperience(p.player,SkillType.FARMING,0);
                p.level.random.setSeed(913);
                h.assertTrue(StardewCropRuntime.harvestForPlayer(p.player,p.plain,InteractionHand.MAIN_HAND,species.requiresScythe(),false).harvested(),"Lower harvest failed: "+species.id());
                var nativeOutput=p.counts();int nativeXp=PlayerStardewDataAPI.getSkillExperience(p.player,SkillType.FARMING);
                p.clearDrops();PlayerStardewDataAPI.setExperience(p.player,SkillType.FARMING,0);p.level.random.setSeed(913);
                h.assertTrue(StardewCropRuntime.harvestForPlayer(p.player,p.attached.above(),InteractionHand.MAIN_HAND,species.requiresScythe(),false).harvested(),"Upper harvest failed: "+species.id());
                h.assertTrue(nativeOutput.equals(p.counts()),"Upper output differs: "+species.id());
                h.assertTrue(PlayerStardewDataAPI.getSkillExperience(p.player,SkillType.FARMING)==nativeXp,"Upper harvest duplicated/lost farming XP");
                h.assertTrue(!StardewCropRuntime.harvestForPlayer(p.player,p.attached,InteractionHand.MAIN_HAND,species.requiresScythe(),false).harvested(),"Upper/lower duplicate harvest: "+species.id());
                if(species.nativeRegrowDays()>0) {
                    p.geometry(h);h.assertTrue(p.growth(p.plain).dayInPhase==p.growth(p.attached).dayInPhase,"Upper regrow differs");
                } else h.assertTrue(p.level.getBlockState(p.attached.above()).isAir() && p.growth(p.attached)==null && StarfieldCompat.planting(p.level,p.attached).isEmpty(),"One-shot harvest retained parts/data");
                checked++;
            }
            h.assertTrue(checked==StarfieldCompat.species().stream().filter(StarfieldCompat.Species::twoParts).count(),"Incomplete multipart coverage");h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void blockedUpperCellPreservesEveryTallNativeSeed(GameTestHelper h) {
        try(var p=new Plot(h,"blocked")) {
            for(var species:StarfieldCompat.species().stream().filter(StarfieldCompat.Species::twoParts).toList()) {
                p.cleanup();p.season=p.allowedSeason(species.id());StardewTimeManager.get().setCurrentSeason(p.season);p.soil(p.attached,true);
                p.level.setBlock(p.attached.above(),Blocks.STONE.defaultBlockState(),3);
                var seed=new ItemStack(BuiltInRegistries.ITEM.get(species.seed()),3);var before=seed.copy();
                seed.useOn(p.context(p.attached.below(),seed));
                h.assertTrue(p.level.getBlockState(p.attached).isAir() && p.level.getBlockState(p.attached.above()).is(Blocks.STONE)
                        && seed.getCount()==before.getCount() && ItemStack.isSameItemSameComponents(seed,before)
                        && p.growth(p.attached)==null && StarfieldCompat.planting(p.level,p.attached).isEmpty(),"Blocked upper consumed seed/left root: "+species.id());
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void partialDestructionAndSupportLossClearEveryMultipartPlant(GameTestHelper h) {
        try(var p=new Plot(h,"remove")) {
            for(var species:StarfieldCompat.species().stream().filter(StarfieldCompat.Species::twoParts).toList()) {
                p.plant(species.id(),p.allowedSeason(species.id()),true);
                p.level.setBlock(p.plain.above(),Blocks.AIR.defaultBlockState(),3);
                p.level.setBlock(p.attached.below(),Blocks.STONE.defaultBlockState(),3);
                for(var root:p.roots()) h.assertTrue(p.level.getBlockState(root).isAir() && p.level.getBlockState(root.above()).isAir()
                        && p.growth(root)==null && StarfieldCompat.planting(p.level,root).isEmpty(),"Partial destruction left root/upper/registration: "+species.id());
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void paddyWaterRadiusRemovalAndGeneticScheduleStayNative(GameTestHelper h) {
        try(var p=new Plot(h,"paddy")) {
            for(String species:List.of("rice","taro_root")) {
                int season=species.equals("rice")?0:1;p.plant(id(species),season,false);
                for(var root:p.roots())p.level.setBlock(root.below().east(3),Blocks.WATER.defaultBlockState(),3);
                h.assertTrue(RiceCropBlock.hasNearbyPaddyWater(p.level,p.attached),"Radius-three source not detected");
                int expected=StarfieldCompat.inspection(p.level,p.attached).orElseThrow().getInt("remaining");
                for(int day=1;day<=expected;day++){p.day(day,false);p.equal(h);p.geometry(h);
                    h.assertTrue(p.level.getBlockState(p.attached.below()).getValue(FarmBlock.MOISTURE)==7,"Paddy did not wet native soil");
                    h.assertTrue(StarfieldCompat.inspect(p.level,p.attached).orElseThrow().mature()==(day==expected),"Paddy forecast differs from real days");}
                p.plant(id(species),season,false);
                for(var root:p.roots()){p.level.setBlock(root.below().east(3),Blocks.AIR.defaultBlockState(),3);p.level.setBlock(root.below().east(4),Blocks.WATER.defaultBlockState(),3);}
                h.assertTrue(!RiceCropBlock.hasNearbyPaddyWater(p.level,p.attached),"Outside-radius water incorrectly detected");
                p.day(1,false);h.assertTrue(p.growth(p.attached).phase==0 && p.growth(p.attached).dayInPhase==0,"Dry paddy grew without nearby source");
                for(var root:p.roots()){p.level.setBlock(root.below().east(4),Blocks.AIR.defaultBlockState(),3);p.level.setBlock(root.below().east(3),Blocks.WATER.defaultBlockState(),3);StarfieldGeneTestData.stat(p.level,root,"growth",10);}
                p.day(2,false);p.equal(h);h.assertTrue(p.growth(p.attached).phase>0,"Source restoration did not advance once");
                for(var root:p.roots())p.level.setBlock(root.below().east(3),Blocks.AIR.defaultBlockState(),3);
                var before=p.progress(p.attached);p.day(3,false);h.assertTrue(before.equals(p.progress(p.attached)),"Removed source still watered crop");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void fiberDryGrowthSeasonModelsScytheQualityAndGainRemainNative(GameTestHelper h) {
        try(var p=new Plot(h,"fiber")) {
            p.plant(id("fiber"),3,false);StarfieldGeneTestData.stat(p.level,p.attached,"gain",10);
            for(int day=1;day<=7;day++){p.day(day,false);p.equal(h);p.geometry(h);}
            h.assertTrue(StarfieldCompat.inspect(p.level,p.attached).orElseThrow().mature(),"Fiber required external water");
            p.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(!StardewCropRuntime.harvestForPlayer(p.player,p.attached.above(),InteractionHand.MAIN_HAND,false,false).harvested(),"Fiber bypassed scythe");
            p.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(com.stardew.craft.item.ModItems.SCYTHE.get()));
            p.level.random.setSeed(820);h.assertTrue(StardewCropRuntime.harvestForPlayer(p.player,p.plain,InteractionHand.MAIN_HAND,true,false).harvested(),"Native fiber scythe failed");
            var expected=p.counts();p.clearDrops();p.level.random.setSeed(820);
            h.assertTrue(StardewCropRuntime.harvestForPlayer(p.player,p.attached.above(),InteractionHand.MAIN_HAND,true,false).harvested(),"Attached fiber scythe failed");
            h.assertTrue(p.counts().entrySet().stream().allMatch(e->e.getValue()==2*expected.getOrDefault(e.getKey(),0))
                    && p.drops.stream().allMatch(e->QualityHelper.getQuality(e.getItem())==QualityHelper.NORMAL),"Fiber primary gain/composition/quality changed");h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void cactusTransferCannotBypassNativeOutdoorGate(GameTestHelper h) {
        try(var p=new Plot(h,"cactus")) {
            var indoor=GreenhouseManager.INTERIOR_ORIGIN.offset(12,2,12);p.level.getChunk(indoor);p.soil(indoor,true);
            var seed=new ItemStack(BuiltInRegistries.ITEM.get(StarfieldCompat.species(id("cactus_fruit")).orElseThrow().seed()),2);
            p.soil(p.attached,true);var before=seed.copy();seed.useOn(p.context(p.attached.below(),seed));
            h.assertTrue(p.level.getBlockState(p.attached).isAir() && seed.getCount()==before.getCount(),"Outdoor native cactus seed planted");
            try {
                h.assertTrue(seed.useOn(p.context(indoor.below(),seed)).consumesAction(),"Native greenhouse cactus seed denied");
                StarfieldCompat.setSticks(p.level,indoor,"obsidian");var identity=p.entry(indoor);
                var tool=new ItemStack(com.agricraft.agricraft.common.registry.ModItems.TROWEL.get());
                h.assertTrue(tool.useOn(p.context(indoor.above(),tool)).consumesAction(),"Greenhouse cactus pickup denied");
                var held=tool.copy();h.assertTrue(tool.useOn(p.context(p.attached.below(),tool))==InteractionResult.FAIL
                        && ItemStack.isSameItemSameComponents(tool,held) && p.level.getBlockState(p.attached).isAir(),"Trowel bypassed cactus indoor gate");
                h.assertTrue(tool.useOn(p.context(indoor.below(),tool)).consumesAction() && identity.equals(p.entry(indoor)),"Cactus greenhouse retry lost state");
                h.succeed();
            } finally {p.level.setBlock(indoor,Blocks.AIR.defaultBlockState(),3);p.level.setBlock(indoor.above(),Blocks.AIR.defaultBlockState(),3);p.level.setBlock(indoor.below(),Blocks.AIR.defaultBlockState(),3);}
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void allWildSeasonsPreserveIdentityModelsInspectionAndSingleNativeForagePickup(GameTestHelper h) {
        try(var p=new Plot(h,"forage")) {
            for(int season=0;season<4;season++) {
                p.plant(id(List.of("spring","summer","fall","winter").get(season)+"_wild_seed"),season,true);
                var plain=p.entry(p.plain);var attached=p.entry(p.attached);
                for(int day=1;day<=7;day++)p.day(day,true);
                for(var root:p.roots())h.assertTrue(p.level.getBlockState(root).getBlock() instanceof ForageBlock && p.growth(root)==null,"Wild maturity retained crop scheduler");
                h.assertTrue(p.entry(p.attached).plantingId().equals(attached.plantingId()) && p.entry(p.attached).genome().equals(attached.genome())
                        && p.entry(p.attached).sticks().equals("iron") && !p.entry(p.attached).form().equals(attached.type()),"Wild transition lost source identity");
                var report=StarfieldCompat.inspection(p.level,p.attached).orElseThrow();h.assertTrue(report.getBoolean("forage") && report.getBoolean("mature") && report.getInt("remaining")==0,"Forage report lost readiness");
                h.assertTrue(StarfieldInspection.matches(p.level,p.attached,Optional.of(p.entry(p.attached)),report),"Forage report rejected actual form");
                var saved=StarfieldPlantData.get(p.level).save(new CompoundTag(),p.level.registryAccess());p.reload(saved);StarfieldCompat.scan(p.level,p.level.getChunkAt(p.attached));
                h.assertTrue(report.getCompound("entry").equals(p.entry(p.attached).encode()),"Scan/reload lost mature form");
                int before=p.xp();p.level.random.setSeed(600);h.assertTrue(p.pick(p.plain).consumesAction(),"Native forage pickup failed");var output=p.counts();int awarded=p.xp()-before;long random=p.level.random.nextLong();p.clearDrops();
                p.level.random.setSeed(600);int secondBefore=p.xp();h.assertTrue(p.pick(p.attached).consumesAction(),"Attached forage pickup failed");
                h.assertTrue(output.equals(p.counts()) && p.xp()-secondBefore==awarded && p.level.random.nextLong()==random,"Forage changed output, quality, XP or random sequence");
                var once=p.counts();p.pick(p.attached);h.assertTrue(once.equals(p.counts()) && StarfieldCompat.planting(p.level,p.attached).isEmpty(),"Forage duplicate pickup or retained entry");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void forageGainRollbackToolAndPermissionGatesDoNotAffectWorldForage(GameTestHelper h) {
        try(var p=new Plot(h,"forage-gain")) {
            p.plant(id("spring_wild_seed"),0,true);StarfieldGeneTestData.stat(p.level,p.attached,"gain",10);for(int day=1;day<=7;day++)p.day(day,true);
            var saved=p.entry(p.attached);var state=p.level.getBlockState(p.attached);int xp=p.xp();
            Consumer<EntityJoinLevelEvent> reject=e->{if(e.getLevel()==p.level && e.getEntity() instanceof ItemEntity)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(reject);
            try {h.assertTrue(p.pick(p.attached)==InteractionResult.FAIL,"Rejected forage output succeeded");}finally{NeoForge.EVENT_BUS.unregister(reject);}
            h.assertTrue(p.level.getBlockState(p.attached).equals(state) && p.entry(p.attached).equals(saved) && p.xp()==xp && p.counts().isEmpty(),"Forage rollback lost identity or leaked output/XP");
            p.player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);h.assertTrue(p.pick(p.attached)==InteractionResult.FAIL && saved.equals(p.entry(p.attached)),"Forage bypassed build permission");p.player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var tool=new ItemStack(com.agricraft.agricraft.common.registry.ModItems.MAGNIFYING_GLASS.get());
            h.assertTrue(state.useItemOn(tool,p.level,p.player,InteractionHand.MAIN_HAND,p.hit(p.attached))==ItemInteractionResult.FAIL && saved.equals(p.entry(p.attached)),"AgriCraft tool fell through to forage pickup");
            p.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.level.random.setSeed(781);h.assertTrue(p.pick(p.plain).consumesAction(),"Baseline forage pickup failed");var expected=p.counts();int firstXp=p.xp();p.clearDrops();p.level.random.setSeed(781);
            h.assertTrue(p.pick(p.attached).consumesAction(),"Forage retry failed");h.assertTrue(p.counts().entrySet().stream().allMatch(e->e.getValue()==2*expected.getOrDefault(e.getKey(),0)),"Forage gain lost native components or doubled twice");
            h.assertTrue(p.xp()>firstXp,"Forage XP missing");
            p.clearDrops();p.soil(p.attached,true);p.level.setBlock(p.attached,state,3);
            h.assertTrue(StarfieldCompat.planting(p.level,p.attached).isEmpty() && StarfieldCompat.inspect(p.level,p.attached).isEmpty(),"World forage acquired a planted genotype");
            h.assertTrue(p.pick(p.attached).consumesAction() && !p.counts().isEmpty(),"World forage native pickup changed");h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void forageTrowelAndNativeSeasonRemovalKeepNoGhostOrScheduler(GameTestHelper h) {
        try(var p=new Plot(h,"forage-transfer")) {
            p.plant(id("spring_wild_seed"),0,true);for(int day=1;day<=7;day++)p.day(day,true);
            var entry=p.entry(p.attached);var tool=new ItemStack(com.agricraft.agricraft.common.registry.ModItems.TROWEL.get());
            h.assertTrue(tool.useOn(p.context(p.attached,tool)).consumesAction(),"Mature forage trowel failed");
            var held=tool.copy();p.level.setBlock(p.attached,Blocks.STONE.defaultBlockState(),3);
            h.assertTrue(tool.useOn(p.context(p.attached.below(),tool))==InteractionResult.FAIL && ItemStack.isSameItemSameComponents(held,tool),"Blocked forage transfer lost held state");p.level.setBlock(p.attached,Blocks.AIR.defaultBlockState(),3);
            h.assertTrue(tool.useOn(p.context(p.attached.below(),tool)).consumesAction() && entry.equals(p.entry(p.attached)) && p.growth(p.attached)==null,"Forage transfer registered daily growth or lost genotype");
            StardewTimeManager.get().setCurrentSeason(1);var state=p.level.getBlockState(p.attached);state.randomTick(p.level,p.attached,p.level.random);
            h.assertTrue(p.level.getBlockState(p.attached).isAir() && StarfieldCompat.planting(p.level,p.attached).isEmpty() && p.growth(p.attached)==null,"Native seasonal removal left forage genome");h.succeed();
        }
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void disabledWildTransformationKeepsOriginalNativeBehavior(GameTestHelper h) {
        try(var p=new Plot(h,"disabled")) {
            p.plant(id("spring_wild_seed"),0,true);StarfieldGeneTestData.stat(p.level,p.attached,"gain",10);
            var before=p.entry(p.attached);CompatConfig.enableStarfield=false;
            for(int day=1;day<=7;day++)p.day(day,true);
            h.assertTrue(p.level.getBlockState(p.attached).getBlock() instanceof ForageBlock && p.growth(p.attached)==null,"Disabled integration changed transformation");
            h.assertTrue(StarfieldCompat.planting(p.level,p.attached).isEmpty(),"Disabled integration exposed dormant genes");
            CompatConfig.enableStarfield=true;var after=p.entry(p.attached);
            h.assertTrue(after.plantingId().equals(before.plantingId()) && after.genome().equals(before.genome())
                    && after.sticks().equals(before.sticks()),"Disabled transformation lost existing planted heredity");h.succeed();
        }finally{CompatConfig.enableStarfield=true;}
    }

    @GameTest(batch="starfield_special", templateNamespace="agricraft", template="starfield_probe")
    public static void maturePlantedForageBreedsItsWildSeedSpeciesAndKeepsParent(GameTestHelper h) {
        try(var p=new Plot(h,"forage-breed")) {
            boolean old=com.agricraft.agricraft.api.config.CoreConfig.cloneMutations;com.agricraft.agricraft.api.config.CoreConfig.cloneMutations=false;
            var target=p.attached.east();
            try {
                p.plant(id("spring_wild_seed"),0,true);StarfieldGeneTestData.stat(p.level,p.attached,"fertility",10);for(int day=1;day<=7;day++)p.day(day,true);
                var parent=p.entry(p.attached);var form=p.level.getBlockState(p.attached);p.soil(target,true);
                p.level.setBlock(target,com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState()
                        .setValue(com.agricraft.agricraft.common.block.CropBlock.CROP_STATE,com.agricraft.agricraft.common.block.CropState.DOUBLE_STICKS),3);
                var crop=(com.agricraft.agricraft.common.block.entity.CropBlockEntity)p.level.getBlockEntity(target);boolean bred=false;
                for(int attempt=0;attempt<200 && !bred;attempt++)bred=com.agricraft.agricraft.api.AgriApi.getMutationHandler().getActiveCrossBreedEngine().handleCrossBreedTick(crop,crop.streamNeighbours(),net.minecraft.util.RandomSource.create(attempt));
                var child=StarfieldCompat.planting(p.level,target).orElseThrow();
                h.assertTrue(bred && child.type().equals(parent.type()) && child.form().equals(child.type()) && child.genome().equals(parent.genome())
                        && !child.plantingId().equals(parent.plantingId()) && p.growth(target).phase==0 && p.growth(target).lastDailyDay<1,"Mature forage bred wrong species or inherited its mature state");
                h.assertTrue(p.entry(p.attached).equals(parent) && p.level.getBlockState(p.attached).equals(form) && p.growth(p.attached)==null,"Forage breeding mutated parent");h.succeed();
            } finally {com.agricraft.agricraft.api.config.CoreConfig.cloneMutations=old;p.level.setBlock(target,Blocks.AIR.defaultBlockState(),3);p.level.setBlock(target.below(),Blocks.AIR.defaultBlockState(),3);}
        }
    }

    @GameTest(batch="starfield_special_restart", templateNamespace="agricraft", template="starfield_probe")
    public static void realJvmRestartPreservesMatureForageIdentityWithoutDailyRegistration(GameTestHelper h) throws Exception {
        String phase=System.getProperty("agricraft.starfield.restartPhase","");if(phase.isEmpty()){h.succeed();return;}
        var p=new Plot(h,"restart");var path=Path.of(System.getProperty("agricraft.starfield.dailyRestartWitness")).resolveSibling("iteration10-restart-witness.nbt");
        try {
            if(phase.equals("write")) {
                p.plant(id("spring_wild_seed"),0,true);StarfieldGeneTestData.stat(p.level,p.attached,"gain",10);for(int day=1;day<=7;day++)p.day(day,true);
                var witness=new CompoundTag();for(int i=0;i<2;i++){var root=p.roots().get(i);var row=p.entry(root).encode();row.putString("block",p.level.getBlockState(root).toString());witness.put("root"+i,row);}NbtIo.writeCompressed(witness,path);
            } else {
                var witness=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());
                for(int i=0;i<2;i++){var root=p.roots().get(i);var actual=p.entry(root).encode();actual.putString("block",p.level.getBlockState(root).toString());h.assertTrue(actual.equals(witness.getCompound("root"+i)) && p.growth(root)==null,"JVM restart lost mature form or restored obsolete crop calendar");}
                p.day(7,true);h.assertTrue(p.growth(p.attached)==null && p.entry(p.attached).encode().getString("form").equals(witness.getCompound("root1").getString("form")),"Restart repeated transition");
                h.assertTrue(p.pick(p.attached).consumesAction() && StarfieldCompat.planting(p.level,p.attached).isEmpty(),"Restart mature forage could not harvest once");
            }
            h.succeed();
        }finally{if(!phase.equals("write"))p.close();else p.restoreClock();}
    }

    private static ResourceLocation id(String species){return ResourceLocation.parse("stardewcraft:"+species+"_crop");}
    private static final class Plot implements AutoCloseable {
        final ServerLevel level;final BlockPos plain,attached;final ServerPlayer player;final ArrayList<ItemEntity> drops=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> observer;final int oldSeason;int season;
        Plot(GameTestHelper h,String name) {
            level=h.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);var uuid=UUID.nameUUIDFromBytes(("agricraft:special:"+name).getBytes(StandardCharsets.UTF_8));
            var farms=FarmInstanceRegistry.get(level.getServer());var farm=farms.getFarm(uuid);if(farm==null)farm=farms.createFarm(uuid,name,"Special probe",FarmType.STANDARD);farm.markInitialized();farm.setGraceDaysLeft(0);
            plain=farm.getOrigin().offset(136,10,136);attached=plain.east(8);level.getChunk(plain);level.getChunk(attached);
            player=FakePlayerFactory.get(level,new GameProfile(uuid,"SpecialProbe"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);oldSeason=StardewTimeManager.get().getCurrentSeason();season=oldSeason;
            observer=e->{if(e.getLevel()==level && e.getEntity() instanceof ItemEntity item && (new AABB(plain).inflate(2).contains(item.position()) || new AABB(attached).inflate(2).contains(item.position())))drops.add(item);};NeoForge.EVENT_BUS.addListener(observer);
        }
        List<BlockPos> roots(){return List.of(plain,attached);}
        int allowedSeason(ResourceLocation species){var block=(StardewCropBlock)BuiltInRegistries.BLOCK.get(species);soil(plain,true);for(int i=0;i<4;i++){StardewTimeManager.get().setCurrentSeason(i);if(block.canPlantAt(level,plain))return i;}throw new IllegalStateException("No season for "+species);}
        void soil(BlockPos root,boolean wet){level.setBlock(root.below(),com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,wet?7:0),3);}
        void plant(ResourceLocation species,int season,boolean wet){cleanup();this.season=season;StardewTimeManager.get().setCurrentSeason(season);for(var root:roots()){soil(root,wet);if(!CropPlanting.place(level,root,BuiltInRegistries.BLOCK.get(species).defaultBlockState()))throw new IllegalStateException("Native placement failed "+species);entry(root);}StarfieldCompat.setSticks(level,attached,"iron");}
        StarfieldPlantData.Entry entry(BlockPos root){return StarfieldCompat.planting(level,root).orElseThrow();}
        CropGrowthManager.CropGrowthState growth(BlockPos root){return CropGrowthManager.get(level).getState(level,root);}
        void day(int day,boolean wet){for(var root:roots()){var soil=level.getBlockState(root.below());level.setBlock(root.below(),soil.setValue(FarmBlock.MOISTURE,wet?7:0),3);level.random.setSeed(442);CropGrowthManager.get(level).settleCrops(level,List.of(GlobalPos.of(level.dimension(),root)),day,season,false);}}
        CompoundTag progress(BlockPos root){var g=growth(root);var t=new CompoundTag();t.putInt("phase",g.phase);t.putInt("day",g.dayInPhase);t.putInt("soil",g.infertileProgress);t.putBoolean("regrow",g.regrowing);return t;}
        void equal(GameTestHelper h){h.assertTrue(progress(plain).equals(progress(attached)),"Paired native phases differ");}
        void geometry(GameTestHelper h){for(var root:roots()){var lower=level.getBlockState(root);var upper=level.getBlockState(root.above());h.assertTrue(lower.getBlock()==upper.getBlock() && upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.UPPER && lower.getValue(StardewCropBlock.AGE).equals(upper.getValue(StardewCropBlock.AGE)),"Native halves differ");}
            h.assertTrue(level.getBlockState(plain).equals(level.getBlockState(attached)) && level.getBlockState(plain.above()).equals(level.getBlockState(attached.above())),"Stick material changed native model state");}
        BlockHitResult hit(BlockPos pos){return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);}
        UseOnContext context(BlockPos pos,ItemStack stack){player.setPos(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);return new UseOnContext(player,InteractionHand.MAIN_HAND,hit(pos));}
        InteractionResult pick(BlockPos root){player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);return level.getBlockState(root).useWithoutItem(level,player,hit(root));}
        int xp(){return PlayerStardewDataAPI.getSkillExperience(player,SkillType.FORAGING);}
        Map<String,Integer> counts(){var result=new HashMap<String,Integer>();for(var e:drops)if(!e.isRemoved()){var s=e.getItem().copy();StarfieldGenomes.clearHeredity(s);result.merge(s.copyWithCount(1).save(level.registryAccess()).toString(),s.getCount(),Integer::sum);}return result;}
        void clearDrops(){drops.forEach(ItemEntity::discard);drops.clear();}
        void reload(CompoundTag tag){level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE,StarfieldPlantData.load(tag,level.registryAccess()));}
        void cleanup(){clearDrops();for(var root:roots()){level.setBlock(root,Blocks.AIR.defaultBlockState(),3);level.setBlock(root.above(),Blocks.AIR.defaultBlockState(),3);FertilizerManager.get(level).removeFertilizer(level,root.below());level.setBlock(root.below(),Blocks.AIR.defaultBlockState(),3);level.setBlock(root.below().east(3),Blocks.AIR.defaultBlockState(),3);level.setBlock(root.below().east(4),Blocks.AIR.defaultBlockState(),3);}}
        void restoreClock(){StardewTimeManager.get().setCurrentSeason(oldSeason);}
        @Override public void close(){cleanup();NeoForge.EVENT_BUS.unregister(observer);restoreClock();}
    }
}
