package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.compat.starfield.*;
import com.agricraft.agricraft.common.block.CropStickVariant;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.api.v1.agriculture.*;
import com.stardew.craft.api.v1.internal.giant.GiantCropGrowth;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.crop.giant.GiantCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.farming.CropPlanting;
import com.stardew.craft.item.quality.QualityHelper;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.player.*;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldGiantGameTests {
    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe", timeoutTicks=200)
    public static void returnedSticksSurviveRealItemPhysicsAboveFarmSoil(GameTestHelper h) {
        var p = new Plot(h, "stick_physics");
        p.forceRegion();
        h.runAfterDelay(40, () -> {
            try {
                p.plant(cauliflower(), true, true);
                h.assertTrue(p.form(p.winningDay(cauliflower(), 1000), true, false).grown() == 1,
                        "Native giant failed in item physics fixture");
                h.assertTrue(p.counts().equals(stickCounts(3, 3, 3)), "Initial stick return was incomplete");
                h.runAfterDelay(40, () -> {
                    try {
                        h.assertTrue(p.counts().equals(stickCounts(3, 3, 3)), "Returned sticks vanished during real item ticks");
                        for (var item : p.drops)
                            h.assertTrue(item.getY() >= p.anchor.getY() - .1,
                                    "Returned stick fell through giant/soil collision: " + item.position());
                        h.succeed();
                    } finally { p.close(); }
                });
            } catch (RuntimeException failure) { p.close(); throw failure; }
        });
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void allFiveNativeGiantsReturnEachStickAndConsumeNineIdentities(GameTestHelper h) {
        try (var p = new Plot(h, "five")) {
            int count = 0;
            for (var definition : coreDefinitions()) {
                p.plant(definition, true, true);
                var before = p.entries();
                var day = p.winningDay(definition, 1000);
                p.level.random.setSeed(302); long expectedRandom = p.level.random.nextLong(); p.level.random.setSeed(302);
                h.assertTrue(p.form(day, true, false).grown() == 1, "Native giant failed: " + definition.id());
                h.assertTrue(p.level.random.nextLong() == expectedRandom, "Giant/stick return consumed world randomness");
                p.assertGiant(h, definition); p.assertConsumed(h);
                h.assertTrue(p.counts().equals(stickCounts(3, 3, 3)), "Wrong material/count restitution: " + p.counts());
                for (var item : p.drops)
                    h.assertTrue(item.getY() >= p.anchor.getY() + definition.height() + .5,
                            "Stick spawned inside native giant footprint: " + definition.id());
                h.assertTrue(p.form(day, true, false).grown() == 0 && p.counts().equals(stickCounts(3,3,3)), "Repeated formation duplicated sticks");
                h.assertTrue(before.values().stream().map(StarfieldPlantData.Entry::plantingId).distinct().count() == 9, "Fixture did not have nine real identities");
                count++;
            }
            h.assertTrue(count == 5, "Incomplete native species coverage"); h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void nativeOnePercentRollAndGeometryMatchWithAndWithoutSticks(GameTestHelper h) {
        try (var p = new Plot(h, "chance")) {
            var definition = cauliflower();
            h.assertTrue(definition.chance() == .01 && definition.width()==3 && definition.depth()==3 && definition.requiresWater(), "Official definition changed");
            var control = new LinkedHashMap<Integer, Boolean>();
            for (boolean attached : List.of(false, true)) {
                p.plant(definition, attached, true);
                for (int day=1000; day<2000; day++) {
                    boolean expected=GiantCropGrowth.wins(p.level.getSeed(), p.level.dimension().location(),p.anchor,definition.id(),day,definition.chance());
                    var result=p.form(day,true,false);
                    h.assertTrue((result.grown()==1)==expected && result.rolls()==1,"Native probability/calendar changed day="+day);
                    if(!attached)control.put(day,result.grown()==1);
                    else h.assertTrue(control.get(day)==(result.grown()==1),"Sidecar changed base roll");
                    if(expected){p.assertGiant(h,definition);break;}
                }
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void missingWrongSpeciesImmatureTriggerAndNativeImmatureNeighbors(GameTestHelper h) {
        try(var p=new Plot(h,"grid")) {
            var definition=cauliflower();
            p.plant(definition,true,true);var missing=p.roots.get(8);p.level.setBlock(missing,Blocks.AIR.defaultBlockState(),3);
            int day=p.winningDay(definition,1000);var entries=p.entries();
            h.assertTrue(p.form(day,true,false).grown()==0 && p.entries().equals(entries) && p.counts().isEmpty(),"Incomplete footprint consumed roots/sticks");
            p.plant(definition,true,true);var wrong=p.roots.get(8);p.level.setBlock(wrong,Blocks.AIR.defaultBlockState(),3);
            CropPlanting.place(p.level,wrong,BuiltInRegistries.BLOCK.get(id("potato_crop")).defaultBlockState());entries=p.entries();
            h.assertTrue(p.form(day,true,false).grown()==0 && p.entries().equals(entries),"Different produce joined giant");
            p.plant(definition,true,false);
            h.assertTrue(p.form(day,true,false).grown()==0,"Immature trigger grew giant");
            p.mature(p.anchor);h.assertTrue(p.form(day,true,false).grown()==1,"Native immature-neighbor rule was tightened");
            p.assertGiant(h,definition);h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void obstructionsContainersAndWrongSupportPreserveState(GameTestHelper h) {
        try(var p=new Plot(h,"obstruction")) {
            for(var block:List.of(Blocks.STONE,Blocks.CHEST)) {
                p.plant(cauliflower(),true,true);var pos=p.anchor.offset(1,1,1);p.level.setBlock(pos,block.defaultBlockState(),3);
                if(block==Blocks.CHEST)((net.minecraft.world.level.block.entity.ChestBlockEntity)p.level.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,7));
                var entries=p.entries();int day=p.winningDay(cauliflower(),1000);
                h.assertTrue(p.form(day,true,false).grown()==0 && p.entries().equals(entries) && p.counts().isEmpty() && p.level.getBlockState(pos).is(block),"Giant erased obstruction/state");
                if(block==Blocks.CHEST)h.assertTrue(((net.minecraft.world.level.block.entity.ChestBlockEntity)p.level.getBlockEntity(pos)).getItem(0).getCount()==7,"Container contents erased");
            }
            p.plant(cauliflower(),true,true);p.level.setBlock(p.roots.get(8).below(),Blocks.STONE.defaultBlockState(),3);
            p.clearDrops(); // Native support-loss drops precede the giant attempt.
            h.assertTrue(p.form(p.winningDay(cauliflower(),1000),true,false).grown()==0 && p.counts().isEmpty(),"Invalid soil formed giant");h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void dryAndRepeatedDatesStayNativeIncludingOfflinePass(GameTestHelper h) {
        try(var p=new Plot(h,"dates")) {
            p.plant(cauliflower(),true,true);int day=p.winningDay(cauliflower(),1000);var entries=p.entries();
            h.assertTrue(p.form(day,false,false).grown()==0,"Dry trigger grew giant");
            h.assertTrue(p.form(day,true,true).rolls()==0 && p.entries().equals(entries) && p.counts().isEmpty(),"Offline/repeated date rerolled or returned sticks");
            int next=p.winningDay(cauliflower(),day+1);
            h.assertTrue(p.form(next,true,true).grown()==1 && p.counts().equals(stickCounts(3,3,3)),"Offline native formation/restitution failed");h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void realDailySchedulerConsumesGridOnceAndReturnsOnlyAttachedMaterials(GameTestHelper h) {
        try(var p=new Plot(h,"daily")) {
            for(boolean offline:List.of(false,true)) {
                p.plant(cauliflower(),true,true);
                for(int i=0;i<4;i++)StarfieldCompat.setSticks(p.level,p.roots.get(i),"");
                var positions=p.roots.stream().map(root->GlobalPos.of(p.level.dimension(),root)).toList();
                int day=p.winningDay(cauliflower(),1000);int season=StardewTimeManager.get().getCurrentSeason();
                h.assertTrue(p.manager().settleCrops(p.level,positions,day,season,offline).grown()==1,"Real daily giant pass failed");
                h.assertTrue(p.counts().equals(stickCounts(1,2,2)),"Daily pass returned detached or duplicate sticks");p.assertConsumed(h);
                h.assertTrue(p.manager().settleCrops(p.level,positions.reversed(),day,season,offline).grown()==0
                        && p.counts().equals(stickCounts(1,2,2)),"Repeated daily pass duplicated giant/materials");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void quarantinedAndFutureSidecarDataRejectReplacementWithoutLoss(GameTestHelper h) {
        try(var p=new Plot(h,"quarantine")) {
            for(boolean future:List.of(false,true)) {
                p.plant(cauliflower(),true,true);var store=p.level.getServer().overworld().getDataStorage();
                var original=StarfieldPlantData.get(p.level).save(new CompoundTag(),p.level.registryAccess());var invalid=original.copy();
                if(future)invalid.putInt("schema",52);
                else for(var value:invalid.getList("plants",Tag.TAG_COMPOUND)){var row=(CompoundTag)value;if(row.getLong("root")==p.anchor.asLong())row.putString("sticks","unreadable");}
                store.set(StarfieldPlantData.STORAGE,StarfieldPlantData.load(invalid,p.level.registryAccess()));
                var storedInvalid=StarfieldPlantData.get(p.level).save(new CompoundTag(),p.level.registryAccess());
                try {
                    int day=p.winningDay(cauliflower(),1000);var blocks=p.blocks();
                    h.assertTrue(p.form(day,true,false).grown()==0 && p.blocks().equals(blocks) && p.counts().isEmpty(),"Invalid sidecar was consumed by giant formation");
                    h.assertTrue(StarfieldPlantData.get(p.level).save(new CompoundTag(),p.level.registryAccess()).equals(storedInvalid),"Quarantined data changed");
                } finally {store.set(StarfieldPlantData.STORAGE,StarfieldPlantData.load(original,p.level.registryAccess()));}
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void giantCannotCrossNativeFarmBoundary(GameTestHelper h) {
        try(var p=new Plot(h,"boundary",true)) {
            p.plant(cauliflower(),true,true);var entries=p.entries();var blocks=p.blocks();
            h.assertTrue(p.form(p.winningDay(cauliflower(),1000),true,false).grown()==0 && entries.equals(p.entries())
                    && blocks.equals(p.blocks()) && p.counts().isEmpty(),"Giant crossed farm boundary/consumed sources");h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void rejectedAndThrowingPartialRestitutionRestoreBothPartsAndAllGenomes(GameTestHelper h) {
        try(var p=new Plot(h,"rollback")) {
            for(boolean throwing:List.of(false,true)) {
                p.plant(cauliflower(),true,true);var entries=p.entries();var blocks=p.blocks();var growth=p.growth();int day=p.winningDay(cauliflower(),1000);
                int[] seen={0};Consumer<EntityJoinLevelEvent> reject=e->{if(e.getLevel()==p.level && e.getEntity() instanceof ItemEntity && ++seen[0]==2){if(throwing)throw new IllegalStateException("giant restitution probe");e.setCanceled(true);}};
                NeoForge.EVENT_BUS.addListener(reject);
                try{var result=throwing?p.manager().settleCrops(p.level,p.roots.stream().map(root->GlobalPos.of(p.level.dimension(),root)).toList(),day,StardewTimeManager.get().getCurrentSeason(),false):p.form(day,true,false);
                    h.assertTrue(result.grown()==0,"Rejected/throwing restitution reported giant success");}finally{NeoForge.EVENT_BUS.unregister(reject);}
                h.assertTrue(p.entries().equals(entries) && p.blocks().equals(blocks) && p.counts().isEmpty(),"Partial restitution lost/doubled state or leaked items");
                for(var root:p.roots)h.assertTrue(p.manager().getState(p.level,root)==growth.get(root),"Rollback replaced native progress object");
                h.assertTrue(p.manager().getAllCropPositions().stream().filter(pos->pos.dimension().equals(p.level.dimension()) && p.roots.contains(pos.pos())).count()==9,"Daily pending-removal batch lost restored scheduler roots");
                h.assertTrue(p.form(day,true,false).rolls()==0,"Rollback allowed same-date reroll");
                h.assertTrue(p.form(p.winningDay(cauliflower(),day+1),true,false).grown()==1 && p.counts().equals(stickCounts(3,3,3)),"Retry lost/doubled materials");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void reentrantFormationCannotPublishDuplicateSticks(GameTestHelper h) {
        try(var p=new Plot(h,"reentrant")) {
            p.plant(cauliflower(),true,true);var entries=p.entries();int day=p.winningDay(cauliflower(),1000);boolean[] nested={false};
            Consumer<EntityJoinLevelEvent> nest=e->{if(e.getLevel()==p.level && e.getEntity() instanceof ItemEntity && !nested[0]){nested[0]=true;p.form(day+1,true,false);}};
            NeoForge.EVENT_BUS.addListener(nest);
            try{h.assertTrue(p.form(day,true,false).grown()==0,"Reentrant output succeeded");}finally{NeoForge.EVENT_BUS.unregister(nest);}
            h.assertTrue(nested[0] && p.entries().equals(entries) && p.counts().isEmpty(),"Reentrant output leaked items or lost roots");
            h.assertTrue(p.form(p.winningDay(cauliflower(),day+1),true,false).grown()==1,"Reentrant guard did not unwind");h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void giantPlayerHarvestKeepsNativeToolsQualityQuantityXpAndNoParentGenome(GameTestHelper h) {
        try(var p=new Plot(h,"harvest")) {
            for(var definition:coreDefinitions()) {
                p.plant(definition,true,true);p.maximumGainOnOneParent();
                h.assertTrue(p.form(p.winningDay(definition,1000),true,false).grown()==1,"Harvest fixture failed");p.clearDrops();
                var main=p.anchor.offset(1,0,1);var block=(GiantCropBlock)p.level.getBlockState(main).getBlock();
                p.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                PlayerStardewDataAPI.setExperience(p.player,SkillType.FARMING,0);
                h.assertTrue(p.player.gameMode.destroyBlock(p.anchor.above()),"Native empty-hand extension destruction failed");
                int count=p.counts().getOrDefault(BuiltInRegistries.ITEM.getKey(block.getDropItem().get()).toString(),0);
                h.assertTrue(count>=block.getDropMin() && count<=block.getDropMax(),"Giant output gained a random parent bonus");
                h.assertTrue(p.drops.stream().filter(e->!e.isRemoved() && e.getItem().is(block.getDropItem().get())).allMatch(e->QualityHelper.getQuality(e.getItem())==QualityHelper.NORMAL
                        && !e.getItem().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(StarfieldGenomes.SEED_KEY)),"Giant output has nonnative quality or arbitrary parent genome");
                h.assertTrue(PlayerStardewDataAPI.getSkillExperience(p.player,SkillType.FARMING)==GiantCropBlock.CHOP_FARMING_XP,"Giant XP changed");
                var counts=p.counts();p.player.gameMode.destroyBlock(main);h.assertTrue(counts.equals(p.counts()),"Other cell duplicated giant harvest");
                for(var pos:p.blocks().keySet())h.assertTrue(p.level.getBlockState(pos).isAir(),"Giant harvest left carrier cells");
            }
            h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void legacyMissingSidecarsStayNativeAndBaselineDoesNotInventSticks(GameTestHelper h) {
        try(var p=new Plot(h,"legacy")) {
            p.plant(cauliflower(),false,true);var data=StarfieldPlantData.get(p.level);var tag=data.save(new CompoundTag(),p.level.registryAccess());
            tag.getList("plants",Tag.TAG_COMPOUND).removeIf(row->p.roots.contains(BlockPos.of(((CompoundTag)row).getLong("root"))) && ((CompoundTag)row).getString("dimension").equals(p.level.dimension().location().toString()));
            p.level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE,StarfieldPlantData.load(tag,p.level.registryAccess()));
            var baseline=StarfieldCompat.planting(p.level,p.anchor).orElseThrow();
            h.assertTrue(baseline.genome().equals(StarfieldGenomes.baseline(p.species().id())) && baseline.sticks().isEmpty(),"Legacy root got unstable genes/materials");
            h.assertTrue(p.form(p.winningDay(cauliflower(),1000),true,false).grown()==1 && p.counts().isEmpty(),"Legacy native giant changed or invented sticks");h.succeed();
        }
    }

    @GameTest(batch="starfield_giants", templateNamespace="agricraft", template="starfield_probe")
    public static void disabledIntegrationAndTileDropsKeepNativePolicies(GameTestHelper h) {
        try(var p=new Plot(h,"disabled")) {
            p.plant(cauliflower(),true,true);int day=p.winningDay(cauliflower(),1000);boolean old=CompatConfig.enableStarfield;
            CompatConfig.enableStarfield=false;
            try{h.assertTrue(p.form(day,true,false).grown()==1 && p.counts().isEmpty(),"Disabled integration altered native formation/output");}finally{CompatConfig.enableStarfield=old;}
            p.assertConsumed(h);p.plant(cauliflower(),true,true);var rule=p.level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);boolean drops=rule.get();rule.set(false,p.level.getServer());
            try{h.assertTrue(p.form(day,true,false).grown()==1 && p.counts().isEmpty(),"doTileDrops=false emitted stick items");}finally{rule.set(drops,p.level.getServer());}
            p.assertConsumed(h);h.succeed();
        }
    }

    @GameTest(batch="starfield_giants_restart", templateNamespace="agricraft", template="starfield_probe", timeoutTicks=200)
    public static void realJvmRestartPreservesGiantAndReturnEntitiesWithoutNineOldRoots(GameTestHelper h) throws Exception {
        String phase=System.getProperty("agricraft.starfield.restartPhase","");if(phase.isEmpty()){h.succeed();return;}
        var p=new Plot(h,"restart");
        // The Gradle run's working directory differs; use the existing repository witness anchor.
        var path=Path.of(System.getProperty("agricraft.starfield.restartWitness")).resolveSibling("iteration11-restart-witness.nbt");
        p.forceRegion();
        // Block FULL loading is synchronous; entity loading/visibility uses later server ticks.
        h.runAfterDelay(40, () -> { try {
            if(phase.equals("write")) {
                p.plant(cauliflower(),true,true);int day=p.winningDay(cauliflower(),1000);h.assertTrue(p.form(day,true,false).grown()==1,"Restart fixture failed");
                var witness=new CompoundTag();witness.putInt("day",day);var entities=new ListTag();
                for(var item:p.drops)if(!item.isRemoved()){item.setUnlimitedLifetime();item.setNoGravity(true);item.setDeltaMovement(0,0,0);item.setNeverPickUp();var row=new CompoundTag();row.putUUID("uuid",item.getUUID());row.put("stack",item.getItem().save(p.level.registryAccess()));entities.add(row);}
                witness.put("items",entities);NbtIo.writeCompressed(witness,path);p.assertConsumed(h);p.assertGiant(h,cauliflower());
            } else {
                var witness=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());p.assertGiant(h,cauliflower());p.assertConsumed(h);
                for(var value:witness.getList("items",Tag.TAG_COMPOUND)){var row=(CompoundTag)value;var entity=p.level.getEntity(row.getUUID("uuid"));h.assertTrue(entity instanceof ItemEntity && ((ItemEntity)entity).getItem().save(p.level.registryAccess()).equals(row.get("stack")),"Real restart lost return entity/materials/UUID");}
                var actualCounts=new LinkedHashMap<String,Integer>();var actualIds=new HashSet<UUID>();
                for(var entity:p.level.getEntitiesOfClass(ItemEntity.class,new AABB(p.anchor).inflate(8))) {
                    actualCounts.merge(BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString(),entity.getItem().getCount(),Integer::sum);actualIds.add(entity.getUUID());
                }
                var expectedIds=new HashSet<UUID>();for(var value:witness.getList("items",Tag.TAG_COMPOUND))expectedIds.add(((CompoundTag)value).getUUID("uuid"));
                h.assertTrue(actualCounts.equals(stickCounts(3,3,3)) && actualIds.equals(expectedIds),"Saved world returned incorrect entities/materials: "+actualCounts);
                h.assertTrue(p.form(witness.getInt("day"),true,false).grown()==0,"Restart reran consumed grid");
                p.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));PlayerStardewDataAPI.setExperience(p.player,SkillType.FARMING,0);
                h.assertTrue(p.player.gameMode.destroyBlock(p.anchor.offset(1,0,1)),"Saved giant cannot harvest");
                h.assertTrue(PlayerStardewDataAPI.getSkillExperience(p.player,SkillType.FARMING)==5,"Restart giant harvested twice/lost XP");
            }
            h.succeed();
        } catch(Exception failure){throw new RuntimeException(failure);}
        finally {if(!phase.equals("write"))p.close();else p.detach();} });
    }

    private static ResourceLocation id(String value){return ResourceLocation.parse("stardewcraft:"+value);}
    private static List<StardewGiantCrops.Definition> coreDefinitions(){return StardewGiantCrops.definitions().stream().filter(d->d.id().getNamespace().equals("stardewcraft")).toList();}
    private static StardewGiantCrops.Definition cauliflower(){return coreDefinitions().stream().filter(d->d.produce().equals(id("cauliflower"))).findFirst().orElseThrow();}
    private static Map<String,Integer> stickCounts(int wood,int iron,int obsidian){var counts=new LinkedHashMap<String,Integer>();for(var e:Map.of(CropStickVariant.WOODEN,wood,CropStickVariant.IRON,iron,CropStickVariant.OBSIDIAN,obsidian).entrySet())if(e.getValue()>0)counts.put(BuiltInRegistries.ITEM.getKey(CropStickVariant.toItem(e.getKey()).getItem()).toString(),e.getValue());return counts;}

    private static final class Plot implements AutoCloseable {
        final ServerLevel level;final BlockPos anchor;final List<BlockPos> roots=new ArrayList<>();final ServerPlayer player;
        final ArrayList<ItemEntity> drops=new ArrayList<>();final Consumer<EntityJoinLevelEvent> observer;final int oldSeason;
        final Set<net.minecraft.world.level.ChunkPos> forcedByProbe=new HashSet<>();
        StardewGiantCrops.Definition definition;
        Plot(GameTestHelper h,String name){this(h,name,false);}
        Plot(GameTestHelper h,String name,boolean boundary){
            level=h.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);var uuid=UUID.nameUUIDFromBytes(("agricraft:giants:"+name).getBytes(StandardCharsets.UTF_8));
            var farms=FarmInstanceRegistry.get(level.getServer());var farm=farms.getFarm(uuid);if(farm==null)farm=farms.createFarm(uuid,name,"Giant probe",FarmType.STANDARD);farm.markInitialized();farm.setGraceDaysLeft(0);
            anchor=boundary?farm.getOrigin().offset(FarmInstanceAllocator.FARM_SLOT_SIZE-2,10,142):farm.getOrigin().offset(142,10,142);for(int z=0;z<3;z++)for(int x=0;x<3;x++){var pos=anchor.offset(x,0,z);roots.add(pos);level.getChunk(pos);}
            player=FakePlayerFactory.get(level,new GameProfile(uuid,"GiantProbe"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(anchor.getX()+1.5,anchor.getY(),anchor.getZ()+1.5);oldSeason=StardewTimeManager.get().getCurrentSeason();
            observer=e->{if(e.getLevel()==level && e.getEntity() instanceof ItemEntity item && new AABB(anchor).expandTowards(3,3,3).inflate(2).contains(item.position()))drops.add(item);};NeoForge.EVENT_BUS.addListener(observer);
        }
        StarfieldCompat.Species species(){return StarfieldCompat.species(id(definition.produce().getPath()+"_crop")).orElseThrow(()->new IllegalStateException("Missing catalog crop: "+definition.produce()));}
        CropGrowthManager manager(){return CropGrowthManager.get(level);}
        void plant(StardewGiantCrops.Definition d,boolean sticks,boolean mature){cleanup();definition=d;var species=species();int season=allowedSeason(species);StardewTimeManager.get().setCurrentSeason(season);
            for(int i=0;i<roots.size();i++){var root=roots.get(i);level.setBlock(root.below(),com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);if(!CropPlanting.place(level,root,BuiltInRegistries.BLOCK.get(species.id()).defaultBlockState()))throw new IllegalStateException("Fixture planting failed");StarfieldCompat.planting(level,root).orElseThrow();if(sticks)StarfieldCompat.setSticks(level,root,List.of("wooden","iron","obsidian").get(i%3));if(mature)mature(root);}
        }
        int allowedSeason(StarfieldCompat.Species species){level.setBlock(anchor.below(),com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState(),3);var block=(StardewCropBlock)BuiltInRegistries.BLOCK.get(species.id());for(int season=0;season<4;season++){StardewTimeManager.get().setCurrentSeason(season);if(block.canPlantAt(level,anchor))return season;}throw new IllegalStateException("No native planting season for "+species.id());}
        void mature(BlockPos root){var crop=(StardewCropBlock)level.getBlockState(root).getBlock();for(int day=1;day<=species().nativePhaseDays().stream().mapToInt(Integer::intValue).sum();day++){level.setBlock(root.below(),level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE,7),3);manager().settleCrops(level,List.of(GlobalPos.of(level.dimension(),root)),day,StardewTimeManager.get().getCurrentSeason(),false);}if(!StardewCropRuntime.inspect(level,root).mature())throw new IllegalStateException("Native maturity fixture failed "+crop);}
        int winningDay(StardewGiantCrops.Definition d,int start){for(int day=start;day<start+100000;day++)if(GiantCropGrowth.wins(level.getSeed(),level.dimension().location(),anchor,d.id(),day,d.chance()))return day;throw new IllegalStateException("No winning native day");}
        GiantCropGrowth.Statistics form(int day,boolean watered,boolean offline){return GiantCropGrowth.process(level,roots,watered?Set.of(anchor):Set.of(),day,offline,List.of(anchor));}
        Map<BlockPos,StarfieldPlantData.Entry> entries(){var result=new LinkedHashMap<BlockPos,StarfieldPlantData.Entry>();for(var root:roots)StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(),root)).ifPresent(e->result.put(root,e));return result;}
        Map<BlockPos,CropGrowthManager.CropGrowthState> growth(){var result=new LinkedHashMap<BlockPos,CropGrowthManager.CropGrowthState>();for(var root:roots)result.put(root,manager().getState(level,root));return result;}
        Map<BlockPos,BlockState> blocks(){var result=new LinkedHashMap<BlockPos,BlockState>();for(var pos:BlockPos.betweenClosed(anchor,anchor.offset(2,2,2)))result.put(pos.immutable(),level.getBlockState(pos));return result;}
        Map<String,Integer> counts(){var result=new LinkedHashMap<String,Integer>();for(var item:drops)if(!item.isRemoved())result.merge(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString(),item.getItem().getCount(),Integer::sum);return result;}
        void assertConsumed(GameTestHelper h){for(var root:roots)h.assertTrue(manager().getState(level,root)==null && manager().getAllCropPositions().stream().noneMatch(p->p.dimension().equals(level.dimension()) && p.pos().equals(root)) && StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(),root)).isEmpty(),"Giant retained original root/data: "+root);}
        void assertGiant(GameTestHelper h,StardewGiantCrops.Definition d){var giant=(GiantCropBlock)BuiltInRegistries.BLOCK.get(d.id());var main=anchor.offset(1,0,1);for(var pos:BlockPos.betweenClosed(anchor,anchor.offset(2,d.height()-1,2)))h.assertTrue(level.getBlockState(pos).is(giant) && main.equals(giant.findMainPos(level,pos,level.getBlockState(pos))),"Incomplete native giant model/footprint");}
        void maximumGainOnOneParent(){StarfieldGeneTestData.stat(level,anchor,"gain",10);}
        void clearDrops(){drops.forEach(ItemEntity::discard);drops.clear();for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(anchor).expandTowards(3,3,3).inflate(2)))entity.discard();}
        void cleanup(){clearDrops();for(var pos:BlockPos.betweenClosed(anchor.below(),anchor.offset(2,2,2)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);for(var root:roots)manager().removeCrop(level,root);}
        void forceRegion(){for(var root:roots){var chunk=new net.minecraft.world.level.ChunkPos(root);if(!level.getForcedChunks().contains(chunk.toLong()) && forcedByProbe.add(chunk))level.setChunkForced(chunk.x,chunk.z,true);}}
        void detach(){for(var chunk:forcedByProbe)level.setChunkForced(chunk.x,chunk.z,false);forcedByProbe.clear();NeoForge.EVENT_BUS.unregister(observer);StardewTimeManager.get().setCurrentSeason(oldSeason);}
        @Override public void close(){cleanup();detach();}
    }
}
