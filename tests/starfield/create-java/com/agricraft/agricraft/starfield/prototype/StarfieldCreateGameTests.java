package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldCreateGameTests {
    @GameTest(batch="starfield_create", templateNamespace="agricraft", template="starfield_probe")
    public static void realCreateHarvesterCollectsNativeMultipartCropIntoContraptionInventoryOnce(GameTestHelper h) {
        var level=level(h); var root=root(level,"corn");
        try {
            plant(level,root,"corn");
            for(int day=1;day<=20 && !StarfieldCompat.inspect(level,root).orElseThrow().mature();day++)grow(level,root,day,1);
            h.assertTrue(StarfieldCompat.inspect(level,root).orElseThrow().mature(),"Create crop never matured");
            var entry=StarfieldCompat.planting(level,root).orElseThrow(); var inventory=new ItemStackHandler(27);
            var context=context(level,inventory); var actor=new HarvesterMovementBehaviour();
            actor.visitNewPosition(context,root.above());
            int count=count(inventory); h.assertTrue(count>0,"Real Create actor did not collect native harvest");
            h.assertTrue(inventory.getStackInSlot(0).getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains(com.agricraft.agricraft.compat.starfield.StarfieldGenomes.PRODUCE_KEY),"Create inventory output lost genome");
            var state=level.getBlockState(root); var upper=level.getBlockState(root.above());
            actor.visitNewPosition(context,root); actor.visitNewPosition(context,root.above());
            h.assertTrue(count(inventory)==count && state.equals(level.getBlockState(root)) && upper.equals(level.getBlockState(root.above())) && StarfieldCompat.planting(level,root).orElseThrow().equals(entry),"Create fallback cut/reset/reharvested native regrowth");
            int days=CropGrowthManager.get(level).getState(level,root).dayInPhase;
            h.assertTrue(days==4,"Create changed native baseline corn regrow duration"); h.succeed();
        } finally {clean(level,root);}
    }

    @GameTest(batch="starfield_create", templateNamespace="agricraft", template="starfield_probe")
    public static void realCreateHarvesterRefusesImmatureForageAndGiantToolFallbacks(GameTestHelper h) {
        var level=level(h); var root=root(level,"refusals");
        try {
            var inventory=new ItemStackHandler(27); var context=context(level,inventory); var actor=new HarvesterMovementBehaviour();
            plant(level,root,"potato"); var immature=level.getBlockState(root); actor.visitNewPosition(context,root);
            h.assertTrue(immature.equals(level.getBlockState(root)) && count(inventory)==0,"Create partially harvested immature native crop");
            for(var block:List.of(ModBlocks.FORAGE_WINTER_ROOT.get(),ModBlocks.GIANT_CAULIFLOWER.get())) {
                clean(level,root); level.setBlock(root.below(),Blocks.DIRT.defaultBlockState(),3); level.setBlock(root,block.defaultBlockState(),2);
                var state=level.getBlockState(root); actor.visitNewPosition(context,root);
                h.assertTrue(state.equals(level.getBlockState(root)) && count(inventory)==0,"Create bypassed unsupported native harvest/tool path");
            }
            h.succeed();
        } finally {clean(level,root);}
    }
    private static MovementContext context(ServerLevel level,ItemStackHandler inventory) {
        var contraption=new BearingContraption(false,Direction.UP); contraption.getStorage().initialize(); contraption.getStorage().attachExternal(inventory);
        return new MovementContext(level,new StructureBlockInfo(BlockPos.ZERO,com.simibubi.create.AllBlocks.MECHANICAL_HARVESTER.getDefaultState(),null),contraption);
    }
    private static ServerLevel level(GameTestHelper h){return h.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);}
    private static BlockPos root(ServerLevel level,String name) {
        var owner=UUID.nameUUIDFromBytes(("agricraft:create:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));var farms=FarmInstanceRegistry.get(level.getServer());var farm=farms.getFarm(owner);
        if(farm==null)farm=farms.createFarm(owner,name,"Create probe",FarmType.STANDARD);farm.markInitialized();farm.setGraceDaysLeft(0);var root=farm.getOrigin().offset(40,30,40);level.getChunk(root);return root;
    }
    private static void plant(ServerLevel level,BlockPos root,String crop) {
        clean(level,root);level.setBlock(root.below(),ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
        com.stardew.craft.farming.CropPlanting.place(level,root,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("stardewcraft:"+crop+"_crop")).defaultBlockState());StarfieldCompat.planting(level,root).orElseThrow();
    }
    private static void grow(ServerLevel level,BlockPos root,int day,int season) {
        level.setBlock(root.below(),level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE,7),3);CropGrowthManager.get(level).settleCrops(level,List.of(GlobalPos.of(level.dimension(),root)),day,season,false);
    }
    private static int count(ItemStackHandler inventory){int result=0;for(int i=0;i<inventory.getSlots();i++)result+=inventory.getStackInSlot(i).getCount();return result;}
    private static void clean(ServerLevel level,BlockPos root){level.setBlock(root,Blocks.AIR.defaultBlockState(),3);level.setBlock(root.above(),Blocks.AIR.defaultBlockState(),3);level.setBlock(root.below(),Blocks.AIR.defaultBlockState(),3);}
}
