package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.*;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.registry.ModItems;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.block.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldReleaseGameTests {
    private static final ResourceLocation POTATO=ResourceLocation.parse("stardewcraft:potato_crop");

    @GameTest(batch="starfield_large_daily",templateNamespace="agricraft",template="starfield_probe",timeoutTicks=400)
    public static void largeRealFarmExecutesNativeDailyGrowthWithoutLoadingOtherChunks(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(com.stardew.craft.core.ModDimensions.STARDEW_VALLEY);
        var registry=com.stardew.craft.farm.FarmInstanceRegistry.get(level.getServer());
        var owner=UUID.nameUUIDFromBytes("agricraft:large-daily".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var farm=registry.getFarm(owner);if(farm==null)farm=registry.createFarm(owner,"large-daily","Acceptance benchmark",com.stardew.craft.farm.FarmType.STANDARD);
        farm.markInitialized();farm.setGraceDaysLeft(0);var origin=farm.getOrigin().offset(64,10,64);
        var roots=new ArrayList<GlobalPos>();var manager=com.stardew.craft.manager.CropGrowthManager.get(level);
        var samples=new ArrayList<Double>();
        try {
            for(int x=0;x<64;x++)for(int z=0;z<64;z++) {
                var pos=origin.offset(x,0,z);level.getChunk(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                level.setBlock(pos.below(),ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
                level.setBlock(pos,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);roots.add(GlobalPos.of(level.dimension(),pos));
                var entry=StarfieldCompat.planting(level,pos).orElseThrow();
                if((x+z)%2==0)StarfieldCompat.setSticks(level,pos,"iron");
                h.assertTrue(entry.genome().equals(StarfieldGenomes.baseline(POTATO)),"Real farm did not start with baseline genome");
            }
            int loaded=level.getChunkSource().getLoadedChunksCount();
            for(int day=1;day<=4;day++) {
                for(var root:roots)level.setBlock(root.pos().below(),level.getBlockState(root.pos().below()).setValue(FarmBlock.MOISTURE,7),3);
                long start=System.nanoTime();manager.settleCrops(level,roots,day,0,false);double elapsed=(System.nanoTime()-start)/1_000_000d;
                if(day>1)samples.add(elapsed);
                for(var root:roots)h.assertTrue(manager.getState(level,root.pos()).lastDailyDay==day,"Large daily sweep skipped a native root");
            }
            for(var root:roots) {
                var state=manager.getState(level,root.pos());h.assertTrue(state.phase>0,"Daily growth did not advance real native crop");
                int phase=state.phase,day=state.dayInPhase;manager.settleCrops(level,List.of(root),4,0,false);
                h.assertTrue(state.phase==phase && state.dayInPhase==day,"Repeated native day grew a large-farm crop twice");
            }
            h.assertTrue(loaded==level.getChunkSource().getLoadedChunksCount(),"Loaded-farm growth loaded additional chunks");
            com.agricraft.agricraft.AgriCraft.LOGGER.info("Starfield acceptance: 4096 real crops, 2048 attached materials, daily ms={}",samples);
            h.succeed();
        } finally {for(var root:roots){level.setBlock(root.pos(),Blocks.AIR.defaultBlockState(),3);level.setBlock(root.pos().below(),Blocks.AIR.defaultBlockState(),3);}}
    }

    @GameTest(batch="starfield_release",templateNamespace="agricraft",template="starfield_probe")
    public static void renderIndexFollowsMaterialChangesForgetAndWorldSession(GameTestHelper h) {
        var cache = new StarfieldPlantCache(); var dimension = h.getLevel().dimension().location(); var session = UUID.randomUUID();
        cache.bind(new Object(), dimension); var near = new BlockPos(0,80,0); var far = new BlockPos(200000,80,200000);
        var entry = new StarfieldPlantData.Entry(UUID.randomUUID(),POTATO,StarfieldGenomes.baseline(POTATO),new CompoundTag(),"iron");
        cache.apply(session, dimension, near, Optional.of(entry)); cache.apply(session, dimension, far, Optional.of(entry));
        var seen = new HashMap<BlockPos,StarfieldPlantData.Entry>(); cache.forEachSticks(new ChunkPos(near),4,seen::put);
        h.assertTrue(seen.size()==1 && seen.get(near).equals(entry),"Renderer scans remote farm or misses local material");
        var empty = new StarfieldPlantData.Entry(entry.plantingId(),POTATO,entry.genome(),entry.seedTemplate(),"");
        cache.apply(session,dimension,near,Optional.of(empty)); seen.clear();cache.forEachSticks(new ChunkPos(near),4,seen::put);
        h.assertTrue(seen.isEmpty() && cache.get(near).isPresent(),"Removing material retained renderer or removed plant metadata");
        cache.apply(session,dimension,near,Optional.of(entry)); cache.forget(new ChunkPos(near));seen.clear();cache.forEachSticks(new ChunkPos(near),4,seen::put);
        h.assertTrue(seen.isEmpty(),"Chunk unload retained rendered material");
        cache.apply(UUID.randomUUID(),dimension,near,Optional.of(entry));seen.clear();cache.forEachSticks(new ChunkPos(far),4,seen::put);
        h.assertTrue(seen.isEmpty(),"Reconnect retained remote old-session materials");
        cache.bind(new Object(),dimension);seen.clear();cache.forEachSticks(new ChunkPos(near),4,seen::put);
        h.assertTrue(seen.isEmpty(),"World replacement retained renderer index");h.succeed();
    }

    @GameTest(batch="starfield_release",templateNamespace="agricraft",template="starfield_probe",timeoutTicks=200)
    public static void largeSavedFarmIndexDoesNotLoadChunksAndSurvivesMigration(GameTestHelper h) {
        var level=h.getLevel(); var tag=new CompoundTag(); tag.putInt("schema",1);var rows=new ListTag();
        var entry=new StarfieldPlantData.Entry(UUID.randomUUID(),POTATO,StarfieldGenomes.baseline(POTATO),new CompoundTag(),"iron");
        for(int dimension=0;dimension<2;dimension++) for(int i=0;i<8192;i++) {
            var row=entry.encode();row.putString("dimension",dimension==0?level.dimension().location().toString():"stardewcraft:stardew_valley");
            row.putLong("root",new BlockPos(200000+(i/256)*16+(i%16),80,200000+(i%256)/16).asLong());rows.add(row);
        }
        tag.put("plants",rows);int loaded=level.getChunkSource().getLoadedChunksCount();
        var data=StarfieldPlantData.load(tag,level.registryAccess());
        var target=new ChunkPos(new BlockPos(200000,80,200000));var snapshot=data.inChunk(level,target);
        h.assertTrue(snapshot.size()==256,"Chunk index includes other chunks/dimensions or misses roots");
        for(int i=0;i<1000;i++) h.assertTrue(data.inChunk(level,target).size()==256,"Repeated lookup changed entries");
        h.assertTrue(data.inChunk(level,new ChunkPos(999999,999999)).isEmpty(),"Empty index returned foreign plants");
        h.assertTrue(level.getChunkSource().getLoadedChunksCount()==loaded && !level.hasChunk(target.x,target.z),"Saved-data lookup loaded chunks");
        var restored=StarfieldPlantData.load(data.save(new CompoundTag(),level.registryAccess()),level.registryAccess());
        h.assertTrue(restored.inChunk(level,target).equals(snapshot),"Runtime index changed persisted identities");
        try {snapshot.clear();h.fail("Chunk snapshot is mutable");}catch(UnsupportedOperationException expected){}
        h.succeed();
    }

    @GameTest(batch="starfield_release",templateNamespace="agricraft",template="starfield_probe")
    public static void chunkIndexTracksRealPlantReplacementAndRemoval(GameTestHelper h) {
        var level=h.getLevel();var root=h.absolutePos(new BlockPos(1,1,1));var chunk=new ChunkPos(root);
        level.setBlock(root.below(),ModBlocks.FARMLAND.get().defaultBlockState(),3);
        try {
            level.setBlock(root,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
            var data=StarfieldPlantData.get(level);var first=StarfieldCompat.planting(level,root).orElseThrow();
            h.assertTrue(data.inChunk(level,chunk).get(GlobalPos.of(level.dimension(),root)).equals(first),"Placement missed index");
            StarfieldCompat.setSticks(level,root,"obsidian");
            h.assertTrue(data.inChunk(level,chunk).get(GlobalPos.of(level.dimension(),root)).sticks().equals("obsidian"),"Replacement left stale index");
            level.setBlock(root,Blocks.AIR.defaultBlockState(),3);
            h.assertTrue(!data.inChunk(level,chunk).containsKey(GlobalPos.of(level.dimension(),root)),"Removal retained indexed root");
            level.setBlock(root,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
            h.assertTrue(!data.inChunk(level,chunk).get(GlobalPos.of(level.dimension(),root)).plantingId().equals(first.plantingId()),"Replant inherited indexed UUID");
            h.succeed();
        } finally {level.setBlock(root,Blocks.AIR.defaultBlockState(),3);}
    }

    @GameTest(batch="starfield_release_toggle",templateNamespace="agricraft",template="starfield_probe")
    public static void liveReactivationMigratesPlantsAddedWhileDisabled(GameTestHelper h) {
        var level=h.getLevel();var root=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(root,Blocks.AIR.defaultBlockState(),3);level.setBlock(root.below(),ModBlocks.FARMLAND.get().defaultBlockState(),3);
        StarfieldCompat.scan(level,level.getChunkAt(root));CompatConfig.enableStarfield=false;
        level.setBlock(root,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
        h.assertTrue(StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(),root)).isEmpty(),"Disabled placement created a genotype");
        h.runAfterDelay(2,()->{try {
            // Exercise the registered end-of-tick listener before reactivation.
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(()->true,level.getServer()));
            CompatConfig.enableStarfield=true;StarfieldCompat.scan(level,level.getChunkAt(root));
            var entry=StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(),root));
            h.assertTrue(entry.isPresent() && entry.get().genome().equals(StarfieldGenomes.baseline(POTATO)),"Reactivation hid a dormant legacy planting");h.succeed();
        } finally {CompatConfig.enableStarfield=true;level.setBlock(root,Blocks.AIR.defaultBlockState(),3);}});
    }

    @GameTest(batch="starfield_release_heights",templateNamespace="agricraft",template="starfield_probe")
    public static void legacyScanKeepsSectionOffsetAndNativeProgressAtDifferentHeights(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(1,1,1));
        var roots=List.of(new BlockPos(base.getX(),-60,base.getZ()),new BlockPos(base.getX(),-49,base.getZ()),new BlockPos(base.getX(),85,base.getZ()));
        var manager=com.stardew.craft.manager.CropGrowthManager.get(level);
        try {
            CompatConfig.enableStarfield=false;
            for(var root:roots) {
                level.setBlock(root,Blocks.AIR.defaultBlockState(),3);level.setBlock(root.below(),ModBlocks.FARMLAND.get().defaultBlockState(),3);
                level.setBlock(root,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
                var growth=manager.getState(level,root);growth.phase=2;growth.dayInPhase=1;growth.lastDailyDay=17;
            }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(()->true,level.getServer()));
            CompatConfig.enableStarfield=true;StarfieldCompat.scan(level,level.getChunkAt(base));
            for(var root:roots) {
                var entry=StarfieldPlantData.get(level).get(GlobalPos.of(level.dimension(),root));var growth=manager.getState(level,root);
                h.assertTrue(entry.isPresent() && entry.get().genome().equals(StarfieldGenomes.baseline(POTATO)),"Legacy migration missed height "+root.getY());
                h.assertTrue(growth.phase==2 && growth.dayInPhase==1 && growth.lastDailyDay==17,"Migration reset native progress at "+root.getY());
            }
            var before=StarfieldPlantData.get(level).inChunk(level,new ChunkPos(base));StarfieldCompat.scan(level,level.getChunkAt(base));
            h.assertTrue(before.equals(StarfieldPlantData.get(level).inChunk(level,new ChunkPos(base))),"Repeated watcher scan reassigned planting identity");h.succeed();
        } finally {CompatConfig.enableStarfield=true;for(var root:roots)level.setBlock(root,Blocks.AIR.defaultBlockState(),3);}
    }

    @GameTest(batch="starfield_release",templateNamespace="agricraft",template="starfield_probe")
    public static void multipleOrdinaryPlayersCannotInspectForeignDimensionOrUnloadedChunks(GameTestHelper h) {
        var level=h.getLevel();var root=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(root.below(),ModBlocks.FARMLAND.get().defaultBlockState(),3);level.setBlock(root,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
        var first=StarfieldCompat.planting(level,root).orElseThrow();int loaded=level.getChunkSource().getLoadedChunksCount();
        try {
            for(int i=0;i<2;i++) {
                var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"release"+i));
                player.setPos(root.getX()+0.5,root.getY(),root.getZ()+1);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
                h.assertTrue(!player.hasPermissions(2),"Fixture unexpectedly has administrative permissions");
                var request=new StarfieldInspectionSync.Request(level.dimension().location(),root,first.plantingId());
                h.assertTrue(StarfieldInspectionSync.inspect(player,request,true).isPresent(),"Ordinary player cannot inspect own nearby dimension");
                h.assertTrue(StarfieldInspectionSync.inspect(player,new StarfieldInspectionSync.Request(ResourceLocation.parse("minecraft:the_end"),root,first.plantingId()),true).isEmpty(),"Foreign dimension request accepted");
                h.assertTrue(StarfieldInspectionSync.inspect(player,new StarfieldInspectionSync.Request(level.dimension().location(),root,UUID.randomUUID()),true).isEmpty(),"Stale identity accepted");
                h.assertTrue(StarfieldInspectionSync.inspect(player,new StarfieldInspectionSync.Request(level.dimension().location(),new BlockPos(900000,80,900000),first.plantingId()),true).isEmpty(),"Unloaded request accepted");
                StarfieldPlantSync.logout(player);
            }
            h.assertTrue(loaded==level.getChunkSource().getLoadedChunksCount(),"Player requests loaded distant chunks");h.succeed();
        } finally {level.setBlock(root,Blocks.AIR.defaultBlockState(),3);}
    }
}
