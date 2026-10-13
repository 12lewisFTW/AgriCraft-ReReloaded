package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.*;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.crop.WildSeedCropBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.Path;
import java.util.*;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldMigrationGameTests {
    @GameTest(batch="starfield_migration",templateNamespace="agricraft",template="starfield_probe",timeoutTicks=200)
    public static void realWorldSurvivesEnabledDisabledEnabledAndNativeWildTransformation(GameTestHelper h) {
        String phase=System.getProperty("agricraft.starfield.migrationPhase");
        var level=h.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        var owner=UUID.nameUUIDFromBytes("agricraft:release-migration".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var registry=FarmInstanceRegistry.get(level.getServer());var farm=registry.getFarm(owner);
        if(farm==null)farm=registry.createFarm(owner,"Release migration","Migration",FarmType.STANDARD);
        var root=farm.getOrigin().offset(48,40,48);var wild=root.offset(2,0,0);var chestPos=root.offset(4,0,0);
        var chunk=new net.minecraft.world.level.ChunkPos(root);boolean forced=level.getForcedChunks().contains(chunk.toLong());
        if(!forced)level.setChunkForced(chunk.x,chunk.z,true);level.getChunk(root);
        h.runAfterDelay(40,()->{try {
            var witnessPath=Path.of(System.getProperty("agricraft.starfield.migrationWitness"));var manager=CropGrowthManager.get(level);
            if(phase.equals("write")) {
                for(var pos:List.of(root,wild)) {level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.below(),ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);}
                level.setBlock(root,ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
                level.setBlock(wild,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("stardewcraft:spring_wild_seed_crop")).defaultBlockState(),3);
                var data=StarfieldPlantData.get(level);var saved=data.save(new CompoundTag(),level.registryAccess());
                for(var row:saved.getList("plants",Tag.TAG_COMPOUND)) {
                    var value=(CompoundTag)row;if(value.getString("dimension").equals(level.dimension().location().toString())
                            && (value.getLong("root")==root.asLong() || value.getLong("root")==wild.asLong())) {
                        var genome=value.getCompound("genome");for(var stat:AgriStatRegistry.getInstance()) {var pair=genome.getCompound("genes").getCompound(stat.getId());pair.putInt("dom",stat.getMax());pair.putInt("rec",stat.getMin());}
                        value.putString("sticks","obsidian");
                    }
                }
                var migrated=StarfieldPlantData.load(saved,level.registryAccess());migrated.setDirty();
                level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE,migrated);
                var entry=StarfieldCompat.planting(level,root).orElseThrow();var growth=manager.getState(level,root);growth.phase=2;growth.dayInPhase=1;growth.lastDailyDay=29;growth.lastDailyWatered=true;manager.setDirty();
                var visual=com.stardew.craft.block.crop.StardewCropBlock.class.getDeclaredMethod("syncVisualStage",net.minecraft.server.level.ServerLevel.class,BlockPos.class,CropGrowthManager.CropGrowthState.class);
                visual.setAccessible(true);visual.invoke(level.getBlockState(root).getBlock(),level,root,growth);
                var seed=StarfieldCompat.seedFromPlant(level,root).orElseThrow();
                level.setBlock(chestPos,Blocks.AIR.defaultBlockState(),3);level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
                ((ChestBlockEntity)level.getBlockEntity(chestPos)).setItem(0,seed);
                var witness=new CompoundTag();witness.put("entry",entry.encode());witness.put("wild",StarfieldCompat.planting(level,wild).orElseThrow().encode());
                witness.put("seed",seed.save(level.registryAccess()));witness.putString("block",level.getBlockState(root).toString());
                NbtIo.writeCompressed(witness,witnessPath);
            } else {
                var witness=NbtIo.readCompressed(witnessPath,NbtAccounter.unlimitedHeap());var data=StarfieldPlantData.existing(level);
                var actual=data.get(GlobalPos.of(level.dimension(),root)).orElseThrow();var growth=manager.getState(level,root);
                h.assertTrue(actual.encode().equals(witness.getCompound("entry")),"Mode change lost sidecar: actual="+actual.encode()+" expected="+witness.getCompound("entry"));
                h.assertTrue(level.getBlockState(root).toString().equals(witness.getString("block")),"Mode change changed block: actual="+level.getBlockState(root)+" expected="+witness.getString("block"));
                h.assertTrue(growth.phase==2 && growth.dayInPhase==1 && growth.lastDailyDay==29 && growth.lastDailyWatered,"Mode change reset native saved progress/date/water");
                var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);
                h.assertTrue(chest!=null && chest.getItem(0).save(level.registryAccess()).equals(witness.getCompound("seed")),"Mode change damaged real stored seed components");
                if(phase.equals("disabled")) {
                    h.assertTrue(!CompatConfig.enableStarfield && !StarfieldCompat.isActive() && StarfieldCompat.planting(level,root).isEmpty(),"Disabled mode still provides genome operations");
                    var crop=(WildSeedCropBlock)level.getBlockState(wild).getBlock();var transform=WildSeedCropBlock.class.getDeclaredMethod("transformToForage",net.minecraft.server.level.ServerLevel.class,BlockPos.class);transform.setAccessible(true);transform.invoke(crop,level,wild);
                    var transformed=data.get(GlobalPos.of(level.dimension(),wild)).orElseThrow();var before=StarfieldPlantData.Entry.decode(witness.getCompound("wild"));
                    h.assertTrue(transformed.plantingId().equals(before.plantingId()) && transformed.genome().equals(before.genome()) && transformed.sticks().equals(before.sticks())
                            && transformed.form().equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(wild).getBlock())) && !transformed.form().equals(transformed.type()),"Disabled native wild transformation lost dormant heredity");
                    witness.put("wild",transformed.encode());NbtIo.writeCompressed(witness,witnessPath);
                } else {
                    h.assertTrue(StarfieldCompat.isActive() && StarfieldCompat.planting(level,wild).orElseThrow().encode().equals(witness.getCompound("wild")),"Reactivation lost native transformed identity");
                    h.assertTrue(StarfieldGenomes.readSeed(chest.getItem(0),actual.type()).equals(actual.genome()),"Reactivation cannot read original inherited seed");
                    h.assertTrue(NativeCropGeneRules.graceRemaining(growth.lastDailyDay,3)==3,"Reactivation shifted historical season grace");
                    level.setBlock(root,Blocks.AIR.defaultBlockState(),3);level.setBlock(wild,Blocks.AIR.defaultBlockState(),3);level.setBlock(chestPos,Blocks.AIR.defaultBlockState(),3);
                }
            }
            h.succeed();
        }catch(Exception failure){throw new RuntimeException(failure);}finally{if(!forced)level.setChunkForced(chunk.x,chunk.z,false);}});
    }
}
